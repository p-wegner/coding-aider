# Interactive Aider Execution Strategy

## Requirements

1. Create a new AiderExecutionStrategy called `InteractiveAiderExecutionStrategy`.
2. This strategy should run Aider without the `-m` option, allowing it to act as a shell application.
3. Extend the CommandObserver interface to support user inputs.
4. Handle two types of user input scenarios:
   a. Arbitrary input (lines starting with ">")
   b. Yes/No prompts from Aider
5. Implement a persistent Aider process that can be reused for multiple interactions.

## Implementation Steps

1. Create `InteractiveAiderExecutionStrategy` class:
    - Implement the `AiderExecutionStrategy` interface.
    - In `buildCommand`, exclude the `-m` option and the message argument.
    - Implement logic to start and maintain a persistent Aider process.

2. Modify `CommandObserver` interface:
    - Add a new method `onUserInputRequired(prompt: String): String` to handle user input.

3. Update `CommandExecutor`:
    - Add logic to choose the new strategy when appropriate (e.g., based on a new flag in `CommandData`).
    - Modify the execution loop to support interactive mode:
        - Continuously read output from the process.
        - Detect prompts for user input ("> " or "(y/n)" prompts).
        - Use the new `onUserInputRequired` method to get user input.
        - Send the user input back to the Aider process.
    - Implement logic to reuse the existing Aider process for subsequent interactions.

4. Implement a new `InteractiveCommandSubject`:
    - Extend `GenericCommandSubject` to include the new user input functionality.

5. Update `ShellLikeProcessHandler`:
    - Integrate it with the new `InteractiveAiderExecutionStrategy`.
    - Ensure it can handle both types of user input scenarios.
    - Modify it to work with the persistent Aider process.

6. Modify the UI components:
    - Add support for displaying prompts and accepting user input in the IDE.
    - This may involve creating a new UI component or extending existing ones.
    - Implement a new dialog or extend AiderInputDialog to support interactive mode:
        - Add a toggle or checkbox to enable interactive mode.
        - Modify the dialog to stay open and allow multiple interactions with the same Aider process.
        - Add a text area to display the ongoing conversation and Aider outputs.
        - Implement an input field for users to enter new prompts or respond to Aider's questions.
        - Add buttons for common actions like submitting a new prompt, answering yes/no, or exiting the interactive session.

7. Update `CommandData`:
    - Add a new property to indicate whether to use interactive mode.

8. Implement a session management system:
    - Create a class to manage the lifecycle of the Aider process.
    - Implement methods to start, interact with, and terminate the Aider session.
    - Ensure proper cleanup of resources when the session ends or the IDE closes.

9. Update AiderAction and related classes:
    - Modify the action to support launching the interactive mode.
    - Implement logic to reuse an existing Aider session if one is already running.

## Considerations

- Ensure thread safety when handling user inputs and process I/O.
- Consider implementing a timeout mechanism for user inputs to prevent indefinite waits.
- Evaluate the impact on existing functionality and ensure backwards compatibility.
- Implement a way to gracefully exit the interactive mode and terminate the Aider process.
- The existing aider execution strategies should remain functional alongside the new interactive strategy.
- Consider implementing a way to save and restore interactive sessions.
- Implement error handling and recovery mechanisms for the persistent Aider process.
- Consider adding a feature to switch between different Aider processes (e.g., for different projects or configurations).
