# Storage Card Example - Bluebird NFC

This repository contains demonstration code for the `keyple-card-cna-storagecard-java-lib` using Bluebird terminals and their specific NFC readers. It shows how to use this Keyple extension library to interact with storage cards (MIFARE Ultralight, MIFARE Classic, ST25, etc.) through the Bluebird contactless reader.

## Description

This Bluebird-specific demo illustrates the main features of the `keyple-card-cna-storagecard-java-lib`:
- Handling of multiple storage card types through the Bluebird NFC reader
- Integration with standard Keyple Calypso operations, including secure transactions with a SAM
- Automated card type detection
- Memory operations for various storage cards

## ⚠️ Important: Storage Card Library Requirement

**This demo requires the official `keyple-card-cna-storagecard-java-lib` library from Calypso Networks Association.**

### Getting the Library
- The storage card extension library is **available on request to CNA members** (Calypso Networks Association)
- **CNA membership required** - Contact: [https://calypsonet.org/](https://calypsonet.org/)
- This is a **member-exclusive library** and is not publicly available

### Installation
1. Request the `keyple-card-cna-storagecard-java-lib-x.x.x.jar` through CNA member channels
2. **Replace** the mock library in the `libs/` folder with the official version
3. The mock library (`keyple-card-cna-storagecard-java-lib-x.x.x-mock.jar`) is provided only for compilation purposes and **will not work at runtime**

### Library Structure
```
libs/
├── keyple-card-cna-storagecard-java-lib-x.x.x-mock.jar            ← Replace with official library
├── keyple-plugin-cna-bluebird-specific-nfc-java-lib-x.x.x.aar    ← Replace with official library
└── [other dependencies...]
```

## Supported Hardware

### Readers
This demo works exclusively with **Bluebird terminals** equipped with a contactless NFC reader and an embedded SAM (e.g., EF500, EF500R).

### Supported Cards
The demo supports:
- Storage Cards (via `keyple-card-cna-storagecard-java-lib`):
    - MIFARE Ultralight (MFOC, MFOICU1)
    - MIFARE Classic 1K
    - ST Microelectronics ST25 / SRT512
- Standard Calypso cards (via core Keyple)

## Prerequisites

- Bluebird terminal with NFC reader and SAM (API level 28+)
- **Official `keyple-card-cna-storagecard-java-lib` from CNA** (available to CNA members only - see above)
- **Official `keyple-plugin-cna-bluebird-specific-nfc-java-lib` from CNA** (available to CNA members only)

## Dependencies

- `keyple-card-cna-storagecard-java-lib`: **Main library for storage card operations (CNA member-exclusive library)**
- `keyple-plugin-cna-bluebird-specific-nfc-java-lib`: **Bluebird NFC plugin (CNA member-exclusive library)**
- `keyple-common`: Keyple common interfaces
- `keyple-card-calypso`: Calypso card extension
- `keyple-card-calypso-crypto-legacysam`: Legacy SAM extension for secure Calypso transactions

## Demo Structure

- `MainActivity.kt`: Main activity handling reader initialization, SAM setup, and card operations
- `MifareClassicKeyProvider.kt`: Provides authentication keys for MIFARE Classic cards
- `CalypsoConstants.kt`: AID, VASUP payload, and file constants for Calypso card operations

## Build and Run

1. Ensure you have **CNA membership** and the **official libraries** from CNA in `libs/`
2. Build the project with Gradle: `./gradlew assembleDebug`
3. Install the APK on a compatible Bluebird terminal

> **Note**: The demo will fail at runtime if using mock libraries. CNA membership and the official libraries are required for actual card operations.

## About the source code

The code is built with **Gradle** and requires **Java 17** or higher.

## Copyright

Copyright (c) 2025 Calypso Networks Association - [https://calypsonet.org/](https://calypsonet.org/)
