# Naar GitHub en daarna verder werken

Dit pakket bevat de broncode van pluginversie **0.3.1**, met tests, bouwscripts en licentievermeldingen. De bestanden horen direct in de hoofdmap van je repository.

## Eerste upload

1. Pak de ZIP uit in een lege map.
2. Maak op GitHub je repository aan, bijvoorbeeld soapui-system-theme. Dit pakket bevat al een README, MIT-licentie en .gitignore; laat GitHub die niet nogmaals genereren.
3. Upload de **inhoud** naar de hoofdmap van de repository. Neem ook .gitignore, .gitattributes, docs/ en licenses/ mee. Upload de ZIP zelf niet als vervanging voor de uitgepakte bronbestanden.
4. Controleer dat GitHub bovenaan README.md, LICENSE, THIRD_PARTY_NOTICES.md, build.ps1 en src/ toont.

## Lokale clone

Clone de repository naar je normale ontwikkelmap:

~~~powershell
git clone <jouw-repository-url>
~~~

Deel daarna de GitHub-URL en het absolute pad van de lokale clone in onze chat. Dan kunnen we vanuit die bestaande repository verder werken. Een private repository moet toegankelijk zijn via je gekoppelde GitHub-account of de lokale werkomgeving.

Build-output, JAR's, ZIP's, lokale SoapUI-profielen en omgevingsbestanden staan in .gitignore. Het pakket bevat geen .git-map of bestaande Git-geschiedenis.

## Bouwen en installeren

Open PowerShell in de hoofdmap, met een JDK 17 of nieuwer en SoapUI Open Source 5.10.0 geïnstalleerd:

~~~powershell
.\build.ps1
~~~

Gebruik -SoapUiHome en -JdkHome als automatische detectie niet lukt; zie de README. De build maakt dist\soapui-system-theme-0.3.1.jar en voert de tests uit. Een Windows-desktopsessie is nodig voor de grafische tests.

Sluit SoapUI voordat je de JAR installeert of .\Install.ps1 gebruikt. Houd maar één pluginversie in je pluginmap.

## Latere updates

Werk vanuit de clone, bekijk wijzigingen met git diff en commit de bedoelde bronbestanden en documentatie. Een plugin-JAR of gebruikers-ZIP kan afzonderlijk als GitHub Release worden aangeboden, met de bijbehorende broncodeversie en licentievermeldingen.
