// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.nio.file.*;
import java.util.Objects;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

/** Loads SoapUI's real bundled pages with page JavaScript disabled; no API or analytics calls. */
public final class WebThemeTests {
    private static int checks;
    private static WebView view;
    private static JFXPanel panel;
    private static JFrame frame;
    private static ThemeEngine theme;
    private static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    private static <T>T fx(Callable<T> action) throws Exception {
        FutureTask<T> task=new FutureTask<>(action);Platform.runLater(task);return task.get(8,TimeUnit.SECONDS);
    }
    private static void loaded() throws Exception {
        long until=System.nanoTime()+8_000_000_000L;
        while(System.nanoTime()<until) {
            if(fx(()->view.getEngine().getLoadWorker().getState()==javafx.concurrent.Worker.State.SUCCEEDED))return;
            Thread.sleep(50);
        }
        throw new AssertionError("bundled page did not load");
    }
    private static String computed(String selector,String property) throws Exception {
        return fx(()->String.valueOf(view.getEngine().executeScript("getComputedStyle(document.querySelector('"+selector+"')).getPropertyValue('"+property+"')")));
    }
    private static void awaitDark(boolean dark) throws Exception {
        long until=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<until) {
            if(computed("body","background-color").equals("rgb(37, 40, 46)")==dark)return;
            Thread.sleep(50);
        }
        throw new AssertionError("Web page dark="+dark+"; body="+computed("body","background-color"));
    }
    public static void main(String[] args) {
        // Test sandbox disallows the loopback socket opened by Java HTTP/2 initialization.
        // The legacy loader can read bundled jar resources without any socket. Production is unchanged.
        System.setProperty("com.sun.webkit.useHTTP2Loader","false");
        Thread watchdog=new Thread(()->{try{Thread.sleep(40000);}catch(InterruptedException e){return;}System.exit(2);});watchdog.setDaemon(true);watchdog.start();
        try{run(Path.of(args[0]));System.out.println("PASS: "+checks+" actual bundled web page, input, scope and stylesheet restoration checks");System.exit(0);}
        catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
    private static void run(Path output) throws Exception {
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(()->{
            frame=new JFrame("Web theme test");frame.setFocusableWindowState(false);frame.setAutoRequestFocus(false);
            frame.setLocation(-10000,-10000);panel=new JFXPanel();frame.add(panel);frame.setSize(900,520);frame.setVisible(true);
            theme=new ThemeEngine();try{theme.setDark(true,frame);}catch(Exception e){throw new RuntimeException(e);}
        });
        Platform.setImplicitExit(false);
        // Scene is deliberately installed after the Swing component was already patched.
        fx(()->{view=new WebView();view.setPrefSize(880,480);view.getEngine().setJavaScriptEnabled(false);
            panel.setScene(new Scene(new Group(view),880,480));return null;});
        String endpoint=com.eviware.soapui.SoapUI.class.getResource("/com/eviware/soapui/explorer/soapui-pro-api-endpoint-explorer-starter-page.html").toExternalForm();
        String offline=com.eviware.soapui.SoapUI.class.getResource("/starter-page/starter-page.html").toExternalForm();
        check(FxWebTheme.isSoapUiPage(endpoint)&&FxWebTheme.isSoapUiPage(offline),"bundled pages allowed");
        check(FxWebTheme.isSoapUiPage("https://www.soapui.org/Appindex/soapui-starterpage-new-2026.html?version=5.10.0"),"official online start page allowed");
        check(!FxWebTheme.isSoapUiPage("https://soapui.org.example.com/Appindex/soapui-starterpage-new-2026.html"),"unrelated host excluded");
        check(!FxWebTheme.isSoapUiPage("https://www.soapui.org/docs/oauth.html")&&!FxWebTheme.isSoapUiPage("https://login.example.com/"),"other web and login pages excluded");
        fx(()->{view.getEngine().load(endpoint);return null;});loaded();awaitDark(true);
        check(computed("body","background-color").equals("rgb(37, 40, 46)"),"Endpoint Explorer dark body");
        fx(()->{view.getEngine().setJavaScriptEnabled(true);
            // Execute only the bundled UI scripts. The separate Google Analytics script stays disabled.
            org.w3c.dom.NodeList scripts=view.getEngine().getDocument().getElementsByTagName("script");
            for(int i=0;i<scripts.getLength();i++) {
                org.w3c.dom.Element script=(org.w3c.dom.Element)scripts.item(i);
                String source=script.getAttribute("src"),code=script.getTextContent();
                if(source!=null && !source.isEmpty()) {
                    java.net.URL url=new java.net.URL(new java.net.URL(endpoint),source);
                    if(!"jar".equals(url.getProtocol()))throw new AssertionError("Unexpected external script");
                    try(java.io.InputStream stream=url.openStream()){code=new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
                }
                if(!code.contains("google-analytics.com"))view.getEngine().executeScript(code);
            }
            return null;});
        long inputDeadline=System.nanoTime()+5_000_000_000L;
        while(!fx(()->Boolean.TRUE.equals(view.getEngine().executeScript("!!document.querySelector('#methodEndpoint')")))&&System.nanoTime()<inputDeadline)Thread.sleep(50);
        fx(()->{view.getEngine().executeScript("window.themeTestDoc=document; var input=document.querySelector('#methodEndpoint');input.value='https://example.invalid/retained';window.themeTestInput=input; window.themeTestClicks=0; var b=document.createElement('button');b.id='theme-test-button';b.onclick=function(){window.themeTestClicks++};document.body.appendChild(b);");return null;});
        check(computed("#methodEndpoint","background-color").equals("rgb(29, 32, 37)"),"endpoint input dark");
        fx(()->{ImageIO.write(SwingFXUtils.fromFXImage(view.snapshot(null,null),null),"png",output.resolve("endpoint-dark.png").toFile());return null;});
        for(int i=0;i<3;i++) {
            SwingUtilities.invokeAndWait(()->{try{theme.setDark(false,frame);}catch(Exception e){throw new RuntimeException(e);}});awaitDark(false);
            check(fx(()->view.getEngine().getUserStyleSheetLocation())==null,"original user stylesheet restored");
            check(fx(()->Objects.equals(view.getEngine().executeScript("themeTestInput.value"),"https://example.invalid/retained")),"endpoint input unchanged");
            check(fx(()->Boolean.TRUE.equals(view.getEngine().executeScript("document===themeTestDoc"))),"web document identity unchanged");
            fx(()->{view.getEngine().executeScript("document.getElementById('theme-test-button').click()");return null;});
            check(fx(()->((Number)view.getEngine().executeScript("themeTestClicks")).intValue())==i+1,"existing click callback still works exactly once");
            SwingUtilities.invokeAndWait(()->{try{theme.setDark(true,frame);}catch(Exception e){throw new RuntimeException(e);}});awaitDark(true);
        }
        fx(()->{view.getEngine().setJavaScriptEnabled(false);view.getEngine().load(offline);return null;});loaded();awaitDark(true);
        check(computed("td","background-color").equals("rgb(37, 40, 46)"),"offline starter page dark table");
        fx(()->{ImageIO.write(SwingFXUtils.fromFXImage(view.snapshot(null,null),null),"png",output.resolve("starter-offline-dark.png").toFile());
            view.getEngine().loadContent("<html><body style='background:white'>Other embedded page</body></html>");return null;});loaded();
        long until=System.nanoTime()+3_000_000_000L;
        while(fx(()->view.getEngine().getUserStyleSheetLocation())!=null && System.nanoTime()<until)Thread.sleep(50);
        check(fx(()->view.getEngine().getUserStyleSheetLocation())==null,"navigating away removes stylesheet");
        SwingUtilities.invokeAndWait(()->{theme.close();frame.dispose();});
    }
}
