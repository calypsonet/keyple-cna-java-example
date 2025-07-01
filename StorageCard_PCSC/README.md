# Keyple Card Storage Java Library - PC/SC Demo

This repository contains demonstration code for the `keyple-card-storage-java-lib` using PC/SC readers. It shows how to use this Keyple extension library to interact with storage cards (MIFARE Ultralight, ST25, etc.) through PC/SC contactless readers.

## Description

This PC/SC-specific demo illustrates the main features of the `keyple-card-storage-java-lib`:
- Handling of multiple storage card types through PC/SC readers
- Integration with standard Keyple Calypso operations
- Automated card type detection using PC/SC ATR patterns
- Memory operations for various storage cards

## ⚠️ Important: Storage Card Library Requirement

**This demo requires the official `keyple-card-storage-java-lib` library from Calypso Networks Association.**

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
├── keyple-card-cna-storagecard-java-lib-x.x.x-mock.jar    ← Replace with official library
└── [other dependencies...]
```

## Supported Hardware

### Readers
This demo works exclusively with PC/SC contactless readers, such as:
- ASK LoGO
- Any standard PC/SC compliant contactless reader that also supports MIFARE Ultralight and ST25 storage cards

### Supported Cards
The demo supports:
- Storage Cards (via keyple-card-storage-java-lib):
    - MIFARE Ultralight (1st generation)
    - ST Microelectronics ST25 / SRT512
- Standard Calypso cards (via core Keyple)

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- PC/SC middleware installed on your system
- A compatible PC/SC contactless reader
- **Official `keyple-card-storage-java-lib` from CNA** (available to CNA members only - see above)

## Dependencies

- `keyple-card-storage-java-lib`: **Main library for storage card operations (CNA member-exclusive library)**
- `keyple-plugin-pcsc`: PC/SC plugin for reader communication (required)
- `keyple-common`: Keyple common interfaces
- `keyple-card-calypso`: Calypso card extension

## Demo Structure

- `MultiTechTransaction.java`: Demonstrates PC/SC reader configuration and card operations

## Build and Run

1. Ensure you have **CNA membership** and the **official storage card library** from CNA in `libs/`
2. Build the project with Gradle
3. Run the demo: `java -cp ... MultiTechTransaction`

> **Note**: The demo will fail at runtime if using the mock library. CNA membership and the official library are required for actual card operations.

## Copyright

Copyright (c) 2025 Calypso Networks Association - [https://calypsonet.org/](https://calypsonet.org/)