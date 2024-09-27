# Configurable Structured Mode for Aider

## Feature Description
Implement a configurable "Structured Mode" for Aider, which can be set in the settings and in the AiderInputDialog. This mode changes how Aider processes requests, focusing on creating detailed descriptions of changes rather than directly modifying code.

## Needed Changes

### 1. Update AiderSettings
- Add a new boolean field `useStructuredMode` to the `AiderSettings` class.
- Update the `AiderSettingsConfigurable` to include a checkbox for enabling/disabling Structured Mode.

### 2. Modify AiderInputDialog
- Add a checkbox in the dialog to toggle Structured Mode for the current request.
- Ensure the checkbox is initialized with the value from AiderSettings.
- Update the `getInputText()` method to include information about whether Structured Mode is enabled.

### 3. Update CommandData
- Add a new boolean field `structuredMode` to the `CommandData` class.
- Modify the `collectCommandData()` function in `AiderAction` to include the Structured Mode setting.

### 4. Adjust AiderExecutionStrategy
- Update the `buildCommonArgs()` function to include logic for Structured Mode.
- If Structured Mode is enabled, modify the command to instruct Aider to create a description file instead of making direct changes.

## Implementation Checklist

### AiderSettings
- [ ] Add `useStructuredMode` field to `AiderSettings` class
- [ ] Update `AiderSettingsConfigurable` UI to include Structured Mode checkbox
- [ ] Implement logic to save/load Structured Mode setting

### AiderInputDialog
- [ ] Add Structured Mode checkbox to dialog UI
- [ ] Initialize checkbox with value from AiderSettings
- [ ] Update `getInputText()` to include Structured Mode information

### CommandData
- [ ] Add `structuredMode` field to `CommandData` class
- [ ] Update `collectCommandData()` in `AiderAction` to set `structuredMode`

### AiderExecutionStrategy
- [ ] Modify `buildCommonArgs()` to handle Structured Mode
- [ ] Implement logic to create description file when Structured Mode is enabled

## Testing Plan
1. Verify that the Structured Mode setting can be toggled in the AiderSettings.
2. Ensure the AiderInputDialog correctly displays and allows toggling of Structured Mode.
3. Test that when Structured Mode is enabled, Aider creates description files instead of making direct code changes.
4. Confirm that the description files are saved in the `.coding-aider-plans` directory.
5. Validate that Structured Mode can be overridden for individual requests in the AiderInputDialog.

## Future Considerations
- Consider adding a feature to easily switch between viewing the original code and the structured description.
- Implement a mechanism to apply the changes described in the structured mode files.
