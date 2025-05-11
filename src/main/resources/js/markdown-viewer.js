/**
 * JavaScript functionality for the Markdown Viewer component
 */

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

// Prevent hover effects during content updates
let isUpdatingContent = false;
const originalUpdateContent = updateContent;
updateContent = function(html) {
    isUpdatingContent = true;
    
    // Add a class to the body during updates to disable hover effects
    document.body.classList.add('updating-content');
    
    // Call the original function
    originalUpdateContent(html);
    
    // Remove the class after the update is complete
    setTimeout(() => {
        document.body.classList.remove('updating-content');
        isUpdatingContent = false;
    }, 200);
};
