# Coding-Aider Plan Generator Examples

This document provides concrete examples of how the coding-aider plan generator skill works in practice.

## Example 1: New Feature Implementation

### User Request
```
Create a coding-aider plan for implementing user authentication with JWT tokens
```

### Generated Files

#### user-authentication.md
```markdown
# [Coding Aider Plan]

# User Authentication with JWT Tokens

## Overview
Implement a secure user authentication system using JSON Web Tokens (JWT) for API authentication. The system will handle user registration, login, token generation, and token validation for protected endpoints.

## Problem Description
Currently, the application lacks user authentication, leaving all API endpoints unprotected. There is no way to identify users or restrict access to sensitive functionality. We need to implement a robust authentication system that can scale with the application.

## Goals
1. Implement secure user registration and login endpoints
2. Generate and validate JWT tokens for authenticated sessions
3. Protect API endpoints with authentication middleware
4. Implement token refresh mechanism for extended sessions
5. Add password security with proper hashing and validation

## Additional Notes and Constraints
- Use BCrypt for password hashing with minimum 12 rounds
- JWT tokens should expire after 1 hour, refresh tokens after 7 days
- Follow OWASP security guidelines for authentication
- Ensure GDPR compliance for user data handling
- Integrate with existing user database schema

## References
- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- JWT RFC 7519: https://tools.ietf.org/html/rfc7519
- Existing User entity in src/main/kotlin/com/example/entity/User.kt
- Current API structure in src/main/kotlin/com/example/controller/
```

#### user-authentication_checklist.md
```markdown
# [Coding Aider Plan - Checklist]

# User Authentication with JWT Tokens - Implementation Checklist

- [ ] Research existing user entity and database schema
- [ ] Add JWT and BCrypt dependencies to build.gradle.kts
- [ ] Create JWT utility class for token generation and validation
- [ ] Implement password hashing service
- [ ] Create authentication request/response DTOs
- [ ] Implement user registration endpoint
- [ ] Implement user login endpoint with token generation
- [ ] Create JWT authentication filter for endpoint protection
- [ ] Implement token refresh endpoint
- [ ] Add security configuration to protect endpoints
- [ ] Write unit tests for authentication service
- [ ] Write integration tests for authentication endpoints
- [ ] Test authentication flow end-to-end
- [ ] Update API documentation with authentication requirements
- [ ] Add authentication examples to README
```

#### user-authentication_context.yaml
```yaml
---
files:
  - path: "src/main/kotlin/com/example/entity/User.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/controller/AuthController.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/service/AuthService.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/config/SecurityConfig.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/util/JwtUtil.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/dto/AuthRequest.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/dto/AuthResponse.kt"
    readOnly: false
  - path: "src/test/kotlin/com/example/service/AuthServiceTest.kt"
    readOnly: false
  - path: "src/test/kotlin/com/example/controller/AuthControllerTest.kt"
    readOnly: false
  - path: "build.gradle.kts"
    readOnly: true
  - path: "src/main/resources/application.yml"
    readOnly: true
```

## Example 2: Code Refactoring

### User Request
```
I need a coding-aider plan for refactoring the payment processing module to use the strategy pattern
```

### Generated Files

#### payment-processing-strategy-refactor.md
```markdown
# [Coding Aider Plan]

# Payment Processing Strategy Pattern Refactor

## Overview
Refactor the monolithic payment processing module to use the Strategy pattern, allowing for easy addition of new payment providers and improving code maintainability and testability.

## Problem Description
The current PaymentProcessor class contains conditional logic for multiple payment providers (Stripe, PayPal, Credit Card) in a single method, making it difficult to add new providers or modify existing ones. The code violates the Open/Closed Principle and has reduced testability.

## Goals
1. Extract payment processing logic into separate strategy implementations
2. Create a payment strategy factory for provider selection
3. Maintain backward compatibility with existing payment API
4. Improve unit test coverage for individual payment providers
5. Add configuration-based provider registration

## Additional Notes and Constraints
- Must maintain existing API contract to avoid breaking clients
- All existing payment providers must continue to work
- New implementation should be more testable and maintainable
- Consider adding payment provider retry logic
- Include proper error handling for provider failures

## References
- Strategy Pattern documentation: https://refactoring.guru/design-patterns/strategy
- Current PaymentProcessor in src/main/kotlin/com/example/payment/PaymentProcessor.kt
- Payment provider configurations in src/main/resources/payment-providers.yml
- Existing payment tests in src/test/kotlin/com/example/payment/
```

#### payment-processing-strategy-refactor_checklist.md
```markdown
# [Coding Aider Plan - Checklist]

# Payment Processing Strategy Pattern Refactor - Implementation Checklist

- [ ] Analyze current PaymentProcessor implementation
- [ ] Design PaymentStrategy interface
- [ ] Create StripePaymentStrategy implementation
- [ ] Create PayPalPaymentStrategy implementation
- [ ] Create CreditCardPaymentStrategy implementation
- [ ] Implement PaymentStrategyFactory for provider selection
- [ ] Update PaymentProcessor to use strategy pattern
- [ ] Add configuration-based provider registration
- [ ] Create unit tests for each payment strategy
- [ ] Create tests for PaymentStrategyFactory
- [ ] Update integration tests for refactored PaymentProcessor
- [ ] Test backward compatibility with existing clients
- [ ] Add error handling for provider failures
- [ ] Update payment provider documentation
- [ ] Run full test suite to ensure no regressions
```

#### payment-processing-strategy-refactor_context.yaml
```yaml
---
files:
  - path: "src/main/kotlin/com/example/payment/PaymentProcessor.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/payment/PaymentStrategy.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/payment/StripePaymentStrategy.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/payment/PayPalPaymentStrategy.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/payment/CreditCardPaymentStrategy.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/payment/PaymentStrategyFactory.kt"
    readOnly: false
  - path: "src/main/resources/payment-providers.yml"
    readOnly: true
  - path: "src/test/kotlin/com/example/payment/PaymentProcessorTest.kt"
    readOnly: false
  - path: "src/test/kotlin/com/example/payment/PaymentStrategyTest.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/dto/PaymentRequest.kt"
    readOnly: true
```

## Example 3: Bug Fix Implementation

### User Request
```
Create a coding-aider plan for fixing the memory leak in the data processor service
```

### Generated Files

#### data-processor-memory-leak-fix.md
```markdown
# [Coding Aider Plan]

# Data Processor Memory Leak Fix

## Overview
Identify and fix the memory leak in the DataProcessor service that causes gradual memory increase during long-running data processing tasks, leading to OutOfMemoryError after several hours of operation.

## Problem Description
The DataProcessor service is accumulating memory during continuous data processing operations. Memory profiling shows that large data structures are not being garbage collected after processing completion, leading to memory leaks that eventually crash the application.

## Goals
1. Identify root cause of memory leak in DataProcessor
2. Implement proper resource cleanup and memory management
3. Add memory monitoring and alerting capabilities
4. Ensure backward compatibility with existing data processing API
5. Add comprehensive testing for memory management

## Additional Notes and Constraints
- Must maintain existing API contract for data processing
- Fix should not impact processing performance significantly
- Add memory usage monitoring for early detection
- Consider implementing memory limits and automatic cleanup
- Ensure thread safety if DataProcessor is used concurrently

## References
- Java Memory Management Guide: https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/
- Current DataProcessor in src/main/kotlin/com/example/service/DataProcessor.kt
- Memory profiling results in docs/memory-profiling.md
- Existing data processing tests in src/test/kotlin/com/example/service/
```

#### data-processor-memory-leak-fix_checklist.md
```markdown
# [Coding Aider Plan - Checklist]

# Data Processor Memory Leak Fix - Implementation Checklist

- [ ] Review memory profiling results to identify leak sources
- [ ] Analyze DataProcessor code for memory retention issues
- [ ] Identify unclosed resources or circular references
- [ ] Implement proper resource cleanup in DataProcessor
- [ ] Add memory usage monitoring and logging
- [ ] Implement memory limits and automatic cleanup
- [ ] Add unit tests for memory management
- [ ] Create integration tests with memory profiling
- [ ] Test fix with long-running data processing tasks
- [ ] Verify no performance regression
- [ ] Add memory leak detection to CI pipeline
- [ ] Update DataProcessor documentation with memory management notes
- [ ] Create monitoring alerts for memory usage
- [ ] Perform thorough testing with various data sizes
- [ ] Document memory management best practices for team
```

#### data-processor-memory-leak-fix_context.yaml
```yaml
---
files:
  - path: "src/main/kotlin/com/example/service/DataProcessor.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/service/MemoryMonitor.kt"
    readOnly: false
  - path: "src/test/kotlin/com/example/service/DataProcessorTest.kt"
    readOnly: false
  - path: "src/test/kotlin/com/example/service/DataProcessorMemoryTest.kt"
    readOnly: false
  - path: "docs/memory-profiling.md"
    readOnly: true
  - path: "src/main/resources/application.yml"
    readOnly: true
  - path: "src/main/kotlin/com/example/config/MonitoringConfig.kt"
    readOnly: false
```

## Example 4: Performance Optimization

### User Request
```
Create a coding-aider plan for optimizing the database queries in the reporting module
```

### Generated Files

#### reporting-database-optimization.md
```markdown
# [Coding Aider Plan]

# Reporting Module Database Query Optimization

## Overview
Optimize slow-performing database queries in the reporting module to reduce report generation time from average 45 seconds to under 10 seconds for standard reports.

## Problem Description
The reporting module currently generates reports slowly due to inefficient database queries, lack of proper indexing, and repeated data fetching. Database analysis shows several full table scans and N+1 query patterns causing performance bottlenecks.

## Goals
1. Identify and optimize slow database queries in reporting module
2. Add proper database indexes for frequently queried columns
3. Implement query result caching where appropriate
4. Eliminate N+1 query patterns with proper joins
5. Reduce average report generation time to under 10 seconds

## Additional Notes and Constraints
- Must maintain accuracy of all report data
- Cannot change report output format or structure
- Consider adding database connection pooling optimization
- Ensure optimizations work with both PostgreSQL and MySQL
- Add performance monitoring for query execution times

## References
- Database optimization best practices guide
- Current reporting queries in src/main/kotlin/com/example/reporting/
- Database schema in docs/database-schema.md
- Query performance analysis in docs/query-analysis.md
```

#### reporting-database-optimization_checklist.md
```markdown
# [Coding Aider Plan - Checklist]

# Reporting Module Database Query Optimization - Implementation Checklist

- [ ] Analyze current query performance with database profiler
- [ ] Identify top 10 slowest queries in reporting module
- [ ] Review database indexes and recommend additions
- [ ] Optimize reporting queries with proper joins
- [ ] Eliminate N+1 query patterns
- [ ] Implement query result caching for frequently accessed data
- [ ] Add database connection pooling optimization
- [ ] Create performance benchmarks for optimization validation
- [ ] Write unit tests for optimized query methods
- [ ] Create integration tests with realistic data volumes
- [ ] Test optimizations with both PostgreSQL and MySQL
- [ ] Verify report accuracy remains unchanged
- [ ] Add query performance monitoring
- [ ] Update documentation with optimization notes
- [ ] Deploy to staging environment for performance validation
```

#### reporting-database-optimization_context.yaml
```yaml
---
files:
  - path: "src/main/kotlin/com/example/reporting/ReportService.kt"
    readOnly: false
  - path: "src/main/kotlin/com/example/reporting/ReportRepository.kt"
    readOnly: false
  - path: "src/main/resources/db/migration/V2__add_reporting_indexes.sql"
    readOnly: false
  - path: "src/main/kotlin/com/example/config/DatabaseConfig.kt"
    readOnly: false
  - path: "src/test/kotlin/com/example/reporting/ReportServiceTest.kt"
    readOnly: false
  - path: "docs/database-schema.md"
    readOnly: true
  - path: "docs/query-analysis.md"
    readOnly: true
  - path: "src/main/resources/application.yml"
    readOnly: true
```

## Usage Patterns

### Common Request Formats
The skill responds to various request formats:
- "Create a coding-aider plan for [feature/task]"
- "Generate an aider plan to [goal]"
- "I need a coding-aider plan for [implementation]"
- "Make a structured development plan like coding-aider for [task]"

### Response Structure
The skill always:
1. Creates the `.coding-aider-plans/` directory if needed
2. Generates three files: main plan, checklist, and context
3. Uses kebab-case naming for plan files
4. Follows the exact structure used by the IntelliJ plugin
5. Includes relevant project files in the context

### Context Discovery
The skill automatically:
- Searches for relevant source files
- Identifies configuration and build files
- Includes appropriate test files
- Sets correct read/write permissions
- Validates file paths before adding to context

These examples demonstrate the skill's ability to generate comprehensive, structured plans that match the coding-aider plugin's format and workflow.