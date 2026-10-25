// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.StandaloneSoapUICore;
import com.eviware.soapui.config.SoapuiSettingsDocumentConfig;
import com.eviware.soapui.config.SoapuiWorkspaceDocumentConfig;
import com.eviware.soapui.settings.UISettings;
import com.eviware.soapui.settings.VersionUpdateSettings;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Starts the actual SoapUI desktop, using an isolated profile and only synthetic local projects. */
public final class FullUiTests {
    private static Path output;
    private static JDialog preferences, newProject;
    private static volatile boolean capturingNative;
    private static com.eviware.soapui.impl.wsdl.WsdlProject firstProject;
    private static JMenuBar originalBar;
    private static JMenu[] originalMenus;
    private static final java.util.Map<JMenuItem, KeyStroke> fileAccelerators = new java.util.IdentityHashMap<>();
    public static void main(String[] args) {
        Thread watchdog = new Thread(() -> { try { Thread.sleep(90000); } catch (InterruptedException e) { return; }
            System.err.println("Full SoapUI UI test timed out"); System.exit(2); }, "ui-test-watchdog");
        watchdog.setDaemon(true); watchdog.start();
        try { run(args); System.exit(0); }
        catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
    private static void run(String[] args) throws Exception {
        System.setProperty("java.util.prefs.PreferencesFactory", "soapui.systemtheme.MemoryPreferencesFactory");
        output = Path.of(args[0]); Files.createDirectories(output);
        Path home = Path.of(System.getProperty("user.home")); Files.createDirectories(home);
        Path settings = home.resolve("soapui-settings.xml");
        SoapuiSettingsDocumentConfig settingsDoc = SoapuiSettingsDocumentConfig.Factory.newInstance();
        settingsDoc.addNewSoapuiSettings(); settingsDoc.save(settings.toFile());
        SoapuiWorkspaceDocumentConfig workspaceDoc = SoapuiWorkspaceDocumentConfig.Factory.newInstance();
        workspaceDoc.addNewSoapuiWorkspace().setCollectInfoForSupport(false);
        workspaceDoc.save(home.resolve("default-soapui-workspace.xml").toFile());
        System.setProperty("soapui.browser.disabled", "true");
        StandaloneSoapUICore core = new StandaloneSoapUICore(settings.toString()) {
            @Override public void prepareUI() {
                super.prepareUI();
                SoapUI.getFrame().setFocusableWindowState(false);
                SoapUI.getFrame().setAutoRequestFocus(false);
                SoapUI.getFrame().setLocation(-10000,-10000);
            }
        };
        core.getSettings().setBoolean(UISettings.DISABLE_ANALYTICS, true);
        core.getSettings().setBoolean(UISettings.SHOULD_DISPLAY_ANALYTICS_DIALOG, false);
        core.getSettings().setBoolean(UISettings.SHOW_STAY_TUNED_DIALOG, false);
        core.getSettings().setBoolean(UISettings.SHOW_ENDPOINT_EXPLORER_ON_START, false);
        core.getSettings().setBoolean(UISettings.SHOW_STARTUP_PAGE, false);
        core.getSettings().setBoolean(VersionUpdateSettings.AUTO_CHECK_VERSION_UPDATE, false);
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            // Configure ownerless dialogs too, before their native peer can request keyboard focus.
            Component source=e.getSource() instanceof Component ? (Component)e.getSource() : null;
            Window window=source instanceof Window ? (Window)source : source==null ? null : SwingUtilities.getWindowAncestor(source);
            if(window!=null) { window.setFocusableWindowState(false);window.setAutoRequestFocus(false);if(!capturingNative)window.setLocation(-10000,-10000); }
        }, AWTEvent.WINDOW_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.HIERARCHY_EVENT_MASK);
        SwingUtilities.invokeAndWait(() -> {
            try { SoapUI.startSoapUI(new String[0], "SoapUI 5.10.0 — isolated plugin test", core); }
            catch (Exception e) { throw new RuntimeException(e); }
            SoapUI.getFrame().setSize(1200,800); SoapUI.getFrame().validate();
        });
        waitForMenu();
        SwingUtilities.invokeAndWait(() -> {
            try { snapshot("soapui-light.png");
                originalBar = SoapUI.getFrame().getJMenuBar();
                originalMenus = new JMenu[originalBar.getMenuCount()];
                for (int i=0; i<originalMenus.length; i++) originalMenus[i] = originalBar.getMenu(i);
                for (Component child : originalMenus[0].getMenuComponents()) if (child instanceof JMenuItem)
                    fileAccelerators.put((JMenuItem)child, ((JMenuItem)child).getAccelerator());
                com.eviware.soapui.impl.wsdl.WsdlProject project=(com.eviware.soapui.impl.wsdl.WsdlProject)
                    SoapUI.getWorkspace().createProject("Theme example",(java.io.File)null);
                firstProject = project;
                reproduceLegacyMenuFailure(project);
                project.addNewTestSuite("Local regression").addNewTestCase("Example case");
                com.eviware.soapui.support.UISupport.showDesktopPanel(project);
                SoapUI.getFrame().validate(); snapshot("soapui-project-light.png");
                menu().getItem(2).doClick(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        Thread.sleep(600);
        nativeSnapshot("soapui-native-dark.png", true);
        SwingUtilities.invokeAndWait(() -> {
            try { checkDarkWorkflows(); } catch (Exception e) { throw new RuntimeException(e); }
        });
        // Newly created components are styled by the controller's queued EDT batch.
        SwingUtilities.invokeAndWait(() -> {
            try { snapshot("soapui-dark.png");
                BufferedImage image=ImageIO.read(output.resolve("soapui-dark.png").toFile());
                StringBuilder dump = new StringBuilder(); dump(SoapUI.getFrame(),dump,0);
                Files.writeString(output.resolve("component-tree.txt"),dump);
                checkToolbars(SoapUI.getFrame(),image);
                Point barPoint = SwingUtilities.convertPoint(originalBar, originalBar.getWidth()-30, originalBar.getHeight()/2, SoapUI.getFrame());
                if (!new Color(image.getRGB(barPoint.x,barPoint.y)).equals(new Color(0x25282e)))
                    throw new AssertionError("Original menu bar paint remains light");
                JMenu fileMenu = originalMenus[0];
                Point mouse = SwingUtilities.convertPoint(fileMenu, fileMenu.getWidth()/2, fileMenu.getHeight()/2, SoapUI.getFrame().getLayeredPane());
                if (SwingUtilities.getDeepestComponentAt(SoapUI.getFrame().getLayeredPane(),mouse.x,mouse.y) != fileMenu)
                    throw new AssertionError("Menu paint overlay intercepted a mouse target");
                checkWindowControls(SoapUI.getFrame());
                JInternalFrame internal=findInternalFrame(SoapUI.getFrame());
                if(internal==null)throw new AssertionError("No actual internal frame");
                internal.setMaximum(true);SoapUI.getFrame().validate();
                checkWindowControls(SoapUI.getFrame());snapshot("soapui-maximized-dark.png");
                internal.setMaximum(false);SoapUI.getFrame().validate();checkWindowControls(SoapUI.getFrame());
                buildDialogs();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        Thread.sleep(500);
        SwingUtilities.invokeAndWait(() -> {
            try {
                checkHeaders(preferences);checkHeaders(newProject);
                snapshot(preferences,"preferences-dark.png");snapshot(newProject,"new-soap-project-dark.png");
                checkAbout();
                checkErrorText();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        nativeSnapshot("soapui-native-after-dialogs-dark.png", true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                menu().getItem(1).doClick(); snapshot("soapui-restored.png");
                snapshot(preferences,"preferences-restored.png");snapshot(newProject,"new-soap-project-restored.png");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        nativeSnapshot("soapui-native-restored.png", false);
        SwingUtilities.invokeAndWait(() -> { preferences.dispose();newProject.dispose();SoapUI.getFrame().dispose(); });
        System.out.println("PASS: actual SoapUI 5.10.0 desktop, About, Preferences, New SOAP Project, new dialog events and restoration (isolated profile)");
    }
    /** Isolate the old algorithm against the real host, then put every menu back immediately. */
    private static void reproduceLegacyMenuFailure(com.eviware.soapui.impl.wsdl.WsdlProject project) throws Exception {
        JMenuBar replacement = new JMenuBar();
        while (originalBar.getComponentCount() > 0) replacement.add(originalBar.getComponent(0));
        SoapUI.getFrame().setJMenuBar(replacement);
        boolean reproduced = false;
        try {
            java.lang.reflect.Method update = com.eviware.soapui.support.swing.MenuBuilderHelper.class.getDeclaredMethod(
                "buildMenu", com.eviware.soapui.model.ModelItem.class, String.class, com.eviware.soapui.model.tree.SoapUITreeNode.class);
            update.setAccessible(true);
            try { update.invoke(null, project, "Project", null); }
            catch (java.lang.reflect.InvocationTargetException error) {
                if (!(error.getCause() instanceof NullPointerException)) throw error;
                reproduced = true;
            }
        } finally {
            while (replacement.getComponentCount() > 0) originalBar.add(replacement.getComponent(0));
            SoapUI.getFrame().setJMenuBar(originalBar);
        }
        if (!reproduced) throw new AssertionError("Old menu transfer did not reproduce the host menu-update failure");
        System.out.println("PASS: old menu-transfer algorithm reproduces NullPointerException in real SoapUI MenuBuilderHelper.buildMenu");
    }

    private static void checkDarkWorkflows() throws Exception {
        if (SoapUI.getMenuBar() != originalBar || SoapUI.getFrame().getJMenuBar() != originalBar)
            throw new AssertionError("SoapUI lost its original menu bar in Dark");
        for (int i=0; i<originalMenus.length; i++)
            if (originalBar.getMenu(i) != originalMenus[i]) throw new AssertionError("Menu identity changed");
        fileAccelerators.forEach((item, accelerator) -> {
            if (item.getParent() != originalMenus[0].getPopupMenu() || !java.util.Objects.equals(accelerator, item.getAccelerator()))
                throw new AssertionError("File menu item or keyboard accelerator changed");
        });
        originalMenus[0].doClick();
        if (MenuSelectionManager.defaultManager().getSelectedPath().length == 0)
            throw new AssertionError("Original File menu no longer opens");
        MenuSelectionManager.defaultManager().clearSelectedPath();
        // Save and import a new synthetic project after Dark is already active.
        com.eviware.soapui.impl.wsdl.WsdlProject fixture = new com.eviware.soapui.impl.wsdl.WsdlProject();
        fixture.setName("Imported while dark");
        fixture.addNewTestSuite("Imported suite").addNewTestCase("Imported case");
        Path file = output.resolve("synthetic-import.xml"); fixture.saveAs(file.toString());
        com.eviware.soapui.impl.wsdl.WsdlProject imported = (com.eviware.soapui.impl.wsdl.WsdlProject)
            SoapUI.getWorkspace().importProject(file.toString());
        if (imported == null || imported.getTestSuiteCount() != 1) throw new AssertionError("Dark project import failed");
        for (var project : new com.eviware.soapui.impl.wsdl.WsdlProject[]{firstProject, imported}) {
            var suite = project.getTestSuiteAt(0); var testCase = suite.getTestCaseAt(0);
            for (var item : new com.eviware.soapui.model.ModelItem[]{project, suite, testCase}) {
                SoapUI.getNavigator().selectModelItem(item);
                var path = SoapUI.getNavigator().getTreePath(item);
                if (path == null) throw new AssertionError("Synthetic item missing from navigator");
                com.eviware.soapui.support.swing.MenuBuilderHelper.buildTreeNodeMenu(
                    (com.eviware.soapui.model.tree.SoapUITreeNode)path.getLastPathComponent());
                String name = item == project ? "Project" : item == suite ? "Suite" : "Case";
                JMenu dynamic = com.eviware.soapui.support.swing.MenuBuilderHelper.getMenu(name);
                if (dynamic == null || dynamic.getItemCount() == 0) throw new AssertionError("Dynamic menu is empty: " + name);
                // Invoke the host's Open Editor action when exposed, without running tests or requests.
                boolean opened = false;
                for (int i=0; i<dynamic.getItemCount(); i++) {
                    JMenuItem action = dynamic.getItem(i);
                    if (action != null && "Open Editor".equals(action.getText()) && action.isEnabled()) {
                        action.doClick(); opened = true; break;
                    }
                }
                if (!opened) com.eviware.soapui.support.UISupport.showDesktopPanel(item);
                SoapUI.getDesktop().closeDesktopPanel(item);
                com.eviware.soapui.support.UISupport.showDesktopPanel(item);
                SoapUI.getDesktop().closeDesktopPanel(item);
            }
        }
        var testCase = imported.getTestSuiteAt(0).getTestCaseAt(0);
        var step = testCase.addTestStep("groovy", "Unsaved synthetic editor");
        com.eviware.soapui.support.UISupport.showDesktopPanel(step);
        org.fife.ui.rsyntaxtextarea.RSyntaxTextArea editor = findSyntax(SoapUI.getFrame());
        if (editor == null) throw new AssertionError("Real Groovy editor not found");
        editor.setText("// unsaved synthetic text; never executed\nreturn 42");
        editor.setCaretPosition(4); editor.moveCaretPosition(15);
        var document = editor.getDocument(); String text = editor.getText();
        for (int cycle=0; cycle<3; cycle++) {
            menu().getItem(1).doClick(); menu().getItem(2).doClick();
            if (editor.getDocument() != document || !editor.getText().equals(text)
                || editor.getCaret().getDot()!=15 || editor.getCaret().getMark()!=4)
                throw new AssertionError("Actual editor document, unsaved text or selection changed");
        }
        SoapUI.getDesktop().closeDesktopPanel(step);
        com.eviware.soapui.support.UISupport.showDesktopPanel(firstProject);
        SoapUI.getFrame().validate();
        System.out.println("PASS: import in Dark, project/suite/case selection, dynamic menu actions, repeated editor open/close, three theme cycles with unsaved Groovy document and selection");
    }
    private static org.fife.ui.rsyntaxtextarea.RSyntaxTextArea findSyntax(Component c) {
        if (c instanceof org.fife.ui.rsyntaxtextarea.RSyntaxTextArea) return (org.fife.ui.rsyntaxtextarea.RSyntaxTextArea)c;
        if (c instanceof Container) for (Component child : ((Container)c).getComponents()) {
            var result = findSyntax(child); if (result != null) return result;
        }
        return null;
    }

    private static void checkAbout() throws Exception {
        JMenuItem about=null;JMenuBar bar=SoapUI.getFrame().getJMenuBar();
        for(int i=0;i<bar.getMenuCount();i++) {
            JMenu menu=bar.getMenu(i);if(menu==null || !"Help".equals(menu.getText()))continue;
            for(int j=0;j<menu.getItemCount();j++)if(menu.getItem(j)!=null&&"About SoapUI".equals(menu.getItem(j).getText()))about=menu.getItem(j);
        }
        if(about==null)throw new AssertionError("About action absent");
        Throwable[] error={null};boolean[] checked={false};int[] attempts={0};
        Timer timer=new Timer(250,null);
        timer.addActionListener(event -> {
            for(Window window:Window.getWindows())if(window instanceof JDialog && window.isShowing()&&"About SoapUI".equals(((JDialog)window).getTitle())) {
                try {
                    JEditorPane pane=findEditor(window);
                    if(pane==null || !new Color(0x1d2025).equals(pane.getBackground())) { if(++attempts[0]<12)return;throw new AssertionError("About did not become dark"); }
                    snapshot(window,"about-dark.png");
                    String html=pane.getText();if(html.contains("background-image")||!html.contains("smartbear.com"))throw new AssertionError("About splash or attribution incorrect");
                    menu().getItem(1).doClick();snapshot(window,"about-restored.png");
                    if(!pane.getText().contains("background-image"))throw new AssertionError("About original splash not restored");
                    menu().getItem(2).doClick();checked[0]=true;
                }catch(Throwable e){error[0]=e;}
                finally{timer.stop();window.dispose();}
                return;
            }
        });
        timer.start();about.doClick();timer.stop();
        if(error[0]!=null)throw new AssertionError("Actual About dialog failed",error[0]);
        if(!checked[0])throw new AssertionError("Actual About dialog was not checked");
    }
    private static JEditorPane findEditor(Component c) {
        if(c instanceof JEditorPane)return (JEditorPane)c;
        if(c instanceof Container)for(Component child:((Container)c).getComponents()) {JEditorPane pane=findEditor(child);if(pane!=null)return pane;}
        return null;
    }
    private static void checkErrorText() throws Exception {
        Throwable[] error={null};boolean[] checked={false};int[] attempts={0};
        Timer timer=new Timer(200,null);
        timer.addActionListener(event -> {
            for(Window window:Window.getWindows())if(window instanceof JDialog&&window.isShowing()&&"Error".equals(((JDialog)window).getTitle())) {
                try {
                    JEditorPane pane=findEditor(window);
                    if(pane==null||!Boolean.TRUE.equals(pane.getClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES))) {
                        if(++attempts[0]<12)return;throw new AssertionError("Actual error HTML did not become readable");
                    }
                    javax.swing.text.Document original=pane.getDocument();String text=original.getText(0,original.getLength());
                    if(!pane.getForeground().equals(new Color(0xe3e6eb)))throw new AssertionError("Error foreground remains dark");
                    snapshot(window,"error-text-dark.png");
                    menu().getItem(1).doClick();snapshot(window,"error-text-restored.png");
                    if(pane.getClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES)!=null||pane.getDocument()!=original
                        ||!text.equals(original.getText(0,original.getLength())))throw new AssertionError("Error document or original display property changed");
                    menu().getItem(2).doClick();checked[0]=true;
                }catch(Throwable e){error[0]=e;}
                finally{timer.stop();window.dispose();}
                return;
            }
        });
        timer.start();
        com.eviware.soapui.support.UISupport.getDialogs().showExtendedInfo("Error","An error of type NullPointerException occurred.",
            "<html><body>java.lang.NullPointerException: Cannot invoke<br>javax.swing.JMenu.removeAll() because curMenu is null</body></html>",new Dimension(430,330));
        timer.stop();if(error[0]!=null)throw new AssertionError("Actual error-dialog styling failed",error[0]);
        if(!checked[0])throw new AssertionError("Actual error dialog was not checked");
    }
    private static Object field(Object object,String name) throws Exception {
        java.lang.reflect.Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);
    }
    private static void build(Object object) throws Exception {
        java.lang.reflect.Method method=object.getClass().getDeclaredMethod("buildDialog");method.setAccessible(true);method.invoke(object);
    }
    private static void buildDialogs() throws Exception {
        com.eviware.soapui.actions.SoapUIPreferencesAction action=com.eviware.soapui.actions.SoapUIPreferencesAction.getInstance();
        build(action);Object config=field(action,"dialog");build(config);preferences=(JDialog)field(config,"dialog");
        for(com.eviware.soapui.actions.Prefs pref:action.getPrefs())pref.setFormValues(SoapUI.getSettings());
        com.eviware.x.impl.swing.JFormDialog form=(com.eviware.x.impl.swing.JFormDialog)
            com.eviware.x.form.support.ADialogBuilder.buildDialog(com.eviware.soapui.impl.actions.NewWsdlProjectAction.Form.class);
        newProject=form.getDialog();
        for(JDialog dialog:new JDialog[]{preferences,newProject}) {
            dialog.setModal(false);dialog.setFocusableWindowState(false);dialog.setAutoRequestFocus(false);
            if(dialog==preferences)dialog.setSize(1000,700);else dialog.pack();
            dialog.setLocation(-10000,-10000);dialog.setVisible(true);
        }
    }
    private static void checkHeaders(Component c) throws Exception {
        if(c instanceof com.eviware.soapui.support.DescriptionPanel) {
            BufferedImage image=new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();c.printAll(g);g.dispose();Color corner=new Color(image.getRGB(1,1));
            if(corner.getRed()>80 || corner.getGreen()>80 || corner.getBlue()>80)
                throw new AssertionError("Real SoapUI dialog header remains bright");
        }
        if(c instanceof Container)for(Component child:((Container)c).getComponents())checkHeaders(child);
    }
    private static JInternalFrame findInternalFrame(Component c) {
        if(c instanceof JInternalFrame)return (JInternalFrame)c;
        if(c instanceof Container)for(Component child:((Container)c).getComponents()) {
            JInternalFrame frame=findInternalFrame(child);if(frame!=null)return frame;
        }
        return null;
    }
    private static void checkWindowControls(Component c) {
        if(c instanceof AbstractButton && c.getClass().getName().contains("BasicInternalFrameTitlePane$NoFocusButton")) {
            Icon icon=((AbstractButton)c).getIcon();
            if(icon==null||!icon.getClass().getName().endsWith(".TitleControlIcon"))throw new AssertionError("Internal-window control still has a legacy icon");
            BufferedImage image=new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();icon.paintIcon(c,g,0,0);g.dispose();Color face=new Color(image.getRGB(1,1));
            if(face.getRed()>80||face.getGreen()>80||face.getBlue()>80)throw new AssertionError("Internal-window control face remains bright");
        }
        if(c instanceof Container)for(Component child:((Container)c).getComponents())checkWindowControls(child);
    }
    private static JMenu menu() {
        JMenuBar bar = SoapUI.getFrame().getJMenuBar();
        for (int i=0;i<bar.getMenuCount();i++) if (bar.getMenu(i)!=null && "Theme".equals(bar.getMenu(i).getText())) return bar.getMenu(i);
        return null;
    }
    private static void waitForMenu() throws Exception {
        long until = System.nanoTime()+10_000_000_000L;
        while (System.nanoTime()<until) {
            boolean[] ready={false}; SwingUtilities.invokeAndWait(() -> ready[0]=menu()!=null);
            if (ready[0]) return; Thread.sleep(50);
        }
        throw new AssertionError("The packaged plugin did not add a Theme menu to SoapUI");
    }
    private static void snapshot(String name) throws Exception {
        snapshot(SoapUI.getFrame(),name);
    }
    private static void nativeSnapshot(String name, boolean expectDark) throws Exception {
        if (!Boolean.getBoolean("soapui.theme.testNativeTitleBar")) return;
        capturingNative=true;
        Rectangle[] bounds = new Rectangle[1];
        Point[] menuSample = new Point[1];
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = SoapUI.getFrame(); frame.setAlwaysOnTop(true); frame.setLocation(50,50); frame.toFront();
            bounds[0]=frame.getBounds();
            JMenuBar bar = frame.getJMenuBar();
            menuSample[0] = SwingUtilities.convertPoint(bar, bar.getWidth()-30, bar.getHeight()/2, frame);
        });
        try {
            Robot robot = new Robot(); long deadline = System.nanoTime()+10_000_000_000L;
            BufferedImage image;
            do {
                robot.delay(150);image=robot.createScreenCapture(bounds[0]);
                Color caption=new Color(image.getRGB(500,15));
                // A dark pixel alone could belong to an unrelated foreground application.
                // Require SoapUI's yellow native icon and the test frame's expected menu surface.
                int yellow = 0;
                for (int y=6; y<25; y++) for (int x=8; x<31; x++) {
                    Color icon = new Color(image.getRGB(x,y));
                    if (icon.getRed()>140 && icon.getGreen()>120 && icon.getBlue()<100) yellow++;
                }
                Color menuColor = new Color(image.getRGB(menuSample[0].x,menuSample[0].y));
                Color expectedMenu = expectDark ? new Color(0x25282e) : Color.WHITE;
                boolean ownFrame = yellow>15 && menuColor.equals(expectedMenu);
                if(ownFrame && (!expectDark || (caption.getRed()<80&&caption.getGreen()<80&&caption.getBlue()<80)))break;
                if(System.nanoTime()>deadline)
                    throw new AssertionError("Native capture did not show the expected SoapUI test frame/title bar; icon pixels="+yellow+"; menu="+menuColor+"; caption="+caption+"; current bounds="+SoapUI.getFrame().getBounds()+"; showing="+SoapUI.getFrame().isShowing()+"; state="+SoapUI.getFrame().getExtendedState()+"; top="+SoapUI.getFrame().isAlwaysOnTop());
            }while(true);
            ImageIO.write(image,"png",output.resolve(name).toFile());
        } finally {
            SwingUtilities.invokeAndWait(() -> { SoapUI.getFrame().setAlwaysOnTop(false);SoapUI.getFrame().setLocation(-10000,-10000); });
            capturingNative=false;
        }
    }
    private static void snapshot(Window frame,String name) throws Exception {
        BufferedImage image = new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics(); frame.printAll(g); g.dispose(); ImageIO.write(image,"png",output.resolve(name).toFile());
    }
    private static void dump(Component c,StringBuilder text,int depth) {
        if (!c.isVisible()) return;
        text.append(" ".repeat(depth)).append(c.getClass().getName()).append(" ").append(c.getBounds())
            .append(" bg=").append(c.getBackground()).append(" fg=").append(c.getForeground()).append('\n');
        if (c instanceof Container) for (Component child:((Container)c).getComponents()) dump(child,text,depth+1);
    }
    private static void checkToolbars(Component c,BufferedImage image) {
        if(!c.isShowing())return;
        if(c instanceof JToolBar && c.getClass().getName().startsWith("com.eviware.soapui.") && c.getWidth()>10 && c.getHeight()>10) {
            Point p=SwingUtilities.convertPoint(c,0,0,SoapUI.getFrame());int bright=0,total=0;
            for(int y=0;y<c.getHeight();y++)for(int x=0;x<c.getWidth();x++) {
                if(p.x+x<0 || p.y+y<0 || p.x+x>=image.getWidth() || p.y+y>=image.getHeight())continue;
                Color color=new Color(image.getRGB(p.x+x,p.y+y));total++;
                if(color.getRed()>220 && color.getGreen()>220 && color.getBlue()>220)bright++;
            }
            if(total>0 && (double)bright/total>.18)throw new AssertionError("Toolbar still has a bright background: "+c.getClass().getName()+"; bounds="+c.getBounds()+"; parent="+c.getParent().getClass().getName()+"; layout="+c.getParent().getLayout());
        }
        if(c instanceof Container)for(Component child:((Container)c).getComponents())checkToolbars(child,image);
    }
}
