# Weekly Plan UI

- `calendar/`: calendario settimanale, header, griglia e supporto di presentazione.
- `checklist/`: checklist quantità/frequenze e relativi builder/modelli.
- `edit/`: dialog di modifica slot, preferenze locali e supporto personalizzazioni.
- `parser/`: parsing e visualizzazione strutturata del testo dei pasti.
- `shopping/`: estrazione e normalizzazione per lista spesa dal weekly plan.
- `slot/`: modelli slot, dialog azioni slot e card di presentazione.
- `state/`: stato aggregato della screen e one-shot events UI.
- `viewmodel/`: entry point del `WeeklyPlanViewModel` e file azione separati per concern.

Il package resta condiviso per limitare regressioni, ma la struttura fisica ora segue responsabilità chiare.
