# **Anforderungskatalog: AI-Gesichtszug-Erkennungs-App**

Dieses Dokument legt die funktionalen und nicht-funktionalen Anforderungen für eine Android-App zur **Erkennung menschlicher Gesichtszüge** fest.  
Die App nutzt die Kamera und ein lokales neuronales Netz, um **offene Augen** und ein **Lächeln** in Echtzeit zu erkennen und anzuzeigen.

---

## **1. Muss-Anforderungen (Must Have – M)**

| ID   | Anforderung              | Typ              | Beschreibung |
| :--- | :----------------------- | :-------------- | :----------- |
| M-01 | **Kamerazugriff**        | Funktional      | Die App muss auf die Kamera des Android-Geräts zugreifen und den Live-Kamerastream anzeigen können. |
| M-03 | **Gesichtszug-Erkennung** | Funktional     | Die App muss innerhalb eines erkannten Gesichts erkennen können, ob **die Augen offen sind** und ob **ein Lächeln vorliegt**. |
| M-04 | **Zustandsanzeige**      | Funktional      | Für jedes erkannte Gesicht muss der erkannte Zustand („Augen offen“, „Lächeln erkannt“ oder „nicht erkannt“) deutlich angezeigt werden. |
| M-05 | **Offline-Fähigkeit**    | Funktional      | Das neuronale Netz muss lokal auf dem Gerät gespeichert sein, damit die Gesichtszug-Erkennung **ohne Internetverbindung** funktioniert. |
| M-06 | **Basis-Stabilität**     | Nicht-Funktional | Die App darf während der Nutzung der Kernfunktionalitäten (Kamerazugriff und Erkennung) unter normalen Bedingungen nicht abstürzen. |

---

## **2. Soll-Anforderungen (Should Have – S)**

| ID   | Anforderung              | Typ              | Beschreibung |
| :--- | :----------------------- | :-------------- | :----------- |
| S-01 | **Fehlermeldungen**      | Funktional      | Bei Fehlern (z. B. fehlende Kameraberechtigung oder Fehler beim Laden des Modells) muss eine verständliche Fehlermeldung angezeigt werden. |
| S-02 | **Responsive UI**        | Nicht-Funktional | Die Benutzeroberfläche muss auf verschiedenen Android-Geräten (Smartphones/Tablets) korrekt und ohne Verzerrungen dargestellt werden. |
| S-03 | **Berechtigungsmanagement** | Funktional   | Die App muss erforderliche Android-Berechtigungen (insbesondere CAMERA) gemäß den Betriebssystemrichtlinien anfordern und verwalten. |
| S-04 | **Minimum SDK-Kompatibilität** | Nicht-Funktional | Die App muss auf allen Geräten laufen, die mindestens das definierte minimale Android SDK unterstützen (z. B. API Level 26 / Oreo). |

---

## **3. Kann-Anforderungen (Could Have – K)**

| ID   | Anforderung                 | Typ              | Beschreibung |
| :--- | :-------------------------- | :-------------- | :----------- |
| K-01 | **Empfindlichkeit anpassen** | Funktional      | Der Benutzer kann die **Erkennungsschwelle** für „Augen offen“ und „Lächeln“ in den Einstellungen anpassen. |
| K-02 | **Erkannte Gesichter speichern** | Funktional  | Der Benutzer kann ein Bild des erkannten Gesichts (inkl. Markierungen und Zustandsanzeige) speichern. |
| K-03 | **Historie der Erkennungen** | Funktional     | Die App kann eine einfache Historie der zuletzt erkannten Gesichtszüge führen. |
| K-04 | **Frontkamera-Unterstützung** | Funktional    | Die App kann optional auch die Frontkamera für Selfie-Erkennung nutzen. |
| K-05 | **Dark Mode**              | Nicht-Funktional | Die App sollte einen optionalen Dark Mode unterstützen. |
| K-06 | **Echtzeit-Verarbeitung**  | Funktional      | Der Kamerastream muss kontinuierlich und in Echtzeit an das neuronale Netz übergeben werden. |

---

## Android SDK Einrichtung

Damit das Projekt gebaut werden kann, muss Gradle den Pfad zum lokalen Android SDK kennen. Dieser Pfad wird **nicht** mehr im Repository hinterlegt. Bitte lege deshalb vor dem ersten Build eine Datei `local.properties` im Projektstamm an (oder exportiere die Variable `ANDROID_HOME`) und trage dort beispielsweise folgenden Inhalt ein:

```
sdk.dir=/pfad/zu/deinem/Android/Sdk
```

Weitere Informationen findest du in der offiziellen [Android Studio Dokumentation](https://developer.android.com/studio/intro/update#sdk-manager). Wenn der Pfad korrekt gesetzt ist, lässt sich `./gradlew assembleDebug` ohne die Fehlermeldung „SDK location not found“ ausführen.

---

## Git-Tipps für lokale Gradle-Daten

Beim Arbeiten mit dem Projekt erstellt Gradle im Projektstamm einen Ordner `.gradle/` sowie die Datei `local.properties`. Beide Artefakte werden automatisch generiert und sind bewusst im `.gitignore` eingetragen. Falls du Änderungen vom Remote-Repository holen möchtest (`git pull`) und dabei Fehlermeldungen wegen lokaler Änderungen in diesen Dateien erhältst, kannst du sie gefahrlos entfernen oder sichern:

1. **Änderungen sichern (optional):** `git stash push --include-untracked`
2. **Lokale Gradle-Daten entfernen:** `rm -rf .gradle local.properties`
3. **Danach erneut pullen:** `git pull`

Beim nächsten Build werden alle benötigten Dateien automatisch neu erzeugt.
