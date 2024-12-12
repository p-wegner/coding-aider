[Coding Aider Plan - Checklist]

# Summarized Output Setting Implementation Checklist

Plan: [summarized_output_setting.md](summarized_output_setting.md)

## Settings Implementation

- [x] Add summarizedOutput boolean property to AiderSettings
- [x] Add UI toggle in settings dialog
- [x] Add default value in AiderDefaults
- [x] Implement persistence for the setting

## Command Line Integration

- [x] Add command line argument handling in AiderExecutionStrategy
- [x] Update buildCommonArgs to include summary flag
- [x] Add option to CommandData class

## Output Processing

- [x] Define XML structure for summary output
- [x] Implement summary flag handling in command execution
- [ ] Add summary parsing capability to output processor
