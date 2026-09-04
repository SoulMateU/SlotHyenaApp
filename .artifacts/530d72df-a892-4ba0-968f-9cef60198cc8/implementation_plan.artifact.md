# Fix Gradle Sync Error: Plugin Incompatibility

The project is currently using Gradle 9.7.1, but the Android Gradle Plugin (AGP) version 8.13.2 is incompatible with Gradle 9.6.0 and later because it relies on internal Gradle APIs that were removed.

The error message suggests two paths:
1. Update the plugin to a version that doesn't use these internal APIs.
2. Downgrade Gradle to 9.5.

Given that AGP 8.13.2 is a major version behind the latest AGP (9.4.0) and Gradle 9.7.1 is very recent, the most stable and direct fix to resolve the sync error is to downgrade Gradle to 9.5, which is the highest version compatible with AGP 8.x.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle-wrapper.properties](file:///C:/APK/SlotHyenaApp/gradle/wrapper/gradle-wrapper.properties)
- Downgrade `distributionUrl` from Gradle 9.7.1 to 9.5.

## Verification Plan

### Automated Tests
- Run Gradle sync to verify that the "Plugin relies on Gradle internal API" error is resolved.
- Build the project using `./gradlew assembleDebug`.

### Manual Verification
- Verify that the project structure is correctly recognized by Android Studio after sync.
