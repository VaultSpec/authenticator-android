# VaultSpec Authenticator

A free, open-source TOTP authenticator for Android with an encrypted vault, biometric unlock, and a clean Material 3 UI.

## Features

- **Encrypted vault** — TOTP secrets stored with AES-256-GCM; master key derived via SCrypt (N=32768, r=8, p=1)
- **Biometric unlock** — fingerprint / face authentication via AndroidX Biometric
- **PIN / password unlock** — full SCrypt-based key derivation on setup
- **Session timeout** — auto-lock after configurable idle period (Immediately, 2 / 5 / 10 / 15 min)
- **QR code scanner** — add accounts by scanning `otpauth://` QR codes (ML Kit)
- **Manual entry** — add accounts without a camera
- **Categories** — organise tokens into custom groups
- **Encrypted backup & restore** — export / import vault as an AES-256-GCM encrypted JSON file
- **Service icons** — live favicon fetching via DuckDuckGo (140+ built-in domain mappings)
- **Dark mode** — full dark theme with in-app toggle
- **Tap to reveal** — hide codes until tapped, with configurable auto-hide timeout
- **Tap to copy** — one-tap clipboard copy
- **Swipe to delete** — swipe left or right on a token card with confirmation dialog
- **Screenshot protection** — prevent screen capture (toggleable)

## Security

| Layer | Algorithm |
|---|---|
| Vault encryption | AES-256-GCM |
| Key derivation | SCrypt (N=32 768, r=8, p=1) |
| Preferences | AES-256-SIV (keys) + AES-256-GCM (values) via EncryptedSharedPreferences |
| Biometric key wrapping | Android Keystore AES-256-GCM |
| Backup file | AES-256-GCM with random IV |

## Requirements

- Android 8.0+ (API 26)
- Android Studio Hedgehog or newer
- JDK 17

## Disclaimer

> **Only download VaultSpec Authenticator from verified sources:**
>
> - [GitHub Releases](https://github.com/VaultSpec/authenticator-android/releases)
> - Google Play Store *(under process)*
>
> You can verify the authenticity of the APK using `apksigner`:
>
> ```bash
> # Verify the APK signature
> apksigner verify --verbose --print-certs VaultSpec-v*.apk
>
> # Expected signer: look for the VaultSpec certificate fingerprint
> # The output should show "Verified using v2 scheme" (or v3/v4)
> # and no warnings about unsigned entries.
> ```
>
> **Do not install APKs obtained from unofficial sources.** Tampered builds may compromise your 2FA secrets.

## Tech Stack

- **Language**: Kotlin 2.1.0
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt 2.53.1
- **Database**: Room 2.6.1
- **Camera**: CameraX 1.4.1 + ML Kit Barcode Scanning 17.3.0
- **Image loading**: Coil 2.7.0
- **Crypto**: Bouncy Castle (SCrypt) + JCA (AES-GCM)
- **Build**: AGP 8.7.3, Gradle 8.11.1

## License

GPL-3.0 — see [LICENSE](LICENSE).
