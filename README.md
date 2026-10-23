# SoapUI System Theme 0.3.1

Een Windows-plugin voor **SoapUI Open Source 5.10.0** die automatisch de lichte of donkere **Windows-appmodus** volgt. De standaardinstelling is **Follow Windows**.
**Onafhankelijke, niet-officiële plugin.** Dit project is niet verbonden aan, gesponsord door, goedgekeurd door of onderhouden door SmartBear. SoapUI en SmartBear zijn namen/merken van hun respectieve rechthebbenden en worden gebruikt om compatibiliteit aan te geven.

De eigen projectcode valt onder de [MIT-licentie](LICENSE). Bibliotheken van SoapUI houden hun eigen licenties; zie [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Ontwikkeld met ondersteuning van OpenAI Codex/ChatGPT.

Dit GitHub-pakket bevat de broncode. Bouw de plugin eerst met `build.ps1`; een JDK 17 of nieuwer en een bestaande SoapUI 5.10.0-installatie zijn nodig. Voor de eerste upload en de lokale clone: [GitHub-instructies](docs/GITHUB_SETUP.md).

## Installeren (geen administrator nodig)

1. Bouw de plugin met `build.ps1` (zie **Zelf bouwen**) en sluit daarna SoapUI. Als je een complete binaire release gebruikt, pak die volledig uit.
2. Open met **Win+R** de map `%USERPROFILE%\.soapuios\plugins`. Maak de map aan als die nog niet bestaat.
3. Verwijder de eerder geïnstalleerde `soapui-system-theme-*.jar`, bijvoorbeeld versie 0.3.0. Er mag maar één versie van System Theme in deze map staan.
4. Kopieer `dist\soapui-system-theme-0.3.1.jar` naar die map.
5. Start SoapUI Open Source 5.10.0. Bovenaan verschijnt het menu **Theme**.
6. Laat **Theme > Follow Windows** geselecteerd.

Als alternatief voer je vanuit de uitgepakte map het PowerShell-commando `.\Install.ps1` uit. Bij een beleid dat scripts blokkeert, gebruik je de handmatige kopieerstappen hierboven.

Er worden geen SoapUI-installatiebestanden of Windows-instellingen aangepast. De plugin heeft geen eigen downloadstap en gebruikt alleen bibliotheken die SoapUI al meelevert.

## Dag en nacht

Je bestaande Windows-scheduler blijft de tijden bepalen. De plugin leest elke **3 seconden** `AppsUseLightTheme` in je gebruikersregister. Donker is waarde `0`, licht is waarde `1`. Hij volgt dus **de appmodus**, niet uitsluitend de kleur van de taakbalk of het Windows-systeemthema.

Wanneer Dark Mode X / Auto Dark Mode de appmodus van Windows verandert, schakelt een al geopende SoapUI mee. Een herstart is daarvoor niet nodig. Tijdens opstarten kan de gewone lichte interface kort zichtbaar zijn voordat de eerste meting binnen is.

Als de Windows-instelling tijdelijk niet gelezen kan worden, houdt SoapUI zijn huidige weergave en probeert de plugin het opnieuw. Je kunt ondertussen handmatig kiezen via **Theme**.

## Menu

| Keuze | Gedrag |
|---|---|
| Follow Windows | Volgt de Windows-appmodus. |
| Light (SoapUI default) | Herstelt de oorspronkelijke SoapUI-weergave en negeert de Windows-appmodus. |
| Dark | Houdt SoapUI donker, ook als Windows licht staat. |

Je keuze wordt bewaard in `%USERPROFILE%\.soapuios\system-theme.properties`. De waarde van `mode` is `SYSTEM`, `LIGHT` of `DARK`. Het bestand wordt bij een volgende start gelezen; gebruik het menu voor direct omschakelen.

## Wat deze versie aanpast

Nieuw in 0.3.1: de pictogrammen voor minimaliseren, maximaliseren, terugzetten en sluiten van SoapUI's interne vensters krijgen donkere vlakken met lichte symbolen. Dit werkt ook na maximaliseren en terugzetten. De echte knoppen en hun acties blijven behouden; lichtmodus herstelt de oorspronkelijke pictogrammen. Dit betreft de interne SoapUI-vensters, niet de native Windows-titelbalk.

Toegevoegd in 0.3.0:

- Donkere, leesbare koppen in Preferences, New SOAP Project en andere dialogen met SoapUI's DescriptionPanel. Het verloop gebruikt intern de voorgrondkleur als schilderkleur; in donkerstand wordt dat verloop tijdelijk vervangen door een effen achtergrond.
- About SoapUI krijgt een donker informatiepaneel. Versie, auteursrecht, builddatum en hyperlinks blijven behouden. De witte achtergrondafbeelding wordt in donkerstand weggelaten. Bij lichtmodus komt het oorspronkelijke HTML-document met die afbeelding terug.
- Het standaard helpicoon krijgt een donker vlak en een lichtblauw vraagteken. De oorspronkelijke actie en afmetingen blijven behouden; lichtmodus herstelt het oorspronkelijke icoon.
- Endpoint Explorer en de bekende online/lokale SoapUI-startpagina krijgen een apart donker JavaFX-webstylesheet. Dit verandert geen HTML-documenten, invoerwaarden of JavaScript-acties en laadt de pagina niet opnieuw. Bij lichtmodus wordt het oorspronkelijke gebruikersstylesheet hersteld. Andere webpagina's, zoals OAuth-loginpagina's, vallen buiten deze ondersteuning.

De verbeteringen uit 0.2.0 blijven behouden: de hoofdwerkbalk, navigatorbalk, inspector- en logbalken en de balk in het projectoverzicht krijgen een donkere achtergrond, ondanks SoapUI's vaste witte schildercode. De echte balken en knoppen blijven bestaan, met dezelfde acties en indeling. In lichtmodus worden de tijdelijke tekenlagen weer verwijderd.

Donkere, neutrale pixels in pictogrammen krijgen meer contrast zonder het bronpictogram te wijzigen. Gekleurde signalen, zoals rode en groene symbolen, behouden hun kleur. Tabellen blijven donker als SoapUI achteraf lichte rijstrepen wil toepassen. Ook de tree-renderer en scrollbalken worden aangepast en bij lichtmodus hersteld.

- Standaard Swing-onderdelen: panelen, menu's, tekstvelden, boomweergave, tabellen en dialoogonderdelen.
- De XML/JSON/Groovy/SQL-editors die SoapUI met RSyntaxTextArea opbouwt: achtergrond, tekst, syntaxkleuren, cursor, selectie en regelnummers.
- De witte hoofdbalk die SoapUI zelf schildert: in donkerstand komt er een standaard Swing-menubalk voor terug, met dezelfde menu's en acties. Bij lichtmodus wordt de oorspronkelijke balk teruggezet.
- Nieuw geopende Swing-vensters en componenten worden tijdens donkerstand ook aangepast.
- Bestaande request-/response- en scriptdocumenten, cursorselecties en projectmodellen worden niet opnieuw opgebouwd. Alleen het niet-bewerkbare About-informatiepaneel gebruikt tijdelijk een apart weergavedocument.

De lichte weergave bewaart bestaande componentkleuren. Componenten die pas tijdens donkerstand worden gemaakt, krijgen bij terugschakelen de lichte standaardkleuren waar een eerdere lichte toestand ontbreekt.

## Grenzen van versie 0.3.1

Dit is een eigen plugin, geen door SmartBear onderhouden uitbreiding. De build en tests gebruiken de echte SoapUI 5.10.0-bibliotheken en de meegeleverde Java 17-runtime. Naast losse componenten wordt nu de echte SoapUI-desktop met een synthetisch project opgestart en visueel gecontroleerd. Niet ieder SoapUI-scherm of iedere andere plugin is afzonderlijk getest.

Sommige oude pictogrammen, grafieken, onderdelen met eigen schildercode, native Windows-vensters en andere ingesloten webpagina's kunnen hun eigen kleuren behouden. De native titelbalk wordt door Windows/Java getekend; de plugin vervangt geen vensterdecoratie. Logo's en afbeeldingen in webpagina's worden niet omgekleurd. Kleuren met een betekenis, zoals statuskleuren, worden zoveel mogelijk behouden. Je kunt altijd naar **Light** terugschakelen.

Endpoint Explorer en de lokale offline-startpagina zijn met echte JavaFX-rendering getest. De online startpagina is via haar bekende SoapUI-URL ondersteund, maar kon in deze testomgeving niet via internet worden geladen en is dus niet volledig visueel gevalideerd. Als SmartBear die pagina of URL wijzigt, kan aanvullende opmaak nodig zijn. Tijdens opstarten of paginaladen kan kort de lichte weergave zichtbaar zijn voordat de plugin/het stylesheet actief is.

De plugin is voor Windows en vereist SoapUI Open Source 5.10.0 of hoger volgens de pluginmetadata. **Alleen 5.10.0 is gevalideerd.** Compatibiliteit met latere versies moet opnieuw worden gecontroleerd. Er is geen afzonderlijke ondersteuning voor ReadyAPI.

## Verwijderen

Sluit SoapUI, verwijder `%USERPROFILE%\.soapuios\plugins\soapui-system-theme-0.3.1.jar` en start SoapUI opnieuw. Je kunt ook `.\Uninstall.ps1` gebruiken. Alleen de eigen JAR wordt verwijderd. De kleine voorkeurenfile blijft bewaard; je mag `system-theme.properties` ook verwijderen.

## Zelf bouwen

Nodig: Windows, een desktop-sessie, een **JDK 17 of nieuwer** en SoapUI Open Source 5.10.0. Maven en internettoegang zijn niet nodig.

```powershell
.\build.ps1 -SoapUiHome 'C:\Program Files\SmartBear\SoapUI-5.10.0'
```

De build probeert de JDK en SoapUI automatisch te vinden; met alleen `.\build.ps1` volstaat dat vaak. Geef desgewenst `-JdkHome` mee met de map van je geïnstalleerde JDK. `SoapUiHome` is de map met `bin` en `lib`. Een bestaande installatie kan na een upgrade nog een oudere mapnaam hebben; de build controleert daarom het bestand `bin\soapui-5.10.0.jar`.

De build maakt `dist\soapui-system-theme-0.3.1.jar`. `--release 17` bewaakt compatibiliteit met SoapUI's Java 17. De tests gebruiken de JRE van SoapUI zelf. Ze werken in `build\test-home` en `build\ui-home` en wijzigen je echte Windows-thema of je SoapUI-projecten niet. Voor grafische regressietests worden testvensters buiten het scherm geplaatst, zonder toetsenbordfocus, en naar PNG gerenderd. De echte desktoptest gebruikt een tijdelijk voorkeurenprofiel en een testimplementatie van Java Preferences, zodat het gebruikersregister niet voor Java-voorkeuren wordt gebruikt. De oude JGoodies-versie van SoapUI vraagt een desktop-sessie, ook bij het renderen naar een afbeelding.

## Pakketinhoud

- `dist/`: de installeerbare plugin-JAR wordt hier door de build gemaakt; deze map zit niet in het broncodepakket.
- `src/main/java/`: volledige broncode.
- `src/test/java/`: regressie- en integratietests.
- `build.ps1`: lokale build zonder downloads.
- `test-ui.ps1`: test de echte SoapUI-desktop met de gebouwde plugin en een afzonderlijk testprofiel.
- `Install.ps1`, `Uninstall.ps1`: optionele installatiehulpen.
- `TEST-REPORT.md`: uitgevoerde controles en beperkingen.
- `build/`: de tests maken hier hun tijdelijke profielen en PNG-beelden; deze bestanden staan buiten Git en het broncodepakket.
- `LICENSE`, `THIRD_PARTY_NOTICES.md`, `licenses/`: projectlicentie en afzonderlijke bibliotheekvermeldingen.
- `.gitignore`, `.gitattributes`: uitsluitingen en consistente regeleinden voor Git.
- `docs/GITHUB_SETUP.md`, `CHANGELOG.md`: GitHub-instructies en versiehistorie.

## Technische bronnen

- [SoapUI 5.10.0-pluginloader](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/plugins/PluginLoader.java)
- [SoapUI-plugininterface](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/plugins/Plugin.java)
- [SoapUI-standaardthema](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/StandaloneSoapUICore.java)
- [SoapUI-interface en vaste witte achtergronden](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/SoapUI.java)
- [SoapUI-dialoogkoppen](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/support/DescriptionPanel.java)
- [JavaFX-browsercomponent](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/support/components/EnabledWebViewBasedBrowserComponent.java)

Er worden geen SoapUI- of andere derdepartij-JAR's met dit pakket verspreid.


## Publiceren en bijdragen

Upload de uitgepakte bronbestanden naar de hoofdmap van je repository. De ZIP bevat geen Git-geschiedenis, installatiebestanden van SoapUI of gegenereerde binaries. Bewaar LICENSE, THIRD_PARTY_NOTICES.md en licenses/ bij de broncode. Een lokaal gebouwde plugin-JAR bevat deze vermeldingen ook onder META-INF/.

Voor wijzigingen: bouw en voer de tests uit met build.ps1. Beschrijf bij UI-problemen je SoapUI-versie, Java-versie, thema-instelling en het betrokken scherm. Gebruik synthetische voorbeelden zonder credentials of echte requestdata.
