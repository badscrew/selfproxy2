# Automated Tests

This directory contains automated tests for the SSH Tunnel Proxy application using Maestro.

## Test Files

### Main Test Suite
- **`ssh-tunnel-maestro-tests.yaml`** - Comprehensive test suite covering:
  - Profile creation and management
  - VPN connection flow
  - System notification verification
  - Background service testing
  - Network change handling

### Discovery and Development Tests
- **`simple-app-test.yaml`** - Basic UI discovery and element detection
- **`working-app-test.yaml`** - Interactive test based on discovered UI elements
- **`explore-app-ui.yaml`** - UI exploration and screenshot capture
- **`inspect-app-hierarchy.yaml`** - UI hierarchy inspection (development helper)

## Prerequisites

1. **Maestro installed** (run setup script if needed):
   ```bash
   ./.kiro/temp/scripts/setup-maestro-testing.sh
   ```

2. **Android device or emulator connected**:
   ```bash
   adb devices
   ```

3. **App built and installed**:
   ```bash
   ./gradlew app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Running Tests

### Quick Test Run
```bash
# Run main test suite
maestro test tests/ssh-tunnel-maestro-tests.yaml

# Run UI discovery test
maestro test tests/simple-app-test.yaml

# Run interactive test
maestro test tests/working-app-test.yaml
```

### Build and Test (Automated)
```bash
# Build app and run tests automatically
./.kiro/temp/scripts/test-after-build.sh
```

### Continuous Testing
```bash
# Run tests continuously during development
maestro test --continuous tests/
```

### Run All Tests
```bash
# Run all test files in sequence
maestro test tests/
```

## Test Configuration

- **App Package**: `com.selfproxy.vpn.debug` (debug build)
- **Target Platform**: Android API 26+ (Android 8.0+)
- **Test Device**: Any Android device or emulator

## Test Scenarios Covered

### Profile Management
- ✅ Profile creation flow
- ✅ Profile editing
- ✅ Profile deletion
- ✅ Profile validation

### VPN Connection
- ✅ Connection establishment
- ✅ VPN permission handling
- ✅ Connection status verification
- ✅ Disconnection flow

### System Integration
- ✅ Notification display
- ✅ Status bar VPN icon
- ✅ Background service persistence
- ✅ App backgrounding/foregrounding

### Network Scenarios
- ✅ Network change handling (WiFi ↔ Mobile)
- ✅ Connection recovery
- ✅ Service stability

## Debugging Tests

### Screenshots
Maestro automatically captures screenshots on failures. Check:
```bash
~/.maestro/tests/[timestamp]/
```

### Logs
Test execution logs are available in the same directory:
```bash
~/.maestro/tests/[timestamp]/maestro.log
```

### Manual Inspection
Use the exploration tests to understand current UI state:
```bash
maestro test tests/explore-app-ui.yaml
```

## Updating Tests

When the app UI changes:

1. **Run discovery test** to see current elements:
   ```bash
   maestro test tests/simple-app-test.yaml
   ```

2. **Check screenshots** in `~/.maestro/tests/[timestamp]/`

3. **Update element selectors** in test files based on actual UI

4. **Test incrementally** with working-app-test.yaml

## CI/CD Integration

These tests can be integrated into CI/CD pipelines:

```yaml
# GitHub Actions example
- name: Run Maestro Tests
  run: |
    maestro test tests/ssh-tunnel-maestro-tests.yaml
```

## Best Practices

- **Real devices preferred** for VPN testing (emulators have limitations)
- **Clean state** - Start each test with fresh app state
- **Wait for animations** - Use proper wait conditions
- **Optional assertions** - Use `optional: true` for flaky elements
- **Screenshots** - Capture state for debugging

## Troubleshooting

### App Won't Launch
- Check package name: `com.selfproxy.vpn.debug`
- Verify app is installed: `adb shell pm list packages | grep selfproxy`
- Check device connection: `adb devices`

### Elements Not Found
- Run `tests/simple-app-test.yaml` to discover available elements
- Check screenshots in `~/.maestro/tests/[timestamp]/`
- Update element selectors in test files

### VPN Permission Issues
- Ensure VPN permission is granted manually first
- Some emulators don't handle VPN permissions properly
- Use real device for VPN-specific tests

### Performance Issues
- Close other apps on test device
- Use `waitForAnimationToEnd` with appropriate timeouts
- Consider device performance limitations

## Contributing

When adding new tests:

1. **Follow naming convention**: `feature-test.yaml`
2. **Add documentation** in this README
3. **Test on real device** before committing
4. **Use descriptive test names** and comments
5. **Handle edge cases** with optional assertions