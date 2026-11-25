# Case Mail Komponente

Ein kompaktes E-Mail-Modul zum Senden und Empfangen von Nachrichten, die einem bestimmten Ivy-Case zugeordnet sind. Alle ausgehenden E-Mails werden automatisch mit dem jeweiligen Case verknüpft – so bleibt die gesamte Kommunikation jederzeit nachvollziehbar und zentral im Workflow verfügbar.

Der Case Mail Komponente unterstützt das Versenden, Empfangen, Beantworten, Weiterleiten und erneute Versenden von E-Mails.  
- Eine Listenansicht zeigt die wichtigsten Informationen wie Datum, Absender, Empfänger und Betreff.  
- Detailansichten und die Integration in Prozesse sorgen für eine lückenlose Nachverfolgung.  
- Eingaben werden validiert, Fehler automatisch behandelt und bei Bedarf wird fehlgeschlagenes Versenden erneut versucht; fehlgeschlagene Nachrichten erzeugen eine Admin-Aufgabe.  
- Inhalt und Anhänge bleiben bei Antworten, Weiterleitungen und erneutem Versand vollständig erhalten.  

## Demo
### E-Mail-Listenansicht
Übersicht aller E-Mails zu einem Fall.  

![Alt text](images/email-list.png)

### E-Mail-Details
Anzeige der vollständigen Informationen zu einer ausgewählten Nachricht.  

![Alt text](images/email-details.png)

### Neue E-Mail
- Erstellen und Versenden neuer Nachrichten.  
- Validierungen:  
  - `From`: Pflichtfeld; gültige Adresse erforderlich.  
  - `To`: Pflichtfeld; gültige Liste von Adressen erforderlich.  
  - `CC`: Optional; falls angegeben, gültige Liste von Adressen.  

![Alt text](images/new-email.png)

### Antwort-E-Mail
Automatische Übernahme der wichtigsten Daten der ursprünglichen Nachricht:  
- `Subject`: wird mit `RE:` ergänzt  
- `Body`: enthält die ursprüngliche Nachricht mit Absender, Datum, Empfänger, Betreff und Text  

![Alt text](images/reply-email.png)

### E-Mail weiterleiten
Weiterleitung eingegangener Nachrichten:  
- `From`: ursprünglicher Absender  
- `To`: vom Benutzer definiert  
- `Subject`: wird mit `FW:` ergänzt  
- `Body`: enthält die gesamte Originalnachricht  
- Anhänge: werden übernommen  

![Alt text](images/forward.png)

### E-Mail erneut senden
- Verfügbar nur für Nachrichten mit Status `Sent`  
- Sendet eine E-Mail erneut mit denselben Daten (Absender, Empfänger, Betreff, Text, Anhänge)  
- Der Nachrichtenkörper enthält einen Hinweis, dass es sich um eine Kopie handelt  

![Alt text](images/resend-confirmation.png)  
![Alt text](images/resend-email.png)

### Fehlerbehandlung
- Automatischer Wiederholungsmechanismus:  
  - Anzahl (`mailLoopRepeatNumber`) und Intervall (`mailLoopRepeatDelay`) konfigurierbar  
- Scheitern alle Versuche, wird eine Admin-Aufgabe erstellt  

### Admin-Aufgaben
- **Abbrechen:** Aufgabe beenden  
- **Erneut versuchen:** Versand erneut starten; bei Fehlschlag greift die Wiederholungslogik und ggf. wird eine neue Aufgabe erzeugt  

![Alt text](images/admin-task.png)  
![Alt text](images/admin-task-detail.png)

## Einrichtung
1. Maximale Größe des Request-Bodys festlegen  

   Bestimmt, wie groß der zwischengespeicherte/speicherbare Request-Body sein darf, z. B. bei:  
   - FORM- oder CLIENT-CERT-Authentifizierung  
   - HTTP/1.1-Upgrade-Requests  

   **Konfiguration:**  
   - In `ivy.yaml`:  
     ```yaml
     Http:
       MaxPostSize: 2097152
     ```  
     👉 Referenz: [Axon Ivy Docs – ivy.yaml](https://developer.axonivy.com/doc/13.2.0/engine-guide/configuration/files/ivy-yaml.html)

   - In der **nginx**-Konfiguration:  
     ```nginx
     client_max_body_size 150M;
     ```

2. Folgende Projektvariablen setzen:  
