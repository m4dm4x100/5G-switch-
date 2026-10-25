# 5G Switch

A small Android Quick Settings tile for requesting **NR-only** when enabled and **LTE-only** when disabled.

## Important requirements

Android does not let a normal third-party app change allowed modem network types directly. This app uses **Shizuku** to run Android's `cmd phone set-allowed-network-types-for-users` command with shell privileges. Shizuku must be installed, started, and authorized for this app. Shizuku's wireless-debugging service commonly needs to be started again after a reboot. Root is an alternative, but is not required if the device's Android/OEM build permits the shell command.

This command and modem behavior vary by Android version and manufacturer. A successful command only confirms Android accepted the request; it does **not** guarantee the modem actually registered on the requested radio mode. The app tile remembers the last requested mode rather than independently reading back the modem's actual mode.

## Modes

- **Tile enabled / selected:** NR-only request (network type NR, bit 20).
- **Tile disabled / unselected:** LTE-only request (network type LTE, bit 13).

NR-only can disconnect mobile data if the carrier or area does not support a compatible 5G standalone connection. LTE-only disables fallback to 2G/3G. Keep Wi-Fi available and be prepared to restore the normal network mode in Android settings if service is lost. This app does not lock specific frequency bands; it requests a radio access technology.

## Setup

1. Install Shizuku from its official source: https://shizuku.rikka.app/
2. Start Shizuku using Wireless debugging (Developer options) and pair if prompted.
3. Install **5G Switch**, open it, and tap **Authorize Shizuku**.
4. Pull down Quick Settings twice, tap Edit, and add the **5G / LTE** tile.
5. Tap the tile to request NR-only; tap again to request LTE-only.

The first use may fail if the OEM build blocks the shell command or the phone uses a different SIM slot. This version targets SIM slot 0. Do not rely on the tile state as proof of actual network registration.

## Build

Built with Android Gradle Plugin 8.7.3, JDK 17, and compile SDK 35. GitHub Actions builds a debug APK and attaches it to a GitHub Release on pushes to `main`.

## Device testing

The project has not yet been verified on a physical OnePlus Nord CE 3 / OxygenOS 15. Please test cautiously and report the Android version, Shizuku status, and any error toast if the command is rejected.
