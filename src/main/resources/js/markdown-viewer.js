/**
 * Markdown Viewer JavaScript
 * Handles content updates, collapsible panels, and smart scroll management
 */

// Track content updates and panel expansion
let isUpdatingContent = false;
const panelStates = {};

// Initialize when document is ready or immediately if already loaded
function initMarkdownViewer() {
    console.log('Markdown viewer JavaScript loaded');
    initCollapsiblePanels();
    initCopyPasteSupport();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initMarkdownViewer);
} else {
    initMarkdownViewer();
}

/**
 * Initialize copy/paste support for the markdown viewer
 */
function initCopyPasteSupport() {
    // Allow text selection and copy operations
    document.addEventListener('keydown', function(event) {
        // Allow Ctrl+C, Ctrl+A, Ctrl+V, and other standard shortcuts
        if (event.ctrlKey || event.metaKey) {
            // Don't prevent default for copy/paste/select operations
            if (event.key === 'c' || event.key === 'a' || event.key === 'v' || 
                event.key === 'x' || event.key === 'z' || event.key === 'y') {
                // Allow these standard shortcuts to work normally
                return true;
            }
        }
    }, false);
    
    // Ensure text selection works properly
    document.addEventListener('selectstart', function(event) {
        // Don't prevent text selection unless we're updating content
        if (isUpdatingContent) {
            event.preventDefault();
        }
    }, false);
}


/**
 * Generate a stable ID for each panel based on its header content
 * This is more reliable than using innerHTML which changes with every update
 */
function getPanelId(panel) {
    // Use panel's data attribute if available
    if (panel.dataset.panelId) {
        return panel.dataset.panelId;
    }
    
    const header = panel.querySelector('.collapsible-header');
    let title = '';
    if (header) {
        const titleElement = header.querySelector('.collapsible-title');
        if (titleElement) {
            title = titleElement.textContent || '';
        }
    }
    
    // Generate a more stable ID based on title and position in document
    const allPanels = Array.from(document.querySelectorAll('.collapsible-panel'));
    const panelIndex = allPanels.indexOf(panel);
    
    // Create a hash from the title for more stability
    let hash = 0;
    for (let i = 0; i < title.length; i++) {
        hash = ((hash << 5) - hash) + title.charCodeAt(i);
        hash |= 0; // Convert to 32bit integer
    }
    
    const stableId = 'panel_' + Math.abs(hash).toString(36) + '_' + panelIndex;
    
    // Store the ID as a data attribute for future reference
    panel.dataset.panelId = stableId;
    return stableId;
}

/**
 * Initialize all collapsible panels in the document
 */
function initCollapsiblePanels() {
    document.querySelectorAll('.collapsible-panel').forEach(panel => {
        const header = panel.querySelector('.collapsible-header');
        const panelId = getPanelId(panel);
        
        if (header && !header.hasAttribute('data-initialized')) {
            // Add keyboard accessibility
            header.setAttribute('tabindex', '0');
            header.setAttribute('role', 'button');
            header.setAttribute('aria-expanded', panel.classList.contains('expanded') ? 'true' : 'false');
            
            // Remove existing event listeners to prevent duplicates
            header.removeEventListener('click', handlePanelClick);
            header.addEventListener('click', handlePanelClick);
            
            // Add keyboard support
            header.removeEventListener('keydown', handlePanelKeydown);
            header.addEventListener('keydown', handlePanelKeydown);
            
            // Mark as initialized
            header.setAttribute('data-initialized', 'true');
            
            // Restore panel state if it exists
            if (panelStates[panelId] === false) {
                panel.classList.remove('expanded');
                const arrow = header.querySelector('.collapsible-arrow');
                if (arrow) {
                    arrow.textContent = '►';
                }
                header.setAttribute('aria-expanded', 'false');
            } else if (panelStates[panelId] === true) {
                panel.classList.add('expanded');
                const arrow = header.querySelector('.collapsible-arrow');
                if (arrow) {
                    arrow.textContent = '▼';
                }
                header.setAttribute('aria-expanded', 'true');
            }
        }
    });
}

/**
 * Handle click events on panel headers
 */
function handlePanelClick(event) {
    togglePanel(this.parentElement);
    event.stopPropagation();
}

/**
 * Handle keyboard events on panel headers
 */
function handlePanelKeydown(event) {
    // Only handle Enter and Space when the header is focused
    // Don't interfere with copy/paste or other shortcuts
    if ((event.key === 'Enter' || event.key === ' ') && event.target === this) {
        event.preventDefault();
        event.stopPropagation();
        togglePanel(this.parentElement);
    }
}

/**
 * Toggle a panel's expanded state
 */
function togglePanel(panel) {
    const panelId = getPanelId(panel);
    const header = panel.querySelector('.collapsible-header');
    const isExpanded = panel.classList.toggle('expanded');
    
    // Update arrow indicator
    const arrow = panel.querySelector('.collapsible-arrow');
    if (arrow) {
        arrow.textContent = isExpanded ? '▼' : '►';
    }
    
    // Update accessibility attributes
    if (header) {
        header.setAttribute('aria-expanded', isExpanded ? 'true' : 'false');
    }
    
    // Store panel state
    panelStates[panelId] = isExpanded;
}

/**
 * Update content while preserving panel states
 */
function updateContent(html) {
    try {
        console.log('Updating content');
        isUpdatingContent = true;
        
        // Store current panel states before updating
        storeCurrentPanelStates();
        
        // Add updating class to prevent hover effects during update
        document.body.classList.add('updating-content');
        
        // Update content
        const contentElement = document.getElementById('content');
        if (contentElement) {
            contentElement.innerHTML = html;
        } else {
            console.error('Content element not found');
            return;
        }
        
        // Initialize panels with restored states
        setTimeout(() => {
            initCollapsiblePanels();
            restorePanelStates();
            
            // Remove updating class
            document.body.classList.remove('updating-content');
            isUpdatingContent = false;
        }, 100);
        
    } catch (e) {
        console.error("Error updating content:", e);
        
        // Basic fallback
        try {
            const contentElement = document.getElementById('content');
            if (contentElement) {
                contentElement.innerHTML = html;
            }
        } catch (innerError) {
            console.error("Fallback update failed:", innerError);
        }
        
        document.body.classList.remove('updating-content');
        isUpdatingContent = false;
    }
}

/**
 * Restore panel states after content update
 */
function restorePanelStates() {
    document.querySelectorAll('.collapsible-panel').forEach(panel => {
        const panelId = getPanelId(panel);
        
        // Apply stored state if it exists
        if (panelId in panelStates) {
            const shouldBeExpanded = panelStates[panelId];
            const isCurrentlyExpanded = panel.classList.contains('expanded');
            
            if (shouldBeExpanded && !isCurrentlyExpanded) {
                panel.classList.add('expanded');
                const arrow = panel.querySelector('.collapsible-arrow');
                const header = panel.querySelector('.collapsible-header');
                
                if (arrow) {
                    arrow.textContent = '▼';
                }
                
                if (header) {
                    header.setAttribute('aria-expanded', 'true');
                }
            } else if (!shouldBeExpanded && isCurrentlyExpanded) {
                panel.classList.remove('expanded');
                const arrow = panel.querySelector('.collapsible-arrow');
                const header = panel.querySelector('.collapsible-header');
                
                if (arrow) {
                    arrow.textContent = '►';
                }
                
                if (header) {
                    header.setAttribute('aria-expanded', 'false');
                }
            }
        }
    });
}


/**
 * Store current panel states
 */
function storeCurrentPanelStates() {
    document.querySelectorAll('.collapsible-panel').forEach(panel => {
        const panelId = getPanelId(panel);
        panelStates[panelId] = panel.classList.contains('expanded');
    });
}
