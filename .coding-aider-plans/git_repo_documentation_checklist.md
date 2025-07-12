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
- [x] proper tag and branch selection (fetch existing branches and tags from the repository)
- [x] allow changing tag/branch after cloning (will checkout and update filetree accordingly)
- [ ] optional after clone documentation generation
UI/UX Improvements:
- [x] Add file selection checkboxes to tree component
- [x] Implement "Select All" / "Deselect All" functionality
- [x] Add file type filtering options
- [x] Show repository information (commit hash, last update) in UI
- [x] Add validation for Git repository URL format
- [x] Implement branch switching after initial clone
- [x] Add repository size estimation before cloning
- [ ] Scrolling in file selection tree in AiderWebCrawlAction$WebCrawlAndGitDialog

Git Operations Enhancements:
- [x] Support for private repositories (authentication)
- [x] Implement shallow cloning for large repositories
- [x] Add support for specific commit hash checkout
- [x] Add support for tag based checkout
- [x] Add support for branch based checkout
- [ ] (later) Handle Git LFS files properly
- [ ] (later) Add retry mechanism for failed clone operations
- [ ] (later) Support for Git submodules

Documentation Generation:
- [ ] Add preview functionality for selected files
- [ ] Implement file content filtering (exclude binary files)
- [ ] Add support for custom ignore patterns (.gitignore-like)
- [ ] Optimize memory usage for large file selections
- [ ] Add progress tracking for documentation generation

Error Handling & Validation:
- [ ] Validate repository accessibility before cloning
- [ ] Handle network connectivity issues gracefully
- [ ] Add proper error messages for Git authentication failures
- [ ] Implement timeout handling for long-running operations
- [ ] Add validation for branch/tag existence
