/* **************************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.androidnfc.example

/**
 * Helper class to provide specific elements to handle Calypso cards.
 * * AID application selection (default Calypso AID)
 * * Files infos (SFI, rec number, etc) for
 * * Environment and Holder
 * * Event Log
 * * Contract List
 * * Contracts
 */
object CalypsoConstants {
  /** AID: Keyple test kit profile 1, Application 2 */
  const val AID = "315449432E"

  // / ** 1TIC.ICA AID */
  // public final static String AID = "315449432E494341";
  const val RECORD_NUMBER_1 = 1
  const val RECORD_NUMBER_2 = 2
  const val RECORD_NUMBER_3 = 3
  const val RECORD_NUMBER_4 = 4
  const val SFI_EnvironmentAndHolder = 0x07.toByte()
  const val SFI_EventLog = 0x08.toByte()
  const val SFI_ContractList = 0x1E.toByte()
  const val SFI_Contracts = 0x09.toByte()
  const val SFI_Counter1 = 0x19.toByte()
  const val eventLog_dataFill = "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC"

  const val RECORD_SIZE = 29
}
