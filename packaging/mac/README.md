macOS builds are for development and testing only (production tills are Windows): unsigned and not
notarized. On first open: right-click the app > Open (or System Settings > Privacy & Security > Open Anyway).
If tills ever run on Macs: add an Apple Developer ID, `mac.identity`, hardened runtime + entitlements for the
bundled Java and PostgreSQL, and notarization.
