---
description: "Create a release of the IntelliJ plugin using GitHub Actions with AI-generated release notes"
argument-hint: "[release_version - may be left empty] [next_version - may be left empty]"
allowed-tools: [Bash, Read, Glob, Write]
---

# Create Plugin Release

Create a release of the IntelliJ plugin using the existing release workflow github action. This process will:
1. **Check git status and commit and push changes** (if any)
2. **Analyze recent changes** since the last release and update releasenotes.md and commit it
3. **Trigger the GitHub Actions release workflow**
4. **Update the release with generated notes**

## Release Configuration

**Release Version**: `$1` (leave empty for current version)
**Next Version**: `$2` (leave empty for auto-incremented patch version)

## Helpful commands

```bash
# Check current version
echo "Current version:"
cat version.txt 2>/dev/null || echo "No version.txt found"

# Check git status
echo "Git status:"
git status

# Find last release tag


Now let me examine the codebase to understand what has changed and generate meaningful release notes:

```bash
# Find changed files
echo "Changed files since last release:"
git diff --name-only $LATEST_TAG..HEAD | sort | uniq
```


```bash
# Determine version
CURRENT_VERSION=$(cat version.txt | sed 's/-SNAPSHOT//' 2>/dev/null || echo "1.0.0")
RELEASE_VERSION="${1:-$CURRENT_VERSION}"
echo "Release version: $RELEASE_VERSION"

# Trigger GitHub Actions workflow
echo "Triggering release workflow..."
gh workflow run manual-release.yml \
  -f release_version="$RELEASE_VERSION" \
  ${2:+-f next_version="$2"}

echo "✅ Release workflow triggered!"
echo "📋 Check the progress at: $(gh repo view --json url -q '.url')/actions"
```

The release workflow is now running. Once it completes, I'll help you update the release with comprehensive AI-generated notes. You can monitor the progress in the GitHub Actions tab.

## What happens next:

1. **GitHub Actions** will build and create the release
2. **Release assets** will be automatically uploaded
3. **Version** will be updated for the next development cycle
4. **Release notes** will be generated based on the actual changes

You can check the release status with:
```bash
gh release list --limit 5
```