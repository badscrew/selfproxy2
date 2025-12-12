# Contributing to SelfProxy

Thank you for your interest in contributing to SelfProxy! This document provides guidelines and instructions for contributing.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Code Style](#code-style)
- [Documentation](#documentation)
- [Community](#community)

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors, regardless of:
- Experience level
- Gender identity and expression
- Sexual orientation
- Disability
- Personal appearance
- Body size
- Race
- Ethnicity
- Age
- Religion
- Nationality

### Our Standards

**Positive behavior includes**:
- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

**Unacceptable behavior includes**:
- Trolling, insulting/derogatory comments, and personal attacks
- Public or private harassment
- Publishing others' private information without permission
- Other conduct which could reasonably be considered inappropriate

### Enforcement

Instances of abusive, harassing, or otherwise unacceptable behavior may be reported by contacting the project team at conduct@example.com. All complaints will be reviewed and investigated promptly and fairly.

## Getting Started

### Prerequisites

- JDK 17 or higher
- Android SDK (API 34)
- Git
- Android Studio (recommended) or IntelliJ IDEA

### Fork and Clone

1. **Fork the repository** on GitHub

2. **Clone your fork**:
```bash
git clone https://github.com/YOUR-USERNAME/selfproxy2.git
cd selfproxy2
```

3. **Add upstream remote**:
```bash
git remote add upstream https://github.com/badscrew/selfproxy2.git
```

4. **Verify remotes**:
```bash
git remote -v
# origin    https://github.com/YOUR-USERNAME/selfproxy2.git (fetch)
# origin    https://github.com/YOUR-USERNAME/selfproxy2.git (push)
# upstream  https://github.com/badscrew/selfproxy2.git (fetch)
# upstream  https://github.com/badscrew/selfproxy2.git (push)
```

## Development Setup

### 1. Configure Android SDK

Create `local.properties` in project root:
```properties
sdk.dir=/path/to/android/sdk
```

**Find your SDK path**:
- **Linux**: `~/Android/Sdk`
- **macOS**: `~/Library/Android/sdk`
- **Windows**: `C:\Users\YourName\AppData\Local\Android\sdk`

### 2. Build the Project

```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Check for issues
./gradlew check
```

### 3. Open in Android Studio

1. Open Android Studio
2. File → Open
3. Select the `selfproxy` directory
4. Wait for Gradle sync to complete

### 4. Run on Device/Emulator

1. Connect Android device or start emulator
2. Click "Run" (green play button)
3. Select target device
4. App will install and launch

## Making Changes

### 1. Create a Branch

Always create a new branch for your changes:

```bash
# Update main branch
git checkout main
git pull upstream main

# Create feature branch
git checkout -b feature/your-feature-name

# Or for bug fixes
git checkout -b fix/issue-number-description
```

**Branch naming conventions**:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring
- `test/` - Test additions/changes
- `chore/` - Build/tooling changes

### 2. Make Your Changes

**Guidelines**:
- Keep changes focused and atomic
- Write clear, self-documenting code
- Add comments for complex logic
- Follow existing code style
- Update documentation as needed
- Add tests for new functionality

### 3. Commit Your Changes

**Commit message format**:
```
type(scope): subject

body (optional)

footer (optional)
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Test changes
- `chore`: Build/tooling changes

**Examples**:
```bash
git commit -m "feat(vless): add Reality protocol support"
git commit -m "fix(vpn): prevent DNS leak on network change"
git commit -m "docs(readme): update installation instructions"
```

**Good commit messages**:
- ✅ `feat(vless): add Reality protocol support`
- ✅ `fix(connection): handle network change during handshake`
- ✅ `docs(qr-code): add troubleshooting section`

**Bad commit messages**:
- ❌ `update code`
- ❌ `fix bug`
- ❌ `changes`

## Testing

### Running Tests

**Unit tests**:
```bash
./gradlew test
```

**Property-based tests**:
```bash
./gradlew test --tests "*PropertiesTest"
```

**Instrumented tests**:
```bash
./gradlew connectedAndroidTest
```

**Specific test**:
```bash
./gradlew test --tests "com.selfproxy.vpn.data.model.VlessProfilePropertiesTest"
```

### Writing Tests

**Unit test example**:
```kotlin
class ProfileRepositoryTest {
    private lateinit var repository: ProfileRepository
    
    @Before
    fun setup() {
        repository = ProfileRepositoryImpl(/* dependencies */)
    }
    
    @Test
    fun `creating profile should persist data`() = runTest {
        // Arrange
        val profile = ServerProfile(/* ... */)
        
        // Act
        val result = repository.createProfile(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val retrieved = repository.getProfile(result.getOrThrow())
        assertEquals(profile.name, retrieved?.name)
    }
}
```

**Property-based test example**:
```kotlin
class ProfilePropertiesTest {
    @Test
    fun `profile round-trip should preserve data`() = runTest {
        // Feature: selfproxy, Property 5: Profile Update Round-Trip
        // Validates: Requirements 1.7
        checkAll(
            iterations = 100,
            Arb.serverProfile()
        ) { profile ->
            val id = repository.createProfile(profile).getOrThrow()
            val retrieved = repository.getProfile(id)
            
            retrieved shouldNotBe null
            retrieved?.name shouldBe profile.name
        }
    }
}
```

### Test Requirements

- All new features must include tests
- Bug fixes should include regression tests
- Aim for 80% code coverage for new code
- Property-based tests for universal properties
- Unit tests for specific behaviors

## Submitting Changes

### 1. Update Your Branch

Before submitting, update your branch with latest changes:

```bash
# Fetch latest changes
git fetch upstream

# Rebase your branch
git rebase upstream/main

# If conflicts, resolve them and continue
git rebase --continue
```

### 2. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

### 3. Create Pull Request

1. Go to your fork on GitHub
2. Click "Pull Request"
3. Select your branch
4. Fill in the PR template:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Changes Made
- Bullet point list of changes
- Be specific and clear

## Testing
- [ ] Unit tests added/updated
- [ ] Property tests added/updated
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Tests pass locally

## Related Issues
Closes #42
Related to #38
```

### 4. Code Review

**What to expect**:
- Maintainers will review your PR
- You may be asked to make changes
- Discussion may occur in PR comments
- Be patient and respectful

**Responding to feedback**:
- Address all comments
- Make requested changes
- Push updates to your branch
- Reply to comments when done

### 5. Merging

Once approved:
- Maintainer will merge your PR
- Your changes will be in the next release
- You'll be credited in release notes

## Code Style

### Kotlin Style Guide

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

**Naming**:
```kotlin
// Classes: PascalCase
class ProfileRepository

// Functions: camelCase
fun createProfile()

// Constants: UPPER_SNAKE_CASE
const val DEFAULT_PORT = 51820

// Properties: camelCase
val serverAddress: String
```

**Formatting**:
```kotlin
// Indentation: 4 spaces
class Example {
    fun method() {
        if (condition) {
            doSomething()
        }
    }
}

// Line length: 120 characters max
// Break long lines at logical points
val result = repository
    .createProfile(profile)
    .getOrThrow()
```

**Documentation**:
```kotlin
/**
 * Creates a new server profile.
 *
 * @param profile The profile to create
 * @return Result containing profile ID on success
 */
suspend fun createProfile(profile: ServerProfile): Result<Long>
```

### Android Conventions

**Compose**:
```kotlin
// Composable functions: PascalCase
@Composable
fun ProfileScreen() {
    // ...
}

// Preview functions
@Preview
@Composable
fun ProfileScreenPreview() {
    // ...
}
```

**Resources**:
```xml
<!-- Strings: snake_case -->
<string name="profile_name">Profile Name</string>

<!-- IDs: snake_case -->
<View android:id="@+id/profile_name" />

<!-- Layouts: snake_case -->
<!-- activity_main.xml -->
<!-- fragment_profile.xml -->
```

### Linting

Run linter before committing:

```bash
# Check style
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat
```

## Documentation

### Code Documentation

**KDoc comments** for public APIs:
```kotlin
/**
 * Manages VPN connections using VLESS protocol.
 *
 * This class provides a unified interface for connecting to VPN servers
 * using the VLESS protocol with multiple transport options.
 *
 * @property vlessAdapter VLESS protocol implementation
 */
class ConnectionManager(
    private val vlessAdapter: ProtocolAdapter
)
```

### User Documentation

Update relevant documentation:
- `README.md` - Main documentation
- `docs/` - Detailed guides
- `docs/server-setup/` - Server setup guides
- `PRIVACY.md` - Privacy policy
- `CONTRIBUTING.md` - This file

### Changelog

Add entry to `CHANGELOG.md`:
```markdown
## [Unreleased]

### Added
- Reality protocol support for VLESS (#123)

### Fixed
- DNS leak on network change (#124)

### Changed
- Improved battery efficiency (#125)
```

## Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Pull Requests**: Code contributions
- **Email**: For private matters (security, conduct)

### Getting Help

**Before asking**:
1. Check existing documentation
2. Search existing issues
3. Read troubleshooting guide

**When asking**:
- Provide clear description
- Include relevant details (OS, Android version, etc.)
- Share error messages/logs
- Describe steps to reproduce

### Reporting Bugs

**Use the bug report template**:
```markdown
**Describe the bug**
Clear description of the bug

**To Reproduce**
1. Go to '...'
2. Click on '...'
3. See error

**Expected behavior**
What you expected to happen

**Screenshots**
If applicable

**Environment**
- Android version:
- Device:
- App version:

**Additional context**
Any other relevant information
```

### Requesting Features

**Use the feature request template**:
```markdown
**Is your feature request related to a problem?**
Clear description of the problem

**Describe the solution you'd like**
Clear description of desired solution

**Describe alternatives you've considered**
Alternative solutions or features

**Additional context**
Any other relevant information
```

## Recognition

### Contributors

All contributors will be:
- Listed in `CONTRIBUTORS.md`
- Credited in release notes
- Mentioned in relevant documentation

### Types of Contributions

We value all contributions:
- 💻 Code contributions
- 📝 Documentation improvements
- 🐛 Bug reports
- 💡 Feature suggestions
- 🧪 Testing and QA
- 🌍 Translations (future)
- 📢 Community support

## License

By contributing to SelfProxy, you agree that your contributions will be licensed under the same license as the project.

## Questions?

- Open a [GitHub Discussion](https://github.com/badscrew/selfproxy2/discussions)
- Email: contribute@example.com

---

**Thank you for contributing to SelfProxy!** 🎉

Your contributions help make privacy accessible to everyone.
