[Coding Aider Plan - Checklist]

Core Implementation:
- [x] Create GitRepoDocumentationDialog class
- [x] Add tabbed interface to switch between web crawl and Git clone modes
- [x] Implement Git repository URL input field
- [x] Add branch/tag selection dropdown
- [x] Create file/folder selection tree component
- [x] Integrate DocumentationGenerationDialog UI elements
- [x] Implement temporary repository cloning functionality
- [x] Add cleanup mechanism for temporary repositories
- [x] Create GitRepoCloneService for handling Git operations
- [x] Update AiderWebCrawlAction to use new dialog
- [x] Add error handling for Git operations
- [x] Implement progress indicators for long-running operations
- [x] Proper tag and branch selection (fetch existing branches and tags from the repository)
- [x] Allow changing tag/branch after cloning (will checkout and update filetree accordingly)

Documentation Generation:
- [x] Fix documentation generation after cloning
- [x] Validate documentation settings before generation
- [x] Add progress indicator during documentation generation
- [x] Ensure proper state handling in documentation dialog
- [x] Add error handling for documentation generation
- [x] Update UI feedback during documentation process
- [x] Fix dialog state after OK button press
- [x] Add validation for required documentation fields
- [x] Implement proper cleanup after documentation generation
- [x] Add success/failure notifications for documentation

UI/UX Improvements:
- [x] Add file selection checkboxes to tree component
- [x] Implement "Select All" / "Deselect All" functionality
- [x] Add file type filtering options
- [x] Show repository information (commit hash, last update) in UI
- [x] Add validation for Git repository URL format
- [x] Implement branch switching after initial clone
- [x] Add repository size estimation before cloning
- [x] Scrolling in file selection tree
- [x] Improve documentation section UI state management
- [x] Add loading indicators for documentation generation
- [x] Improve error message display
- [ ] Add documentation preview option

Git Operations Enhancements:
- [x] Support for private repositories (authentication)
- [x] Implement shallow cloning for large repositories
- [x] Add support for specific commit hash checkout
- [x] Add support for tag based checkout
- [x] Add support for branch based checkout
- [ ] (later) Handle Git LFS files properly
- [ ] (later) Add retry mechanism for failed clone operations
- [ ] (later) Support for Git submodules
