# MarkdownViewer Implementation Issues

## Übersicht

Diese Datei dokumentiert alle identifizierten Probleme in der aktuellen MarkdownViewer-Implementierung. Die Issues sind nach Kategorien und Priorität sortiert.

## 1. Threading und EDT-Verletzungen

### 1.1 Kritische Threading-Probleme

| Problem | Beschreibung | Auswirkung | Priorität |
|---------|--------------|------------|-----------|
| **Timer-Missbrauch** | `java.util.Timer` wird in `MarkdownViewer.setMarkdown()` verwendet, aber diese sind nicht daemon threads | Verhindert JVM shutdown, Thread-Leaks | HOCH |
| **EDT-Verletzungen** | Swing-Komponenten werden von Timer-Threads ohne `SwingUtilities.invokeLater` manipuliert | Zufällige "must be called on EDT" Exceptions | HOCH |
| **Doppelte invokeLater** | In `JcefMarkdownRenderer.executeJavaScript()` wird `ApplicationManager.invokeLater` in bereits EDT-Code verwendet | Kann JavaScript-Injektionen neu ordnen | MITTEL |

### 1.2 Race Conditions

| Problem | Beschreibung | Auswirkung |
|---------|--------------|------------|
| **Renderer-Initialisierung** | `loadCompleted` und `pendingContent` können gleichzeitig von verschiedenen Threads modifiziert werden | Doppelte Content-Updates, inkonsistenter Zustand |
| **Content-Updates** | Mehrere Timer können gleichzeitig `setMarkdown()` aufrufen | Überlappende Updates, Performance-Probleme |

## 2. Ressourcen-Management

### 2.1 Memory Leaks

| Problem | Beschreibung | Lösung |
|---------|--------------|--------|
| **Theme-Listener** | `MarkdownThemeManager` Listener werden nie entfernt | Disposable-Pattern verwenden |
| **Timer-Cleanup** | Timer in `MarkdownViewer` werden nicht gecancelt | Timer-Referenzen speichern und cleanup |
| **JCEF-Ressourcen** | `JBCefJSQuery` wird erstellt aber nie verwendet | Entfernen oder korrekt verwenden |

### 2.2 Dispose-Probleme

| Problem | Beschreibung | Auswirkung |
|---------|--------------|------------|
| **Dispose-Threading** | `dispose()` kann off-EDT aufgerufen werden | Undefined behavior bei Swing/JCEF calls |
| **Incomplete Cleanup** | Nicht alle Ressourcen werden in `dispose()` bereinigt | Memory leaks, zombie threads |

## 3. Performance-Probleme

### 3.1 Ineffiziente Updates

| Problem | Beschreibung | Auswirkung |
|---------|--------------|------------|
| **Vollständige DOM-Ersetzung** | `innerHTML` wird bei jedem Update komplett ersetzt | Verlust von Scroll-Position, Panel-Zuständen |
| **Markdown-Reprocessing** | Bei Theme-Änderungen wird komplettes Markdown neu verarbeitet | Unnötige CPU-Last |
| **Multiple Timer** | Für jeden `setMarkdown()` Aufruf werden neue Timer erstellt | Ressourcen-Verschwendung |

### 3.2 String-Handling

| Problem | Beschreibung | Auswirkung |
|---------|--------------|------------|
| **JavaScript-Escaping** | Große HTML-Strings werden mehrfach kopiert für Escaping | Memory spikes bei großen Logs |
| **Base64-Encoding** | Template wird als Base64 data URL geladen | 2MB Limit in CEF, kann silent fails verursachen |

## 4. UX und Funktionalitäts-Gaps

### 4.1 Smart Scrolling Defekte

| Problem | Beschreibung | PRD-Referenz |
|---------|--------------|--------------|
| **Scroll-State-Verlust** | `updateContent()` resettet Scroll-Position bevor `isScrolledToBottom()` aufgerufen wird | "Smart Autoscrolling" |
| **Panel-State-Verlust** | Collapsible Panel Zustände gehen bei Updates verloren | "Preserves expansion state" |
| **Fehlende Animations** | Keine CSS-Transitions für Panel-Animationen | "Smooth animations" |

### 4.2 Accessibility-Probleme

| Problem | Beschreibung | PRD-Referenz |
|---------|--------------|--------------|
| **Keyboard Navigation** | Keine Keyboard-Shortcuts für Panels | "Keyboard Navigation" |
| **Focus Management** | Kein korrektes Focus-Handling | "Focus Management" |
| **ARIA-Attribute** | Fehlende Accessibility-Attribute | Accessibility Standards |

## 5. Code-Qualität und Wartbarkeit

### 5.1 Architektur-Probleme

| Problem | Beschreibung | Auswirkung |
|---------|--------------|------------|
| **Tight Coupling** | `MarkdownViewer` ist stark gekoppelt mit Timer-Logik | Schwer testbar, fragil |
| **Mixed Responsibilities** | Rendering, Timing und State-Management vermischt | Unklare Verantwortlichkeiten |
| **Fallback-Inkonsistenz** | JCEF und Fallback-Renderer haben unterschiedliche APIs | Inkonsistente UX |

### 5.2 Error Handling

| Problem | Beschreibung | Auswirkung |
|---------|--------------|------------|
| **Silent Failures** | Viele Exceptions werden nur geloggt, nicht behandelt | Benutzer sieht keine Fehlermeldungen |
| **Inconsistent Logging** | Mix aus `println` und Logger | Schwer zu debuggen |
| **Missing Validation** | Keine Input-Validierung für Markdown-Content | Potentielle Security-Issues |

## 6. JavaScript-spezifische Probleme

### 6.1 Template-Defekte

| Problem | Beschreibung | Sicherheitsrisiko |
|---------|--------------|-------------------|
| **XSS-Vulnerabilität** | `escapeJsString()` escaped nicht alle gefährlichen Zeichen | HOCH |
| **Script-Injection** | `</script>` Sequenz wird nicht korrekt escaped | HOCH |
| **Dead Code** | `originalUpdateContent` wird definiert aber nie verwendet | Verwirrung |

### 6.2 Panel-ID-Algorithmus

| Problem | Beschreibung | Auswirkung |
|---------|--------------|------------|
| **Instabile IDs** | Panel-IDs basieren auf `innerHTML` das sich ständig ändert | Panel-States gehen verloren |
| **Hash-Kollisionen** | Einfacher Hash-Algorithmus kann kollidieren | Falsche Panel-States |

## 7. Konfiguration und Settings

### 7.1 DevTools-Integration

| Problem | Beschreibung | PRD-Gap |
|---------|--------------|---------|
| **Settings-Abhängigkeit** | DevTools nur verfügbar wenn Setting aktiviert | "On-demand debugging" |
| **Fehlende Runtime-Controls** | Keine Möglichkeit DevTools zur Laufzeit zu aktivieren | Debugging-Workflow |

## 8. Empfohlene Lösungsansätze

### 8.1 Sofortige Fixes (Quick Wins)

1. **Timer-Ersetzung**: Alle `java.util.Timer` durch `AppExecutorUtil.getAppScheduledExecutorService()` ersetzen
2. **EDT-Compliance**: Alle Swing-Operationen in `SwingUtilities.invokeLater` wrappen
3. **Disposable-Pattern**: Alle Listener mit Disposable registrieren
4. **JavaScript-Escaping**: Robustes JSON-Escaping implementieren

### 8.2 Architektur-Verbesserungen

1. **State-Management**: Separates State-Management für Panel-Zustände und Scroll-Position
2. **Background-Processing**: Markdown-Processing in Background-Thread verlagern
3. **Incremental Updates**: DOM-Updates statt kompletter Ersetzung
4. **Unified API**: Konsistente API zwischen JCEF und Fallback-Renderer

### 8.3 Performance-Optimierungen

1. **Content-Diffing**: Nur geänderte Teile des Contents updaten
2. **CSS-Theme-Switching**: Theme-Änderungen über CSS statt Markdown-Reprocessing
3. **Lazy-Loading**: Große Inhalte erst bei Bedarf rendern
4. **Debounced-Updates**: Rapid Updates zusammenfassen

## 9. Test-Strategie

### 9.1 Unit Tests

- Timer-Cleanup-Verhalten
- Thread-Safety von State-Updates
- JavaScript-Escaping-Funktionen
- Panel-ID-Stabilität

### 9.2 Integration Tests

- JCEF vs Fallback-Renderer Konsistenz
- Theme-Switching-Verhalten
- Large-Content-Handling
- Memory-Leak-Detection

### 9.3 Performance Tests

- Update-Performance bei großen Dokumenten
- Memory-Usage bei rapid Updates
- Scroll-Performance mit vielen Panels
- JavaScript-Execution-Time

## 10. Prioritäten-Matrix

| Kategorie | Kritisch | Hoch | Mittel | Niedrig |
|-----------|----------|------|--------|---------|
| **Security** | XSS-Fixes | - | - | - |
| **Stability** | EDT-Violations, Timer-Leaks | Memory-Leaks | Race-Conditions | - |
| **Performance** | - | DOM-Updates | Theme-Switching | String-Handling |
| **UX** | - | Smart-Scrolling | Panel-Animations | Keyboard-Nav |
| **Maintainability** | - | Architecture | Error-Handling | Code-Style |

Diese Analyse zeigt, dass die aktuelle Implementierung mehrere kritische Probleme hat, die die Stabilität und Sicherheit beeinträchtigen. Die Threading-Probleme und XSS-Vulnerabilitäten sollten mit höchster Priorität behoben werden.
