package de.andrena.codingaider.outputview.markdown

import com.intellij.ui.JBColor

class MarkdownThemeManager {
    var isDarkTheme: Boolean = !JBColor.isBright()
        private set
        
    /**
     * Loads JavaScript from resources
     */
    private fun loadJavaScriptResource(): String {
        return try {
            val resourcePath = "/js/markdown-viewer.js"
            val inputStream = javaClass.getResourceAsStream(resourcePath)
            inputStream?.bufferedReader()?.use { it.readText() } ?: getDefaultJavaScript()
        } catch (e: Exception) {
            // Fallback to default JavaScript if resource loading fails
            println("Error loading JavaScript resource: ${e.message}")
            getDefaultJavaScript()
        }
    }
    
    /**
     * Provides default JavaScript if resource loading fails
     */
    private fun getDefaultJavaScript(): String {
        return """
            // Persistent panel state storage
            const panelStates = {};
            
            // Generate a stable ID for each panel based on its content
            function getPanelId(panel) {
                const header = panel.querySelector('.collapsible-header');
                let title = '';
                if (header) {
                    const titleElement = header.querySelector('.collapsible-title');
                    if (titleElement) {
                        title = titleElement.textContent || '';
                    }
                }
                
                // Use panel's data attribute if available
                if (panel.dataset.panelId) {
                    return panel.dataset.panelId;
                }
                
                // Generate a more stable ID based on title and position in document
                const allPanels = Array.from(document.querySelectorAll('.collapsible-panel'));
                const panelIndex = allPanels.indexOf(panel);
                const stableId = title.replace(/[^a-z0-9]/gi, '') + '-' + panelIndex;
                
                // Store the ID as a data attribute for future reference
                panel.dataset.panelId = stableId;
                return stableId;
            }
            
            // Function to initialize collapsible panels
            function initCollapsiblePanels() {
                document.querySelectorAll('.collapsible-panel').forEach(panel => {
                    const header = panel.querySelector('.collapsible-header');
                    const panelId = getPanelId(panel);
                    
                    // Remove existing event listeners to prevent duplicates
                    if (header) {
                        header.removeEventListener('click', togglePanel);
                        header.addEventListener('click', togglePanel);
                        
                        // Restore panel state if it exists
                        if (panelStates[panelId] === false) {
                            panel.classList.remove('expanded');
                            const arrow = header.querySelector('.collapsible-arrow');
                            if (arrow) {
                                arrow.textContent = '▶';
                            }
                        }
                    }
                });
            }
            
            // Toggle panel function
            function togglePanel(event) {
                const panel = this.parentElement;
                const panelId = getPanelId(panel);
                const isExpanded = panel.classList.toggle('expanded');
                
                // Update arrow indicator
                const arrow = this.querySelector('.collapsible-arrow');
                if (arrow) {
                    arrow.textContent = isExpanded ? '▼' : '▶';
                }
                
                // Store panel state
                panelStates[panelId] = isExpanded;
                
                // Prevent event propagation
                event.stopPropagation();
            }
            
            // Function to update content while preserving panel states
            function updateContent(html) {
                // Save current scroll position
                const scrollPosition = window.scrollY;
                const wasAtBottom = isScrolledToBottom();
                
                // Store current panel states before updating
                storeCurrentPanelStates();
                
                // Update content
                document.getElementById('content').innerHTML = html;
                
                // Initialize panels with restored states - use a more reliable approach
                // with multiple attempts to ensure states are properly restored
                restorePanelStates();
                
                // Schedule additional restoration attempts to handle race conditions
                setTimeout(restorePanelStates, 50);
                setTimeout(restorePanelStates, 150);
                
                // Restore scroll position
                setTimeout(() => {
                    if (wasAtBottom) {
                        scrollToBottom();
                        setTimeout(scrollToBottom, 100);
                    } else {
                        window.scrollTo(0, scrollPosition);
                    }
                }, 50);
            }
            
            // More reliable panel state restoration
            function restorePanelStates() {
                document.querySelectorAll('.collapsible-panel').forEach(panel => {
                    const panelId = getPanelId(panel);
                    
                    // Apply stored state if it exists
                    if (panelStates[panelId] === false) {
                        panel.classList.remove('expanded');
                        const arrow = panel.querySelector('.collapsible-arrow');
                        if (arrow) {
                            arrow.textContent = '▶';
                        }
                    } else if (panelStates[panelId] === true) {
                        panel.classList.add('expanded');
                        const arrow = panel.querySelector('.collapsible-arrow');
                        if (arrow) {
                            arrow.textContent = '▼';
                        }
                    }
                    
                    // Ensure event listeners are attached
                    const header = panel.querySelector('.collapsible-header');
                    if (header) {
                        header.removeEventListener('click', togglePanel);
                        header.addEventListener('click', togglePanel);
                    }
                });
            }
            
            // Check if scrolled to bottom
            function isScrolledToBottom() {
                // More generous threshold (100px) to determine if we're at the bottom
                return (window.innerHeight + window.scrollY) >= (document.body.offsetHeight - 100);
            }
            
            // Scroll to bottom
            function scrollToBottom() {
                // Force scroll to absolute bottom
                window.scrollTo({
                    top: document.body.scrollHeight,
                    behavior: 'auto'
                });
            }
            
            // Store current panel states
            function storeCurrentPanelStates() {
                document.querySelectorAll('.collapsible-panel').forEach(panel => {
                    const panelId = getPanelId(panel);
                    panelStates[panelId] = panel.classList.contains('expanded');
                });
            }
            
            // Initialize panels when page loads
            document.addEventListener('DOMContentLoaded', initCollapsiblePanels);
        """.trimIndent()
    }

    /**
     * Updates the current theme state
     * @param isDark True if dark theme should be used, false for light theme
     * @return True if the theme changed, false otherwise
     */
    fun updateTheme(isDark: Boolean): Boolean {
        val changed = isDarkTheme != isDark
        isDarkTheme = isDark
        return changed
    }

    /**
     * Creates the base HTML template with appropriate theme styling
     */
    fun createBaseHtml(): String {
        val jsContent = loadJavaScriptResource()
        
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
            <meta http-equiv="Pragma" content="no-cache">
            <meta http-equiv="Expires" content="0">
            <script>
                $jsContent
                
                // Add a direct content update function that doesn't rely on existing functions
                window.directUpdateContent = function(html) {
                    try {
                        document.getElementById('content').innerHTML = html;
                        // Initialize panels after direct update
                        if (typeof initCollapsiblePanels === 'function') {
                            initCollapsiblePanels();
                        }
                    } catch(e) {
                        console.error("Error in directUpdateContent:", e);
                    }
                };
                
                // Add a callback function for Java communication
                window.javaCallback = function(message) {
                    // This will be replaced by the Java-injected function
                    console.log("Java callback called with: " + message);
                };
                
                // Ensure the document is ready before initializing
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', initCollapsiblePanels);
                } else {
                    // Document already loaded, run immediately
                    initCollapsiblePanels();
                }
            </script>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                    line-height: 1.6;
                    margin: 0;
                    padding: 20px;
                    background: ${if (isDarkTheme) "#2b2b2b" else "#ffffff"};
                    color: ${if (isDarkTheme) "#ffffff" else "#000000"};
                }
                
                /* Custom scrollbar styling to match IDE native look */
                ::-webkit-scrollbar {
                    width: 12px;
                    height: 12px;
                }
                
                ::-webkit-scrollbar-track {
                    background: ${if (isDarkTheme) "#3c3f41" else "#f0f0f0"};
                    border-radius: 0;
                }
                
                ::-webkit-scrollbar-thumb {
                    background: ${if (isDarkTheme) "#5a5d5e" else "#c9c9c9"};
                    border-radius: 0;
                    border: 2px solid ${if (isDarkTheme) "#3c3f41" else "#f0f0f0"};
                }
                
                ::-webkit-scrollbar-thumb:hover {
                    background: ${if (isDarkTheme) "#6e7071" else "#a0a0a0"};
                }
                
                ::-webkit-scrollbar-corner {
                    background: ${if (isDarkTheme) "#3c3f41" else "#f0f0f0"};
                }
                #content {
                    max-width: 100%;
                    overflow-wrap: break-word;
                }
                pre {
                    white-space: pre-wrap;
                    overflow-x: auto;
                    background: ${if (isDarkTheme) "#1e1e1e" else "#f5f5f5"};
                    padding: 10px;
                    border-radius: 4px;
                }
                code {
                    font-family: 'JetBrains Mono', Consolas, Monaco, 'Courier New', monospace;
                }
            </style>
        </head>
        <body>
            <div id="content"><!-- Content will be loaded here --></div>
        </body>
        </html>
        """.trimIndent()
    }

    fun createHtmlWithContent(content: String): String {
        val placeholder = Regex(
            """<div\s+id\s*=\s*["']content["'][^>]*>.*?</div>""",
            RegexOption.DOT_MATCHES_ALL
        )
        return createBaseHtml().replaceFirst(placeholder, """<div id="content">$content</div>""")
    }
}
