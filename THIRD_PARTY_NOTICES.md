# Third-party notices

Reviewed for the source publication of SoapUI System Theme 0.3.1 on 23 October 2026.

## Original project code

Original source code, tests, scripts and documentation are provided under the MIT license in [LICENSE](LICENSE), copyright (c) 2026 Vincent Vonk. Development was assisted by OpenAI Codex/ChatGPT.

MIT does not relicense SoapUI or other third-party components. The reference texts in [licenses/](licenses/) retain their own notices and terms.

This repository includes no third-party JARs, source implementations, fonts, images or web-page assets. It compiles against an existing SoapUI installation and uses its libraries at runtime. The plugin JAR contains project classes and notices, without embedding those libraries.

The original test-only com.eviware.soapui.plugins.PluginLoadTest shares the host package to exercise SoapUI's package-private loader API. It is not a copied implementation and is not included in the plugin JAR.

## Referenced components

| Component | Artifact checked | License and notice | Use |
| --- | --- | --- | --- |
| SoapUI Open Source, SmartBear and contributors | 5.10.0 | EUPL 1.1 or later; [text](licenses/SoapUI-EUPL-1.1-or-later.txt) | Plugin/application APIs and test fixtures. |
| JGoodies Looks, Karsten Lentzsch and contributors | looks-2.2.0.jar | BSD 3-Clause; [notice](licenses/JGoodies-Looks-BSD.txt) | Plastic look and feel with an original dark palette. |
| JGoodies Forms, Karsten Lentzsch and contributors | forms-1.0.7.jar | BSD 3-Clause; [notice](licenses/JGoodies-Forms-BSD.txt) | Public layout constraints for toolbar wrappers. |
| SwingX, SwingLabs and contributors | SoapUI's swingx-soapui.jar; no version in its manifest | SoapUI supplies LGPL 2.1; [supplied text](licenses/SwingX-LGPL-2.1.txt). Check the actual distribution for variant-specific notices. | Public layout types and host UI/tests. |
| RSyntaxTextArea, Robert Futrell and contributors | rsyntaxtextarea-2.5.0.jar | Modified BSD / BSD 3-Clause; [upstream 2.5.0 notice](licenses/RSyntaxTextArea-2.5.0-BSD.txt) | Public editor, syntax scheme, gutter and token APIs. |
| OpenJFX / JavaFX, Oracle and contributors | 17.0.12 in the checked SoapUI installation | GPL v2 with Classpath Exception for designated files; [license and exception](licenses/OpenJFX-17.0.12-GPL-2.0-with-Classpath-Exception.txt), [additional information](licenses/OpenJFX-17.0.12-ADDITIONAL_LICENSE_INFO.txt) | Public WebView and Swing bridge APIs. JavaFX also has component-specific third-party notices. |
| Java SE / OpenJDK | Java 17 target; checked runtime OpenJDK 17.0.12 | The checked OpenJDK runtime uses GPL v2 with Classpath Exception for designated files, plus component-specific notices. Separately installed JDKs may have different terms. | Standard Java/Swing APIs; no JDK/JRE is redistributed. |

### RSyntaxTextArea installation discrepancy

SoapUI's checked installation also has a licences/RSyntaxTextArea-LICENSE.txt containing LGPL 3.0. However, the exact 2.5.0 JAR's embedded Maven POM identifies Modified BSD, and the upstream 2.5.0 tag provides BSD 3-Clause terms. This inventory follows those version-specific sources rather than silently assigning the older installation notice. No RSyntaxTextArea binary or implementation is redistributed here. Anyone packaging that library must verify the provenance and complete notices of the exact artifact distributed.

## Notice sources

- SoapUI: [official license](https://www.soapui.org/docs/developers/license/) and the checked installation's LICENSE.txt.
- JGoodies: licences/looks-LICENSE.txt and licences/forms-LICENSE.txt in the checked SoapUI distribution.
- SwingX variant: licences/swingx-LICENSE.txt in the checked SoapUI distribution.
- RSyntaxTextArea: [upstream 2.5.0 license](https://github.com/bobbylight/RSyntaxTextArea/blob/2.5.0/distfiles/RSyntaxTextArea.License.txt) and the JAR's embedded META-INF/maven/com.fifesoft/rsyntaxtextarea/pom.xml.
- OpenJFX: [17.0.12+1 LICENSE](https://github.com/openjdk/jfx17u/blob/17.0.12%2B1/LICENSE) and [ADDITIONAL_LICENSE_INFO](https://github.com/openjdk/jfx17u/blob/17.0.12%2B1/ADDITIONAL_LICENSE_INFO).

Other libraries on the build classpath belong to the user's SoapUI installation. This inventory covers direct plugin interfaces and principal host APIs used by tests, rather than the entire SoapUI application.

## Attribution and distribution

Keep this file, the project MIT license and licenses/ with source distributions. The local build also puts them under META-INF/ in the plugin JAR. Leave third-party notices intact. Redistributing third-party binaries or copying/modifying their implementation code requires compliance with the licenses of that exact material; this project's MIT license does not replace them.

SoapUI System Theme is an independent, unofficial plugin. It is not affiliated with, sponsored by, endorsed by, or maintained by SmartBear. SoapUI and SmartBear names and marks belong to their respective owners and identify compatibility. Third-party names do not imply endorsement.
