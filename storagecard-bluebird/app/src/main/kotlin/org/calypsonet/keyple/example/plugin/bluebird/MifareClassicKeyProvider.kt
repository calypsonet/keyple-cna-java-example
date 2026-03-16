/* **************************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * Access, distribution and usage restricted to effective members of the Calypso Networks
 * Association.
 *
 * This program and the accompanying materials are made available under the terms of the
 * CNAML - (Calypso Networks Association Member License).
 *
 * SPDX-License-Identifier: LicenseRef-CNAML
 ************************************************************************************** */
package org.calypsonet.keyple.example.plugin.bluebird

import org.calypsonet.keyple.plugin.bluebird.spi.KeyProvider
import org.eclipse.keyple.core.util.HexUtil

class MifareClassicKeyProvider : KeyProvider {
  override fun getKey(keyNumber: Int): ByteArray? {
    // Returns a default key for demonstration purposes, ignoring the keyNumber.
    return HexUtil.toByteArray("FFFFFFFFFFFF")
  }
}
