Below is a consolidated **code-review style checklist** of every problem, risk, or PRD-gap I could identify in **`JcefMarkdownRenderer`** (and the closely-coupled pieces that influence it).
The items are grouped so you can turn them straight into tickets or TODOs.

---

## 1  Lifecycle & Resource-management issues

| #       | Problem                                                                                                                                                                                                                      | Why it matters / symptom                                       |
| ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| **1.1** | `JBCefJSQuery jsQuery = JBCefJSQuery.create(browser)` is never used (only disposed).                                                                                                                                         | Wasted native CEF resources; confusing dead code.              |
| **1.2** | Theme-change listener registered in ctor is **never removed** in `dispose()`.                                                                                                                                                | Memory leak + still fires after renderer is gone.              |
| **1.3** | Several `Timer()` instances (`initializationTimer`, ad-hoc timers in `scrollToBottom`) are **non-daemon** and sometimes never cancelled.                                                                                     | Prevents JVM shutdown in tests, leaks threads, increasing CPU. |
| **1.4** | `dispose()` can run off the EDT (there is no `invokeAndWait`). Swing, JCEF and message-bus calls are legally only allowed on the UI thread.                                                                                  | Undefined behaviour / occasional crashes.                      |
| **1.5** | `devToolsOpened` flag only prevents a *double‐close* when you opened DevTools exactly once. A second call to `showDevTools()` replaces the old instance and the `dispose()` logic will try to close the *first* one and NPE. |                                                                |
| **1.6** | `pendingContent` is a plain var; `loadCompleted` is atomic. The combination still races: browser may finish loading while the fallback timer thread is still running, causing *double* `updateContent()` from two threads.   |                                                                |

---

## 2  Threading / EDT-violations

| #       | Problem                                                                                                                                                                                                                                                                  | Details                                    |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------ |
| **2.1** | Heavy work (`contentProcessor.processMarkdown()`) is performed on whichever thread called `setMarkdown()` – often the EDT from `MarkdownDialog.updateProgress`.                                                                                                          | Freezes UI on large outputs.               |
| **2.2** | Plain `java.util.Timer` tasks touch Swing (and CEF) APIs without `SwingUtilities.invokeLater`. Some places wrap with `invokeLater`, others do not (e.g. the first scroll inside `scrollToBottom`).                                                                       | Random “must be called on EDT” exceptions. |
| **2.3** | `ApplicationManager.getApplication().invokeLater` in `executeJavaScript()` is *nested* in callers that already used `SwingUtilities.invokeLater`. That double hop can reorder JS injections and defeats the guarantee that *all* updates happen after the page is ready. |                                            |

---

## 3  JavaScript / HTML template defects

| #       | Problem                                                                                                                                                                                                                     | Consequence                     |
| ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- |
| **3.1** | The template defines `let originalUpdateContent;` and immediately overrides `window.updateContent`, but **there is no original function yet**. `originalUpdateContent` stays `undefined`.                                   | Dead code / future regressions. |
| **3.2** | Panel-ID algorithm uses `panel.innerHTML` in the hash → any tiny content change breaks the key, so expansion state is lost on every refresh.                                                                                |                                 |
| **3.3** | `escapeJsString()` only escapes \ , \`  and \$ – it misses **newlines, quotes and the sequence `</script>`**. A crafted markdown file can break out of the template literal and run arbitrary JS.                           |                                 |
| **3.4** | No CSS is shipped for `.collapsible-panel`, `.updating-content`, etc. → the feature relies on the content processor injecting its own CSS; if not, the JS manipulates non-existent classes.                                 |                                 |
| **3.5** | `isScrolledToBottom()` uses `window.scrollY + innerHeight >= docHeight - 100`, but **`updateContent()` resets scroll position before it is called**; the “smart scrolling” PRD requirement is therefore only partially met. |                                 |
| **3.6** | Template is shoved through a **Base64 data URL**. On Windows the hard limit in CEF is \~2 MiB; long inline scripts + fonts can break loading silently.                                                                      |                                 |

---

## 4  Performance concerns

| #       | Problem                                                                                                                                                       |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **4.1** | Whole-document `innerHTML` replacement on every update – even small diffs – invalidates the DOM, syntax highlighting, scroll anchors and forces re-layout.    |
| **4.2** | Multiple new `Timer()` objects per `scrollToBottom()` call (the method can be triggered many times per second during streaming output).                       |
| **4.3** | Theme change triggers a *full markdown re-parse* (`updateContent(currentContent)`), instead of toggling a dark / light CSS class.                             |
| **4.4** | `escapeJsString()` creates a full copy of the HTML string; then Kotlin builds another huge `script` string; then CEF reparses it – memory spikes on big logs. |

---

## 5  Correctness / UX gaps vs. PRD

| #       | Gap                                                                                                                                                                                                                           | PRD reference |
| ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------- |
| **5.1** | **Smooth animations** for collapsible panels are specified, but the JS only toggles a class – no CSS transition is included.                                                                                                  |               |
| **5.2** | **Bottom-following logic** (“Auto-Scroll Detection”) does not honour the `isUpdatingContent` flag correctly: when live updates stream in very quickly the flag is often `true`, suppressing the auto-scroll the user expects. |               |
| **5.3** | **Fallback rendering** to `JEditorPane` is entirely missing from this class (contradicting “gracefully degrades when JCEF unavailable”).                                                                                      |               |
| **5.4** | **DevTools support** is gated by a boolean in settings, but the PRD asks for *on-demand* debugging; the “Show DevTools” button is hidden when the setting is off – there is no runtime override.                              |               |
| **5.5** | No keyboard shortcuts or focus handling for collapsible headers (PRD: “Keyboard Navigation”).                                                                                                                                 |               |
| **5.6** | No sanitation / stripping of HTML when Markdown originates from un-trusted process output (security & PRD “proper formatting”).                                                                                               |               |

---

## 6  Minor / style / maintainability

* Redundant `//` commented code in constructor (`backgroundColor`) clutters file.
* `currentContent` is updated **before** `contentProcessor.processMarkdown()` – if that call throws an exception the renderer now reports the *wrong* state.
* Several println-based logs instead of IntelliJ `Logger` (makes it impossible to filter in IDE).
* Kotlin `ApplicationManager.getApplication().invokeLater` is referenced without a **disposable** – leaks runnables if the editor is closed before it fires.
* `isDisposed` is a plain var, **not** `AtomicBoolean`, but accessed from non-UI threads (timers).
* `supportsDevTools()` simply returns `true`; if CEF is disabled by the platform, `openDevtools()` throws, caught but still prints stack trace every time.

---

### Quick wins

1. Replace all `java.util.Timer` usages with `com.intellij.util.concurrency.AppExecutorUtil.getAppScheduledExecutorService()` **or** `Timer("name", /*daemon=*/true)`, and always keep a field to cancel.
2. Move `contentProcessor.processMarkdown()` into a **background coroutine** and `SwingUtilities.invokeLater { updateContent(html) }` when done.
3. Attach all listeners (`themeManager`, load handler) to a `Disposer.newDisposable()` tied to `browser` – cleans them up automatically.
4. Use a small client-side script with `postMessage` instead of stuffing full HTML through `executeJavaScript`, avoiding huge strings and escaping issues.
5. Harden `escapeJsString()` with a full JSON/string encode or, better, pass HTML via `JBCefJSQuery`.

---

Addressing the items above will make the renderer safer, faster, and fully aligned with the product requirements.
