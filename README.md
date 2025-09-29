# **Anforderungskatalog: AI-Gesichtszug-Erkennungs-App**

Dieses Dokument legt die funktionalen und nicht-funktionalen Anforderungen für eine Android-App zur **Erkennung menschlicher Gesichtszüge** fest.  
Die App nutzt die Kamera und ein lokales neuronales Netz, um **offene Augen** und ein **Lächeln** in Echtzeit zu erkennen und anzuzeigen.

---

## **1. Muss-Anforderungen (Must Have – M)**

Diese Anforderungen sind zwingend erforderlich, um eine funktionsfähige und brauchbare App bereitzustellen. Sie definieren die Kernfunktionalität.

| ID   | Anforderung              | Typ              | Beschreibung |
| :--- | :----------------------- | :-------------- | :----------- |
| M-01 | **Kamerazugriff**        | Funktional      | Die App muss auf die Kamera des Android-Geräts zugreifen und den Live-Kamerastream anzeigen können. |
| M-02 | **Gesichtserkennung**    | Funktional      | Die App muss Gesichter im Kamerabild erkennen und deren Position visuell hervorheben (z. B. durch Bounding Boxes oder Markierungen). |
| M-03 | **Gesichtszug-Erkennung** | Funktional     | Die App muss innerhalb eines erkannten Gesichts erkennen können, ob **die Augen offen sind** und ob **ein Lächeln vorliegt**. |
| M-04 | **Zustandsanzeige**      | Funktional      | Für jedes erkannte Gesicht muss der erkannte Zustand („Augen offen“, „Lächeln erkannt“ oder „nicht erkannt“) deutlich angezeigt werden. |
| M-05 | **Offline-Fähigkeit**    | Funktional      | Das neuronale Netz muss lokal auf dem Gerät gespeichert sein, damit die Gesichtszug-Erkennung **ohne Internetverbindung** funktioniert. |
| M-06 | **Basis-Stabilität**     | Nicht-Funktional | Die App darf während der Nutzung der Kernfunktionalitäten (Kamerazugriff und Erkennung) unter normalen Bedingungen nicht abstürzen. |

---

## **2. Soll-Anforderungen (Should Have – S)**

Diese Anforderungen verbessern die Benutzerfreundlichkeit und Qualität der App. Ihre Nichterfüllung führt nicht zum Scheitern des Projekts, reduziert aber den Nutzen.

| ID   | Anforderung              | Typ              | Beschreibung |
| :--- | :----------------------- | :-------------- | :----------- |
| S-01 | **Fehlermeldungen**      | Funktional      | Bei Fehlern (z. B. fehlende Kameraberechtigung oder Fehler beim Laden des Modells) muss eine verständliche Fehlermeldung angezeigt werden. |
| S-02 | **Responsive UI**        | Nicht-Funktional | Die Benutzeroberfläche muss auf verschiedenen Android-Geräten (Smartphones/Tablets) korrekt und ohne Verzerrungen dargestellt werden. |
| S-03 | **Berechtigungsmanagement** | Funktional   | Die App muss erforderliche Android-Berechtigungen (insbesondere CAMERA) gemäß den Betriebssystemrichtlinien anfordern und verwalten. |
| S-04 | **Minimum SDK-Kompatibilität** | Nicht-Funktional | Die App muss auf allen Geräten laufen, die mindestens das definierte minimale Android SDK unterstützen (z. B. API Level 26 / Oreo). |

---

## **3. Kann-Anforderungen (Could Have – K)**

Diese Anforderungen sind optional oder für zukünftige Versionen vorgesehen. Sie bieten zusätzlichen Komfort oder erweiterte Funktionen.

| ID   | Anforderung                 | Typ              | Beschreibung |
| :--- | :-------------------------- | :-------------- | :----------- |
| K-01 | **Empfindlichkeit anpassen** | Funktional      | Der Benutzer kann die **Erkennungsschwelle** für „Augen offen“ und „Lächeln“ in den Einstellungen anpassen. |
| K-02 | **Erkannte Gesichter speichern** | Funktional  | Der Benutzer kann ein Bild des erkannten Gesichts (inkl. Markierungen und Zustandsanzeige) speichern. |
| K-03 | **Historie der Erkennungen** | Funktional     | Die App kann eine einfache Historie der zuletzt erkannten Gesichtszüge führen. |
| K-04 | **Frontkamera-Unterstützung** | Funktional    | Die App kann optional auch die Frontkamera für Selfie-Erkennung nutzen. |
| K-05 | **Dark Mode**              | Nicht-Funktional | Die App sollte einen optionalen Dark Mode unterstützen. |
| K-06 | **Echtzeit-Verarbeitung**  | Funktional      | Der Kamerastream muss kontinuierlich und in Echtzeit an das neuronale Netz übergeben werden. |
