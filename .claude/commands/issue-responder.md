---
description: "Analyze open issues against latest release changes and generate response files"
argument-hint: "[release_version - leave empty for latest release] [response_style - defaults to detailed]"
allowed-tools: [Bash, Read, Glob, Write, Task]
---

# Issue Responder - Generate Release Response Files

This command analyzes open GitHub issues against the latest release changes and generates markdown response files for issues that appear to be resolved by the release.

## Process Overview

1. **Check git status** and ensure working directory is clean
2. **Get latest release information** and changes
3. **Analyze open issues** to identify potentially resolved ones
4. **Generate response files** in `issue-responses/` directory

## Configuration

**Release Version**: `$1` (leave empty for latest release)

## Analysis Process

Let me start by checking the current state and analyzing the release:

```bash
# Check git status
echo "Checking git status..."
git status

# Get latest release if no version specified
if [ -z "$1" ]; then
  LATEST_RELEASE=$(gh release list --limit 1 --json tagName --jq '.[0].tagName')
  echo "Latest release: $LATEST_RELEASE"
else
  LATEST_RELEASE="$1"
  echo "Analyzing release: $LATEST_RELEASE"
fi

# Get release details
echo "Getting release details..."
gh release view "$LATEST_RELEASE" --json body,tagName,publishedAt
```

Now let me analyze the open issues and compare with release changes:

```bash
# Get open issues
echo "Fetching open issues..."
gh issue list --state open --limit 20 --json number,title,author,createdAt,body
```

## Analysis and Response Generation

Based on the release notes and open issues analysis, I'll now generate response files for issues that appear to be resolved. Let me examine the codebase structure and create comprehensive responses:

```bash
# Create issue-responses directory if it doesn't exist
mkdir -p issue-responses
echo "Created issue-responses directory"
```

Now I'll analyze the specific issues and generate appropriate response files. For each issue that appears to be resolved based on the release notes, I'll create a detailed response file.

The response files will include:
- Issue number and title
- Problem description
- Root cause analysis
- Solution details from the release
- Verification steps
- Technical implementation details
- Related changes

## Generated Response Files

Let me create the response files for identified issues:

```bash
# List generated response files
echo "Generated response files:"
ls -la issue-responses/
```

## Summary

This analysis has identified and generated response files for issues that appear to be resolved by the release changes. The response content depends on the [response_style]. Response file content depends on the used style:

### response_style==detailed

1. **Status**: Marked as RESOLVED with the specific version
2. **Root Cause**: Analysis of what caused the issue
3. **Solution**: Specific changes that fixed the issue
4. **Verification Steps**: How users can verify the fix
5. **Related Changes**: Other improvements in the same release

### response_style==informal

- Start with a personal greeting that acknowledges the user's specific issue
- Use conversational language instead of technical jargon
- Express empathy for the user's frustration
- Focus on the positive outcome ("Great news!" instead of "This has been resolved")
- Keep instructions simple and actionable
- End with appreciation for their contribution
- Maintain a helpful, supportive tone throughout

## Usage Instructions

The generated response files are ready to be posted manually to the respective GitHub issues. Each file is named in the format `issue-{number}-{brief-description}.md` for easy identification.

## Next Steps

1. **Review the generated response files** in the `issue-responses/` directory
2. **Post the responses** to the corresponding GitHub issues
3. **Close the issues** once the responses are posted and confirmed
4. **Update any documentation** if needed

## Customization

You can customize this command by:
- Modifying the issue analysis logic to focus on specific issue types
- Adding more detailed technical analysis based on your codebase
- Including automated issue closing via `gh issue close`
- Adding response templates for different types of issues