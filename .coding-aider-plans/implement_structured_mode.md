# Implementing Configurable Structured Mode for Aider

## Feature Description
Implement a configurable "Structured Mode" for Aider, which can be set in the settings and in the AiderInputDialog. This mode changes how Aider processes requests, focusing on creating detailed descriptions of changes rather than directly modifying code. Additionally, ensure that existing actions use the settings for structuredMode.

## Needed Changes

### 1. Update AiderSettings
- [ ] Add a new boolean field `useStructuredMode` to the `AiderSettings` class.
- [ ] Update the `AiderSettingsConfigurable` to include a checkbox for enabling/disabling Structured Mode.
- [ ] Implement logic to save/load Structured Mode setting.

### 2. Modify AiderInputDialog
- [ ] Add a checkbox in the dialog to toggle Structured Mode for the current request.
- [ ] Ensure the checkbox is initialized with the value from AiderSettings.
- [ ] Update the `getInputText()` method to include information about whether Structured Mode is enabled.

### 3. Update CommandData
- [ ] Add a new boolean field `structuredMode` to the `CommandData` class.
- [ ] Modify the `collectCommandData()` function in `AiderAction` to include the Structured Mode setting.

### 4. Adjust AiderExecutionStrategy
- [ ] Update the `buildCommonArgs()` function to include logic for Structured Mode.
- [ ] If Structured Mode is enabled, modify the command to instruct Aider to create a description file instead of making direct changes.

### 5. Update Existing Actions
- [ ] Modify `CommitAction` to use the structuredMode setting.
- [ ] Update `RefactorToCleanCodeAction` to respect the structuredMode setting.
- [ ] Adjust `DocumentCodeAction` to use structuredMode when generating documentation.
- [ ] Ensure `AiderWebCrawlAction` considers structuredMode in its execution.

## Implementation Details

### AiderSettings (src\main\kotlin\de\andrena\codingaider\settings\AiderSettings.kt)
1. Add `useStructuredMode: Boolean = false` to the data class.
2. Update `copy()` method to include `useStructuredMode`.
3. Modify `toProperties()` and `fromProperties()` to handle the new field.

### AiderSettingsConfigurable (src\main\kotlin\de\andrena\codingaider\settings\AiderSettingsConfigurable.kt)
1. Add a JCheckBox for Structured Mode in the UI.
2. Update `isModified()` to check for changes in the Structured Mode setting.
3. Modify `apply()` to save the Structured Mode setting.
4. Update `reset()` to load the Structured Mode setting.

### AiderInputDialog (src\main\kotlin\de\andrena\codingaider\inputdialog\AiderInputDialog.kt)
1. Add a JCheckBox for Structured Mode in the dialog.
2. Initialize the checkbox with `settings.useStructuredMode`.
3. Update `getInputText()` to include Structured Mode information.
4. Modify `collectCommandData()` to set the `structuredMode` field in `CommandData`.

### CommandData (src\main\kotlin\de\andrena\codingaider\command\CommandData.kt)
1. Add `structuredMode: Boolean` to the data class.

### AiderExecutionStrategy (src\main\kotlin\de\andrena\codingaider\executors\AiderExecutionStrategy.kt)
1. Update `buildCommonArgs()` to handle Structured Mode:
   ```kotlin
   if (commandData.structuredMode) {
       args.add("--structured-mode")
       args.add(".coding-aider-plans")
   }
   ```

### Updating Existing Actions
1. CommitAction (src\main\kotlin\de\andrena\codingaider\actions\aider\CommitAction.kt):
   - Update `actionPerformed()` to pass `structuredMode` to `CommandData`.

2. RefactorToCleanCodeAction (src\main\kotlin\de\andrena\codingaider\actions\aider\RefactorToCleanCodeAction.kt):
   - Modify `refactorToCleanCode()` to include `structuredMode` in `CommandData`.

3. DocumentCodeAction (src\main\kotlin\de\andrena\codingaider\actions\aider\DocumentCodeAction.kt):
   - Update `documentCode()` to set `structuredMode` in `CommandData`.

4. AiderWebCrawlAction (src\main\kotlin\de\andrena\codingaider\actions\aider\AiderWebCrawlAction.kt):
   - Modify `actionPerformed()` to include `structuredMode` in `CommandData`.

## Testing Plan
1. Verify that the Structured Mode setting can be toggled in the AiderSettings.
2. Ensure the AiderInputDialog correctly displays and allows toggling of Structured Mode.
3. Test that when Structured Mode is enabled, Aider creates description files in the `.coding-aider-plans` directory instead of making direct code changes.
4. Confirm that all existing actions (Commit, Refactor, Document, WebCrawl) respect the Structured Mode setting.
5. Validate that Structured Mode can be overridden for individual requests in the AiderInputDialog.

## Future Considerations
- Implement a mechanism to apply the changes described in the structured mode files.
- Add a feature to easily switch between viewing the original code and the structured description.
- Consider adding a "diff view" to compare the original code with the proposed changes in structured mode.
