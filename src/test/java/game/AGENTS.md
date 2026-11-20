# JoshHack Agent Instructions

## Environment
- Java 17, Maven (always use the wrapper: ./mvnw), JUnit 5
- Production code: src/main/java · Tests: src/test/java

## Before work is considered complete, run
- ./mvnw clean test (all tests must pass)
- ./mvnw clean test-compile org.pitest:pitest-maven:mutationCoverage
- ./mvnw compile spotbugs:spotbugs

## Rules
- Preserve existing behavior unless a change is explicitly requested.
- Never weaken, remove, skip, or disable tests just to make the build pass.
- Clearly identify any production-code (src/main) change for human review and get approval before making it.
- Never commit, push, or merge changes. A human reviews and pushes all work.