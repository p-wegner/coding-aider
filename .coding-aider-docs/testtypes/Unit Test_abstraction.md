# Testing Patterns and Conventions in CodingAider

This document outlines the testing patterns and conventions used in the CodingAider project, specifically focusing on the execution strategy components.

## Testing Framework and Libraries

The project uses the following testing frameworks and libraries:

- **JUnit 5**: The primary testing framework, with support for parameterized tests
- **AssertJ**: For fluent assertions (`assertThat()`)
- **Mockito-Kotlin**: For mocking dependencies
- **IntelliJ Platform Test Framework**: For testing IntelliJ IDEA plugin components

## Test Structure

### Test Class Organization

Test classes follow these conventions:

1. Test classes are named with the pattern `{ClassUnderTest}Test`
2. Test classes extend `BasePlatformTestCase` for IntelliJ platform integration
3. Test classes are organized in the same package structure as the classes they test

### Test Method Organization

Test methods follow these conventions:

1. Methods are named descriptively using backticks: `` `MethodName does something specific` ``
2. Parameterized tests use `@ParameterizedTest` and `@EnumSource` for testing multiple scenarios
3. Regular tests use `@Test` annotation

## Test Setup

Tests typically include:

1. A `@BeforeEach` method named `mySetup()` that:
   - Sets up the test fixture
   - Initializes mocks
   - Registers service instances
   - Prepares test data

2. Mock initialization using:
   ```kotlin
   myProject.registerServiceInstance(ServiceClass::class.java, mock())
   ```

3. Test data preparation, often loading from resource files:
   ```kotlin
   val resourcesPath = "src/test/resources"
   structuredModeSystemMessage = File("$resourcesPath/structured_mode_system_message.txt").readText().trimIndent()
   ```

## Given-When-Then Pattern

Tests follow the Given-When-Then pattern:

1. **Given**: Set up the test scenario and preconditions
   ```kotlin
   commandData = commandData.copy(
       aiderMode = AiderMode.STRUCTURED,
       message = "Continue with the plan",
       files = commandData.files + existingPlanFile
   )
   ```

2. **When**: Execute the method under test
   ```kotlin
   val command = nativeStrategy.buildCommand(commandData)
   ```

3. **Then**: Assert the expected outcomes
   ```kotlin
   assertThat(command).contains("-m")
   assertThat(command.last()).contains("A plan already exists. Continue implementing the existing plan")
   ```

## Mocking Conventions

Mocking follows these patterns:

1. Dependencies are mocked using Mockito-Kotlin:
   ```kotlin
   val mockDependency: DependencyClass = mock()
   ```

2. Stubbing behavior with `whenever`:
   ```kotlin
   whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
   ```

## Assertion Patterns

Assertions use AssertJ's fluent API:

1. For collections:
   ```kotlin
   assertThat(command).containsExactly("docker", "run", ...)
   assertThat(command).contains("--auto-commits")
   ```

2. For strings:
   ```kotlin
   assertThat(command.last()).contains("Continue with the plan")
   assertThat(command.last()).isEqualTo("$structuredModeSystemMessage\n<UserPrompt> $multiLineMessage </UserPrompt>")
   ```

3. For negative assertions:
   ```kotlin
   assertThat(command).doesNotContain("--auto-commits", "--no-auto-commits")
   ```

## Testing Edge Cases

Tests cover various edge cases:

1. Different configuration settings:
   ```kotlin
   @ParameterizedTest
   @EnumSource(AiderSettings.AutoCommitSetting::class)
   fun `NativeAiderExecutionStrategy handles auto-commits setting`(autoCommitSetting: AiderSettings.AutoCommitSetting) {
       // Test with each enum value
   }
   ```

2. Empty or null values:
   ```kotlin
   val command = dockerStrategy.buildCommand(commandData.copy(llm = ""))
   ```

3. Files outside project directory:
   ```kotlin
   val outsideFile = FileData("/outside/file2.txt", true)
   commandData = commandData.copy(files = commandData.files + outsideFile)
   ```

## Best Practices

1. Keep tests focused on a single behavior
2. Use descriptive method names that explain the test scenario
3. Separate test setup from assertions
4. Use parameterized tests for testing multiple similar scenarios
5. Mock external dependencies to isolate the unit under test
6. Use appropriate assertions for the expected outcomes
7. Test both happy paths and edge cases
