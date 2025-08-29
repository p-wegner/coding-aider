# IntelliJ Plugin Testing Documentation

This document provides a summary of the key information for testing IntelliJ Platform plugins, based on the official documentation.

## Testing Overview

Most tests for IntelliJ Platform plugins are model-level functional tests that run in a headless environment. These tests utilize real production implementations for most components, with the exception of many UI components. The testing approach focuses on testing features as a whole rather than individual functions.

Key characteristics of this testing philosophy include:
*   **Model-Level Testing**: Tests interact directly with the underlying data model instead of the Swing UI.
*   **File-Based I/O**: Tests typically take source files as input, execute a feature, and then compare the output with expected results, which can be defined in separate files or through special markup in the input file.
*   **Stability Over Speed**: This method results in highly stable tests that require minimal maintenance, even as the underlying code is refactored. This benefit is considered to outweigh the slower execution speed and more complex debugging process compared to traditional, isolated unit tests.
*   **Minimal Mocking**: The recommended approach is to test with real components rather than using mocks, as it can be difficult to mock all the necessary interactions with the platform's components.

The documentation also provides links to tutorials and code samples for writing tests, including integration tests.

## Integration and UI Tests

Integration tests are crucial for ensuring plugin quality and reliability. Unlike unit tests, which assess isolated components, integration tests validate how a plugin functions within the full, running IDE, offering a clearer picture of real-world performance.

### Why Bother with Integration Tests?

*   **Testing Complex Scenarios**: Many plugin features, especially those involving user interface (UI) interactions, cannot be effectively tested with unit tests alone.
*   **Full Product Validation**: By running tests against the complete product, developers can identify issues that unit tests might miss, such as module interaction problems, classpath conflicts, and plugin declaration errors.
*   **User Story Confirmation**: Integration tests often mirror real user scenarios, providing confidence that the plugin works as expected from start to finish.

### The Core Components of the Testing Framework

The integration testing framework consists of two main components:

*   **Starter**: This component handles the test environment setup, including IDE configuration, test project setup, starting the IDE with the plugin installed, and collecting the output.
*   **Driver**: This provides an API for interacting with the running IDE from the test code. This can include interacting with the UI and making JMX calls to the IDE's API.

### How it Works: A Two-Process Architecture

Integration tests for IntelliJ plugins operate across two separate processes:

1.  **The Test Process**: This is where your test code is executed. It manages the lifecycle of the IDE, controls the flow of the test, and makes assertions about the results.
2.  **The IDE Process**: This is a full instance of the IntelliJ IDE running with your plugin installed. The test process sends commands to this IDE instance to simulate user interactions and other events.

This separation allows for comprehensive testing of the plugin in a realistic environment. The framework provides a `remote-robot` to send messages to the IDE for UI testing, communicating over HTTP.

### Getting Started with Integration Testing

To begin writing integration tests, you need to add the necessary dependencies to your Gradle build file. The framework supports JUnit 5 and provides libraries for the Starter and Driver components. Once configured, you can write a basic JUnit 5 test that can install your plugin and run it on any OS and any IDE version starting from 2024.2.

The test setup typically involves pointing to the plugin distribution file so the Starter framework knows what to install in the test IDE instance. A simple first test might involve starting the IDE with the plugin installed, waiting for it to load completely, and then shutting it down to confirm that the basic setup is working correctly.


## Tests and Fixtures

You can use a standard base class like `BasePlatformTestCase` for test setup, or manually manage it with a fixture class. The `IdeaTestFixtureFactory` class is used to create fixture instances for your test environment. While the platform uses various frameworks like JUnit, TestNG, and Cucumber, most tests are written with JUnit 3.

## Light and Heavy Tests

IntelliJ Platform plugin tests run in a real environment, leading to a distinction between light and heavy tests based on performance. Light tests are recommended because they reuse a project from a previous run, making them faster. Heavy tests, in contrast, create a new project for each test, which is a more expensive operation. Different base classes are used for each type, with `LightPlatformTestCase` for light tests and `HeavyPlatformTestCase` for heavy tests. Multi-module projects require the use of heavy tests.

## Test Project and Testdata Directories

When testing IntelliJ plugins, a test fixture creates a temporary or in-memory test project. Test data, such as files for testing plugin features, should be stored in a `testdata` directory, and you must override the `getTestDataPath()` method to point to it. The `CodeInsightTestFixture` class helps manage this by copying files from your `testdata` directory into the test project for execution. You can also use special markup like `<caret>` and `<selection>` within these test files to set the caret position or select text.

## Writing Tests

This document outlines how to write automated tests for IntelliJ Platform plugins. It highlights that as of version 2024.2, all test framework dependencies must be explicitly declared. The testing framework provides helper methods to simulate user actions such as typing, executing actions, invoking code completion, and running refactorings. To verify test outcomes, you can compare the results with a file containing the expected output using `checkResultByFile()` or compare entire directories for project-wide changes. For further guidance, the page links to a step-by-step tutorial, code samples, and a list of other useful testing classes.

## Testing Highlighting

The IntelliJ Platform provides tools for testing plugin highlighting features like inspections and syntax. You can use `CodeInsightTestFixture.checkHighlighting()` to compare actual highlighting with expected results defined in test files using an XML-like markup. This markup specifies the severity (e.g., `<warning>`) and an optional description of the highlighted code. To test inspections, they must be explicitly enabled, and for syntax highlighting, `EditorTestUtil.testFileSyntaxHighlighting()` is used with an answer file. The platform also includes an internal action to help generate this markup from a code sample.

## Testing FAQ

This document provides a FAQ for testing IntelliJ Platform plugins, covering useful classes like `UsefulTestCase` and `PlatformTestUtil` for test setup and execution. It addresses common issues such as avoiding flaky tests by using `super.tearDown()` in a `finally` block and handling unresolved test-framework dependencies. The guide also offers techniques for various testing scenarios, including how to replace components or services, run performance tests, and test plugins for JVM languages. Additionally, it explains how to manage asynchronous operations like indexing and `ProjectActivity` in recent platform versions.

## Service Mocking in Light Tests

There are two primary ways to replace services for testing purposes: dynamically at runtime using `ServiceContainerUtil`, or statically in your `plugin.xml` via the `testServiceImplementation` attribute.

### Dynamic Mocking with `ServiceContainerUtil` (Recommended)

This is the most flexible approach for mocking in light tests. The `ServiceContainerUtil` class allows you to replace a service with a mock implementation for the duration of a single test. This is ideal for isolating the code you're testing from its dependencies.

Here is an example using `ServiceContainerUtil` with Mockito in a test based on `BasePlatformTestCase`:

```java
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.mockito.Mockito;

// The service you want to mock
public interface MyApplicationService {
    String getSomeValue();
}

// A component that uses the service
public class MyComponent {
    public String doSomething() {
        MyApplicationService service = ApplicationManager.getApplication().getService(MyApplicationService.class);
        return "Component got: " + service.getSomeValue();
    }
}

// The test class
public class MyComponentTest extends BasePlatformTestCase {

    public void testComponentWithMockService() {
        // 1. Create the mock service using Mockito
        MyApplicationService mockService = Mockito.mock(MyApplicationService.class);
        Mockito.when(mockService.getSomeValue()).thenReturn("mock value");

        // 2. Replace the real service with the mock for the duration of the test
        ServiceContainerUtil.replaceService(
                ApplicationManager.getApplication(),
                MyApplicationService.class,
                mockService,
                getTestRootDisposable()
        );

        // 3. Run the code that uses the service
        MyComponent myComponent = new MyComponent();
        String result = myComponent.doSomething();

        // 4. Assert that the component used the mock service
        assertEquals("Component got: mock value", result);
    }
}
```

**Key Points:**
*   **`replaceService()`**: This method substitutes a service implementation within a specific container (e.g., application or project).
*   **Test Lifecycle**: The replacement is tied to a `Disposable`. Using `getTestRootDisposable()` in a `BasePlatformTestCase` ensures the mock is automatically removed after the test finishes.
*   **Service Level**: You can replace services at the application level (`ApplicationManager.getApplication()`) or project level (`getProject()`).

### Mocking Project-Scoped Services

The process for mocking project-scoped services is very similar. Instead of the application container, you provide the `Project` instance to `ServiceContainerUtil.replaceService()`.

Here is an example of how to mock a project-scoped service:

```java
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.mockito.Mockito;

// A project-scoped service
public interface MyProjectService {
    String getData();
}

public class MyComponentTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Create and register the mock service before each test
        MyProjectService mockService = Mockito.mock(MyProjectService.class);
        Project project = getProject();
        ServiceContainerUtil.replaceService(project, MyProjectService.class, mockService, getTestRootDisposable());
    }

    public void testComponentUsesMockProjectService() {
        // 1. Arrange: Get the (mocked) service from the project
        MyProjectService service = getProject().getService(MyProjectService.class);
        Mockito.when(service.getData()).thenReturn("mocked project data");

        // 2. Act: Call the code that uses the service
        String result = service.getData();

        // 3. Assert: Verify the result and interaction with the mock
        assertEquals("mocked project data", result);
        Mockito.verify(service).getData();
    }
}
```

### Static Replacement with `testServiceImplementation`

You can specify a test-only implementation for a service directly in your `plugin.xml` file. The IntelliJ Platform will use this implementation when running in a test environment. This approach is less flexible than dynamic mocking but can be useful for complex services that require a different implementation for testing.

Here is an example of how to declare a service with a separate test implementation in `plugin.xml`:

```xml
<extensions defaultExtensionNs="com.intellij">
    <applicationService
            serviceInterface="com.example.MyService"
            serviceImplementation="com.example.MyServiceImpl"
            testServiceImplementation="com.example.MyServiceTestImpl"/>
</extensions>
```

*   `serviceInterface`: The interface for your service.
*   `serviceImplementation`: The main implementation used in production.
*   `testServiceImplementation`: The alternative implementation used in tests.
