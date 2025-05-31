/**
 * Markdown Viewer JavaScript
 * Handles content updates, collapsible panels, and smart scroll management
 */

// Track scroll state and panel expansion
let isUserScrolled = false;
let isUpdatingContent = false;
let shouldAutoScroll = true; // Flag to control auto-scrolling behavior
const panelStates = {};

// Initialize when document is ready
document.addEventListener('DOMContentLoaded', function() {
    // Set up scroll tracking
    window.addEventListener('scroll', function() {
        if (!isUpdatingContent) {
            const wasAtBottom = isScrolledToBottom();
            isUserScrolled = !wasAtBottom;
            
            // If user scrolls to bottom manually, enable auto-scroll again
            if (wasAtBottom && isUserScrolled) {
                shouldAutoScroll = true;
            }
        }
    });
    
    // Initialize any panels that might be in the initial content
    initCollapsiblePanels();
});

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
    if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
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
 * Update content while preserving panel states and implementing smart scroll
 */
function updateContent(html) {
    try {
        isUpdatingContent = true;
        
        // Save current scroll position and bottom state
        const scrollPosition = window.scrollY;
        const wasAtBottom = isScrolledToBottom();
        
        // Store current panel states before updating
        storeCurrentPanelStates();
        
        // Add updating class to prevent hover effects during update
        document.body.classList.add('updating-content');
        
        // Update content
        document.getElementById('content').innerHTML = html;
        
        // Initialize panels with restored states
        initCollapsiblePanels();
        restorePanelStates();
        
        // Schedule additional restoration attempts to handle race conditions
        setTimeout(restorePanelStates, 50);
        
        // Smart scroll logic with a slight delay to allow rendering
        setTimeout(() => {
            if (shouldAutoScroll && (wasAtBottom || !isUserScrolled)) {
                // Auto-scroll to bottom if:
                // 1. Auto-scroll is enabled AND
                // 2. User was at bottom OR user hasn't manually scrolled
                scrollToBottomSmooth();
            } else {
                // Restore previous scroll position
                window.scrollTo(0, scrollPosition);
            }
            
            // Remove updating class
            document.body.classList.remove('updating-content');
            isUpdatingContent = false;
        }, 50);
    } catch (e) {
        console.error("Error updating content:", e);
        
        // Basic fallback
        try {
            document.getElementById('content').innerHTML = html;
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
 * Check if the view is scrolled to the bottom
 */
function isScrolledToBottom() {
    const scrollY = window.scrollY || window.pageYOffset;
    const windowHeight = window.innerHeight;
    const documentHeight = Math.max(
        document.body.scrollHeight,
        document.body.offsetHeight,
        document.documentElement.clientHeight,
        document.documentElement.scrollHeight,
        document.documentElement.offsetHeight
    );
    
    // Consider "bottom" if within 100px of the actual bottom
    return scrollY + windowHeight >= documentHeight - 100;
}

/**
 * Scroll to the bottom of the content (smooth)
 */
function scrollToBottomSmooth() {
    window.scrollTo({
        top: document.body.scrollHeight,
        behavior: 'smooth'
    });
    isUserScrolled = false;
}

/**
 * Scroll to the bottom of the content (instant)
 */
function scrollToBottom() {
    window.scrollTo(0, document.body.scrollHeight);
    isUserScrolled = false;
}

/**
 * Enable or disable auto-scrolling
 */
function setAutoScroll(enabled) {
    shouldAutoScroll = enabled;
}

/**
 * Force scroll to bottom (used for programmatic scrolling)
 */
function forceScrollToBottom() {
    shouldAutoScroll = true;
    isUserScrolled = false;
    scrollToBottomSmooth();
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
