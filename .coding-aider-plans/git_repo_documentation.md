[Coding Aider Plan]

## Overview
Extend the existing webcrawl action to support Git repository documentation generation by adding a Git clone mode. This enhancement will allow users to clone Git repositories, select specific branches/tags, and generate documentation using the existing documentation generation features.

## Problem Description
Currently, the webcrawl action only supports crawling web pages. There's a need to extend this functionality to handle Git repositories, allowing users to:
- Clone repositories
- Select specific branches/tags
- Browse and select files/folders
- Generate documentation using existing document types
- Maintain the simple web crawl functionality
- Ensure seamless documentation generation after repository cloning

## Goals
1. Add Git repository support to the webcrawl action
2. Create a tabbed interface to switch between web crawl and Git clone modes
3. Integrate branch/tag selection functionality
4. Implement file/folder selection using IntelliJ's standard UI
5. Reuse existing documentation generation components
6. Minimize code duplication
7. Ensure reliable documentation generation after cloning
8. Provide clear feedback during the documentation process

## Additional Notes and Constraints
- Maintain backward compatibility with existing web crawl functionality
- Reuse DocumentationGenerationDialog components where possible
- Follow IntelliJ UI guidelines for consistency
- Handle Git operations asynchronously to prevent UI freezing
- Clean up temporary cloned repositories after documentation generation
- Consider memory usage when cloning large repositories
- Validate documentation settings before generation
- Provide clear error messages and status updates
- Handle state transitions between cloning and documentation generation

## References
- [DocumentationGenerationDialog](../src/main/kotlin/de/andrena/codingaider/features/documentation/dialogs/DocumentationGenerationDialog.kt)
- [AiderWebCrawlAction](../src/main/kotlin/de/andrena/codingaider/actions/aider/AiderWebCrawlAction.kt)
- [DocumentTypeConfiguration](../src/main/kotlin/de/andrena/codingaider/features/documentation/DocumentTypeConfiguration.kt)
