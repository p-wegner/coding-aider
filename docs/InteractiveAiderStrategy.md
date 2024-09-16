# Interactive Aider Execution Strategy

## Requirements

1. Create a new AiderExecutionStrategy called `InteractiveAiderExecutionStrategy`.
2. This strategy should run Aider without the `-m` option, allowing it to act as a shell application.
3. Extend the CommandObserver interface to support user inputs.
4. Handle two types of user input scenarios:
   a. Arbitrary input (lines starting with ">")
   b. Yes/No prompts from Aider

## Implementation Steps

1. Create `InteractiveAiderExecutionStrategy` class:
   - Implement the `AiderExecutionStrategy` interface.
   - In `buildCommand`, exclude the `-m` option and the message argument.

2. Modify `CommandObserver` interface:
   - Add a new method `onUserInputRequired(prompt: String): String` to handle user input.

3. Update `CommandExecutor`:
   - Add logic to choose the new strategy when appropriate (e.g., based on a new flag in `CommandData`).
   - Modify the execution loop to support interactive mode:
     - Continuously read output from the process.
     - Detect prompts for user input ("> " or "(y/n)" prompts).
     - Use the new `onUserInputRequired` method to get user input.
     - Send the user input back to the Aider process.

4. Implement a new `InteractiveCommandSubject`:
   - Extend `GenericCommandSubject` to include the new user input functionality.

5. Update `ShellLikeProcessHandler`:
   - Integrate it with the new `InteractiveAiderExecutionStrategy`.
   - Ensure it can handle both types of user input scenarios.

6. Modify the UI components:
   - Add support for displaying prompts and accepting user input in the IDE.
   - This may involve creating a new UI component or extending existing ones.

7. Update `CommandData`:
   - Add a new property to indicate whether to use interactive mode.

8. Extend error handling and logging:
   - Ensure proper handling of interactive mode-specific scenarios.
   - Log user interactions appropriately.

9. Write unit tests:
   - Test the new strategy and interactive functionality.
   - Mock user inputs for testing purposes.

10. Update documentation:
    - Describe the new interactive mode in user documentation.
    - Update developer documentation with the new classes and interfaces.

## Considerations

- Ensure thread safety when handling user inputs and process I/O.
- Consider implementing a timeout mechanism for user inputs to prevent indefinite waits.
- Evaluate the impact on existing functionality and ensure backwards compatibility.
- Consider adding a way to gracefully exit the interactive mode.
