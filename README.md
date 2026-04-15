# NutriSlot

NutriSlot è un’app Android pensata per gestire un piano alimentare settimanale in modo pratico: import del piano da PDF, revisione manuale dei pasti estratti, pianificazione settimanale, lista della spesa, tracking acqua, calorie, peso e strumenti rapidi come scanner e widget. Il progetto è sviluppato in Kotlin con Jetpack Compose e persistenza locale. ([GitHub][1])

## Perché esiste

L’idea alla base di NutriSlot è semplice: un piano dieta reale non è rigido. Può capitare di consumare il pranzo di un altro giorno, spostare un pasto, segnare alimenti già usati e voler evitare duplicazioni accidentali nella settimana. NutriSlot nasce per rendere questo flusso gestibile direttamente da smartphone, con una UI moderna e dati salvati in locale. ([GitHub][2])

## Funzionalità principali

* **Piano settimanale dei pasti** con visualizzazione calendario, gestione degli slot, checklist quantità/frequenze settimanali e supporto alla modifica dei pasti. ([GitHub][3])
* **Import da file PDF** con flow dedicato: selezione file, parsing del piano, costruzione di un draft e schermata di anteprima/edit prima del salvataggio. ([GitHub][4])
* **Parsing PDF locale** basato su PDFBox Android e componenti dedicati per scansione pagina, normalizzazione del testo, parsing tabellare settimanale ed estrazione di contenuti aggiuntivi/frequenze. ([GitHub][5])
* **Lista della spesa** collegata al piano settimanale, con parsing e normalizzazione del testo per trasformare i pasti in elementi acquistabili. ([GitHub][6])
* **Scanner prodotti** con feature AI/Gemini, pensato per acquisizione fotografica, catalogazione del prodotto e integrazione con lista spesa o tracker calorie. ([GitHub][7])
* **Conta calorie**, **tracker acqua** con notifiche promemoria, e **tracker peso** con entry, summary e history. ([GitHub][8])
* **Widget home screen** per il calendario/piano pasti. ([GitHub][9])

## Stack tecnico

* **Kotlin**
* **Jetpack Compose**
* **Material 3**
* **Navigation Compose**
* **Room**
* **DataStore Preferences**
* **WorkManager**
* **PDFBox Android**
* **KSP**
* **Gemini API** per alcune feature AI dedicate ([GitHub][5])

## Architettura

Il progetto è organizzato in package separati per dominio, data layer, navigation, feature UI e widget. In particolare:

* `domain/model` contiene modelli di piano, slot, import e frequenze settimanali. ([GitHub][2])
* `data/importer` contiene la pipeline di import/parsing PDF. ([GitHub][4])
* `data/local/room` contiene entità, DAO e database locale. ([GitHub][10])
* `data/repository` contiene repository, supporti di persistenza e logica di pianificazione. ([GitHub][11])
* `ui/weeklyplan`, `ui/importfile`, `ui/importpreview`, `ui/shoppinglist`, `ui/scanner`, `ui/water`, `ui/calories`, `ui/weight` contengono le feature di presentazione principali. ([GitHub][12])
* `navigation/AppNavGraph.kt` collega tutte le schermate dell’app e definisce il flusso tra planner, spesa, acqua, scanner, calorie, peso e import. ([GitHub][13])

## Navigazione dell’app

NutriSlot usa una struttura con tre destinazioni top-level nel bottom bar:

* **Piano**
* **Spesa**
* **Acqua**

In più è presente un menu rapido in overlay per aprire:

* **Conta calorie**
* **Scanner**
* **Peso** ([GitHub][14])

## Persistenza locale

I dati principali vengono salvati in locale tramite Room. Nel database sono presenti entità dedicate per:

* piani settimanali
* slot pasto
* assegnazioni
* consumi
* opzioni del pasto
* regole del pasto
* target settimanali
* lista della spesa ([GitHub][10])

Questo rende il progetto adatto a un utilizzo offline-first, senza dipendenza da backend o autenticazione per il core dell’app. ([GitHub][10])

## Requisiti

* **Android Studio**
* **JDK 11**
* **minSdk 26**
* **targetSdk 36**
* **compileSdk 36.1**
* device/emulatore Android compatibile con Compose ([GitHub][5])

## Setup del progetto

1. Clona il repository e aprilo in Android Studio.
2. Esegui la sync Gradle.
3. Configura, se necessario, la chiave Gemini per le feature AI.
4. Avvia l’app su emulatore o dispositivo reale.

La build legge `GEMINI_API_KEY` da una di queste sorgenti:

* variabile d’ambiente
* `secrets.properties`
* `local.properties`
* `gradle.properties` utente

Il progetto include anche un controllo esplicito per evitare di salvare la chiave dentro il `gradle.properties` versionato del repository. ([GitHub][5])

### Esempio `secrets.properties`

```properties
GEMINI_API_KEY=YOUR_API_KEY_HERE
```

## Permessi e servizi

L’app dichiara il permesso Internet e il permesso per le notifiche, crea un notification channel per i reminder dell’acqua e richiede il permesso di notifica su Android 13+ all’avvio. ([GitHub][15])

## Stato del progetto

Il repository mostra un’app già strutturata su più feature, con import PDF, planner settimanale, shopping list, tracker acqua, peso, calorie, scanner AI e widget, ma senza ancora un README root che documenti in un solo punto il progetto. Questo file serve proprio a colmare quel vuoto. ([GitHub][16])

## Struttura sintetica

```text
app/
└── src/main/java/it/lagioiaproductions/nutrislot/
    ├── data/
    │   ├── ai/
    │   ├── importer/
    │   ├── local/room/
    │   ├── repository/
    │   ├── water/
    │   └── work/
    ├── domain/model/
    ├── navigation/
    ├── notifications/water/
    ├── ui/
    │   ├── calories/
    │   ├── importfile/
    │   ├── importpreview/
    │   ├── root/
    │   ├── scanner/
    │   ├── shared/
    │   ├── shoppinglist/
    │   ├── theme/
    │   ├── water/
    │   ├── weeklyplan/
    │   └── weight/
    ├── widget/
    └── MainActivity.kt
```

Struttura derivata direttamente dall’albero del repository attuale. ([GitHub][17])

## Roadmap naturale

* migliorare la documentazione root del progetto
* aggiungere screenshot/GIF delle schermate principali
* documentare in dettaglio il formato PDF supportato
* separare ulteriormente le feature AI dal core offline
* introdurre test automatici più espliciti per parsing e planner

## Licenza

Aggiungi qui la licenza del progetto se vuoi renderla esplicita nel repository. Al momento, dalla pagina GitHub del repo non risulta una licenza dichiarata nella root. ([GitHub][16])

[1]: https://raw.githubusercontent.com/StitchMl/NutriSlot/master/app/src/main/java/it/lagioiaproductions/nutrislot/navigation/AppNavGraph.kt "raw.githubusercontent.com"
[2]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/domain/model "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/domain/model at master · StitchMl/NutriSlot · GitHub"
[3]: https://github.com/StitchMl/NutriSlot/blob/master/app/src/main/java/it/lagioiaproductions/nutrislot/ui/weeklyplan/README.md "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/ui/weeklyplan/README.md at master · StitchMl/NutriSlot · GitHub"
[4]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/data/importer "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/data/importer at master · StitchMl/NutriSlot · GitHub"
[5]: https://raw.githubusercontent.com/StitchMl/NutriSlot/master/app/build.gradle.kts "raw.githubusercontent.com"
[6]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/ui/shoppinglist "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/ui/shoppinglist at master · StitchMl/NutriSlot · GitHub"
[7]: https://github.com/StitchMl/NutriSlot/blob/master/app/src/main/java/it/lagioiaproductions/nutrislot/ui/scanner/README.md "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/ui/scanner/README.md at master · StitchMl/NutriSlot · GitHub"
[8]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/ui/calories "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/ui/calories at master · StitchMl/NutriSlot · GitHub"
[9]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/widget "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/widget at master · StitchMl/NutriSlot · GitHub"
[10]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/data/local/room "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/data/local/room at master · StitchMl/NutriSlot · GitHub"
[11]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/data/repository "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/data/repository at master · StitchMl/NutriSlot · GitHub"
[12]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/ui "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/ui at master · StitchMl/NutriSlot · GitHub"
[13]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot/navigation "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot/navigation at master · StitchMl/NutriSlot · GitHub"
[14]: https://raw.githubusercontent.com/StitchMl/NutriSlot/master/app/src/main/java/it/lagioiaproductions/nutrislot/ui/root/AppRootScaffold.kt "raw.githubusercontent.com"
[15]: https://github.com/StitchMl/NutriSlot/blob/master/app/src/main/AndroidManifest.xml "NutriSlot/app/src/main/AndroidManifest.xml at master · StitchMl/NutriSlot · GitHub"
[16]: https://github.com/StitchMl/NutriSlot "GitHub - StitchMl/NutriSlot · GitHub"
[17]: https://github.com/StitchMl/NutriSlot/tree/master/app/src/main/java/it/lagioiaproductions/nutrislot "NutriSlot/app/src/main/java/it/lagioiaproductions/nutrislot at master · StitchMl/NutriSlot · GitHub"
