[Coding Aider Plan]

# Add Summarized Output Setting to Aider

See checklist: [summarized_output_setting_checklist.md](summarized_output_setting_checklist.md)

## Overview

Add a new setting to enable a summarized output feature in Aider responses. When enabled, Aider will include a
structured, parseable summary of its changes in addition to its regular output.

## Problem Description

Currently, Aider provides detailed output of its changes, but there's no easy way to programmatically parse and process
the results. Adding a structured summary would make it easier to:

- Quickly understand the key changes made
- Programmatically process Aider's output
- Integrate Aider's results with other tools

## Goals

1. Add a new setting to enable/disable summarized output
2. Implement XML-tagged summary blocks in Aider's output
3. Ensure the summary includes:
    - List of modified files
    - Type of changes made (add/modify/delete)
    - Brief description of changes
    - Status (success/failure)

## Additional Notes and Constraints

- Summary must be in a parseable XML format
- Summary should be concise but informative
- Regular Aider output should remain unchanged
- Setting should be configurable in both UI and command line
- XML tags should be unique to avoid conflicts

## XML Structure

The output will use the following XML format:

```xml
<aider-intention>
    Description of planned changes
</aider-intention>

[The actual changes are shown here]

<aider-summary>
    Summary of completed changes
</aider-summary>
```

The format includes:
- An intention block describing planned changes
- The actual changes in the middle
- A summary block with the completed changes

## References

- [AiderSettings.kt](../src/main/kotlin/de/andrena/codingaider/settings/AiderSettings.kt)
- [AiderExecutionStrategy.kt](../src/main/kotlin/de/andrena/codingaider/executors/AiderExecutionStrategy.kt)
