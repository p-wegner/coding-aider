# Plan: GitHub Copilot CLI als Aider-Ersatz integrieren

Der Plan analysiert den Austausch von Aider durch `gh copilot` CLI als Backend. Da beide Tools fundamental unterschiedliche Designziele haben (Aider: direktes Code-Editing; `gh copilot suggest`: Shell-Command-Vorschläge), ist dies eine **tiefgreifende Umgestaltung**. Der Kern-Ansatz wäre eine neue Execution Strategy mit Pflicht-"Plugin-based Edits" und Prompt-Engineering für Datei-Inhalte.

## Schritte

1. **Neue Strategy implementieren**: `GithubCopilotCliExecutionStrategy` in `executors/strategies/` erstellen, die `buildCommand()` auf `gh copilot suggest -t shell "<prompt>"` mappt statt `aider`-Flags.

2. **Datei-Kontext umbauen**: Da `gh copilot` keine `--file`-Argumente kennt, müssen Dateiinhalte in den Prompt injiziert werden (ähnlich wie es `pluginBasedEdits` + `PLUGIN_BASED_EDITS_INSTRUCTION` in `AiderDefaults` schon andeutet) – Plugin-based Edits wäre hier **zwingend**, nicht optional.

3. **Authentifizierung ersetzen**: `ApiKeyChecker` und `AiderSetupPanel` in `settings/` um GitHub-Auth-Prüfung (`gh auth status`) erweitern statt API-Key-Felder – kein OPENAI_API_KEY, etc.

4. **`CommandExecutor` erweitern**: In `CommandExecutor.kt` die Strategy-Auswahl um einen dritten Zweig (`useCopilotCli`) ergänzen; Sidecar- und Docker-Pfade für Copilot CLI deaktivieren/sperren.

5. **Settings anpassen**: In `AiderSettings` und `AiderDefaults` ein `ghExecutablePath`-Feld ergänzen; Aider-spezifische Optionen (edit format, auto-commits, repo-map, docker) für Copilot-Modus ausblenden.

6. **`AiderMode`-Einschränkungen**: In `AiderMode.kt` und `AiderInputDialog` ARCHITECT, STRUCTURED und SHELL für Copilot CLI deaktivieren, da `gh copilot suggest` nur einstufige Prompts kennt – kein mehrstufiger Dialog, kein Sidecar-Protokoll.

## Weitere Überlegungen

1. **Fundamentale Einschränkung von `gh copilot`**: `gh copilot suggest` ist für Shell-Command-Vorschläge, nicht für Code-Edits konzipiert – der eigentliche Output wäre ein Shell-Befehl (z. B. `sed`, `awk`, `patch`), kein SEARCH/REPLACE-Block. Alternativ könnte man `gh api` direkt gegen die Copilot Chat API aufrufen, was eher Aiders Verhalten abbildet. Präferenz: direkter API-Aufruf vs. CLI-Wrapper?

2. **Modell-Kontrolle entfällt**: GitHub steuert, welches Modell Copilot nutzt – `commandData.llm`, `CustomLlmProviderService` und `LlmProviderType`-Kaskade werden für dieses Backend weitgehend irrelevant. Sollen diese Settings für andere Backends erhalten bleiben (paralleler Betrieb) oder wird Aider komplett ersetzt?

3. **History-Kompatibilität**: `AiderHistoryService` liest `.aider.input.history` und `.aider.chat.history.md` – Copilot CLI produziert kein ähnliches Format. Ein neues History-Format oder der Verzicht auf die History-Funktion für Copilot-Befehle muss entschieden werden.

