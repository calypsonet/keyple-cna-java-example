/* **************************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
 *
 * Access, distribution and usage restricted to effective members of the Calypso Networks
 * Association.
 *
 * This program and the accompanying materials are made available under the terms of the
 * CNAML - (Calypso Networks Association Member License).
 *
 * SPDX-License-Identifier: LicenseRef-CNAML
 ************************************************************************************** */
package org.calypsonet.keyple.example.storagecard;

import java.io.IOException;
import java.util.Properties;
import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.plugin.pcsc.PcscCardCommunicationProtocol;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ConfigurableCardReader;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.BasicCardSelector;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.eclipse.keypop.storagecard.MifareClassicKeyType;
import org.eclipse.keypop.storagecard.card.ProductType;
import org.eclipse.keypop.storagecard.card.StorageCardSelectionExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the multi-technology card detection example using PC/SC and a scheduled
 * selection scenario.
 *
 * <p>This class demonstrates how to configure a PC/SC contactless reader to detect and process
 * multiple card technologies in a single observation loop. The selection scenario is prepared
 * upfront and scheduled on the reader: upon card insertion, it is executed automatically and the
 * observer is notified with the result.
 *
 * <p><b>Supported Technologies:</b>
 *
 * <ul>
 *   <li><b>Calypso cards</b> (ISO 14443-4) – identified by AID; reads the Environment &amp; Holder
 *       file (SFI 07h, record 1) during selection.
 *   <li><b>MIFARE Ultralight</b> – protocol-based selection; reads block 1 during selection.
 *   <li><b>MIFARE Classic 1K</b> – protocol-based selection; authenticates sector 0 with default
 *       Key A then reads block 1 during selection.
 *   <li><b>ST25/SRT512</b> – protocol-based selection; reads block 1 during selection.
 * </ul>
 *
 * <p><b>Execution Flow:</b>
 *
 * <pre>
 * 1. Initialize PC/SC plugin and reader (activate all supported protocols)
 * 2. Prepare the scheduled multi-technology selection scenario
 * 3. Register a card reader observer and start card detection
 * 4. Wait indefinitely for card insertion events (CTRL-C to exit)
 * 5. On each insertion, the observer logs the card type and the data read during selection
 * </pre>
 *
 * <p><b>Prerequisites:</b>
 *
 * <ul>
 *   <li>PC/SC compatible contactless reader connected
 *   <li>Official keyple-card-cna-storagecard-java-lib from CNA (for storage cards)
 *   <li>{@code config.properties} on the classpath with {@code cardReader} and {@code aid} entries
 * </ul>
 *
 * @author Calypso Networks Association
 * @since 1.0.0
 */
public class Main_MultiTech_ScheduledSelection_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_MultiTech_ScheduledSelection_Pcsc.class);

  private static final Properties properties = new Properties();

  static {
    try {
      properties.load(
          Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Reader name regex read from {@code config.properties}. */
  private static final String CARD_READER_NAME_REGEX = properties.getProperty("cardReader");

  // ===============================================================================================
  // LOGICAL PROTOCOL IDENTIFIERS
  // ===============================================================================================

  /** ISO 14443-4 logical protocol – used for Calypso cards. */
  private static final String ISO_CARD_PROTOCOL = "ISO_14443_4_CARD";

  /** MIFARE Ultralight logical protocol. */
  private static final String MIFARE_ULTRALIGHT_PROTOCOL = "MIFARE_ULTRALIGHT";

  /** MIFARE Classic logical protocol. */
  private static final String MIFARE_CLASSIC_PROTOCOL = "MIFARE_CLASSIC";

  /** ST25/SRT512 logical protocol. */
  private static final String ST25_SRT512_PROTOCOL = "ST25_SRT512";

  // ===============================================================================================
  // CALYPSO CONFIGURATION
  // ===============================================================================================

  /** Calypso Application Identifier (AID), read from {@code config.properties}. */
  private static final String AID = properties.getProperty("aid");

  /**
   * Short File Identifier of the Environment &amp; Holder file.
   *
   * <p>Pre-read during selection to immediately expose cardholder and network data.
   */
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;

  // ===============================================================================================
  // SERVICE INSTANCES
  // ===============================================================================================

  /** PC/SC plugin instance. */
  private static Plugin plugin;

  /** Configured contactless card reader. */
  private static CardReader cardReader;

  /** Factory for reader-related objects (selectors, managers). */
  private static ReaderApiFactory readerApiFactory;

  /** Factory for Calypso-specific objects (selection extension, transaction managers). */
  private static CalypsoCardApiFactory calypsoCardApiFactory;

  // ===============================================================================================
  // MAIN
  // ===============================================================================================

  public static void main(String[] args) throws InterruptedException {

    logger.info("=== Multi-Technology Scheduled Selection Demo (PC/SC) ===");
    logger.info("Supported: Calypso, MIFARE Ultralight, MIFARE Classic 1K, ST25/SRT512");

    // Step 1 – Initialize services and reader
    initKeypleService();
    initCardReader();
    initCalypsoCardExtensionService();

    // Step 2 – Build the multi-technology scheduled selection scenario
    CardSelectionManager cardSelectionManager = prepareCardSelection();

    // Step 3 – Schedule the scenario on the observable reader
    // MATCHED_ONLY: the observer is notified only when at least one selection case succeeds.
    cardSelectionManager.scheduleCardSelectionScenario(
        (ObservableCardReader) cardReader, ObservableCardReader.NotificationMode.MATCHED_ONLY);

    // Step 4 – Register the observer and start detection in repeating mode
    CardReaderObserver cardReaderObserver =
        new CardReaderObserver(cardReader, cardSelectionManager);
    ((ObservableCardReader) cardReader).setReaderObservationExceptionHandler(cardReaderObserver);
    ((ObservableCardReader) cardReader).addObserver(cardReaderObserver);
    ((ObservableCardReader) cardReader)
        .startCardDetection(ObservableCardReader.DetectionMode.REPEATING);

    logger.info("Waiting for a card... (press CTRL-C to exit)");

    // Step 5 – Park the main thread; all processing happens in the observer callbacks
    synchronized (waitForEnd) {
      waitForEnd.wait();
    }

    // Cleanup
    SmartCardServiceProvider.getService().unregisterPlugin(plugin.getName());
    logger.info("Exit program.");
    System.exit(0);
  }

  // ===============================================================================================
  // INITIALIZATION
  // ===============================================================================================

  /**
   * Initializes the Keyple smart card service and registers the PC/SC plugin.
   *
   * <p>Also retrieves the {@link ReaderApiFactory} used to create selectors and managers.
   */
  private static void initKeypleService() {
    SmartCardService smartCardService = SmartCardServiceProvider.getService();
    plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());
    readerApiFactory = smartCardService.getReaderApiFactory();
  }

  /**
   * Finds, configures, and prepares the contactless PC/SC reader.
   *
   * <p>Activates all four protocol mappings required to detect the supported card technologies:
   *
   * <ul>
   *   <li>ISO 14443-4 → {@value #ISO_CARD_PROTOCOL}
   *   <li>MIFARE Ultralight → {@value #MIFARE_ULTRALIGHT_PROTOCOL}
   *   <li>MIFARE Classic 1K → {@value #MIFARE_CLASSIC_PROTOCOL}
   *   <li>ST25/SRT512 → {@value #ST25_SRT512_PROTOCOL}
   * </ul>
   */
  private static void initCardReader() {
    cardReader = plugin.findReader(CARD_READER_NAME_REGEX);

    plugin
        .getReaderExtension(PcscReader.class, cardReader.getName())
        .setContactless(true)
        .setIsoProtocol(PcscReader.IsoProtocol.T1)
        .setDisconnectionMode(PcscReader.DisconnectionMode.UNPOWER)
        .setSharingMode(PcscReader.SharingMode.SHARED);

    ConfigurableCardReader configReader = (ConfigurableCardReader) cardReader;
    configReader.activateProtocol(
        PcscCardCommunicationProtocol.ISO_14443_4.name(), ISO_CARD_PROTOCOL);
    configReader.activateProtocol(
        PcscCardCommunicationProtocol.MIFARE_ULTRALIGHT.name(), MIFARE_ULTRALIGHT_PROTOCOL);
    configReader.activateProtocol(
        PcscCardCommunicationProtocol.MIFARE_CLASSIC_1K.name(), MIFARE_CLASSIC_PROTOCOL);
    configReader.activateProtocol(
        PcscCardCommunicationProtocol.ST25_SRT512.name(), ST25_SRT512_PROTOCOL);
  }

  /**
   * Initializes the Calypso card extension service and retrieves its API factory.
   *
   * <p>The factory is used later to create the {@link CalypsoCardSelectionExtension}.
   */
  private static void initCalypsoCardExtensionService() {
    CalypsoExtensionService calypsoExtensionService = CalypsoExtensionService.getInstance();
    SmartCardServiceProvider.getService().checkCardExtension(calypsoExtensionService);
    calypsoCardApiFactory = calypsoExtensionService.getCalypsoCardApiFactory();
  }

  // ===============================================================================================
  // CARD SELECTION
  // ===============================================================================================

  /**
   * Builds the multi-technology scheduled selection scenario.
   *
   * <p>Each selection case targets one card technology. The {@link CardSelectionManager} tries them
   * in registration order; the first match wins. Data read during selection is available
   * immediately in the observer callback without an extra card exchange.
   *
   * <p><b>Selection order:</b>
   *
   * <ol>
   *   <li>Calypso (AID-based, ISO 14443-4) – most specific, checked first
   *   <li>MIFARE Ultralight (protocol-based)
   *   <li>MIFARE Classic 1K (protocol-based, requires sector 0 authentication)
   *   <li>ST25/SRT512 (protocol-based)
   * </ol>
   *
   * @return a configured {@link CardSelectionManager} ready to be scheduled on the reader
   */
  private static CardSelectionManager prepareCardSelection() {
    CardSelectionManager manager = readerApiFactory.createCardSelectionManager();

    // --- Case 1: Calypso card (AID-based, ISO 14443-4) ---
    // Filters on both protocol and AID; pre-reads record 1 of the Environment & Holder file.
    IsoCardSelector calypsoSelector =
        readerApiFactory
            .createIsoCardSelector()
            .filterByCardProtocol(ISO_CARD_PROTOCOL)
            .filterByDfName(AID);
    CalypsoCardSelectionExtension calypsoExtension =
        calypsoCardApiFactory
            .createCalypsoCardSelectionExtension()
            .acceptInvalidatedCard()
            .prepareReadRecord(SFI_ENVIRONMENT_AND_HOLDER, 1);
    manager.prepareSelection(calypsoSelector, calypsoExtension);

    // --- Case 2: MIFARE Ultralight (protocol-based) ---
    // Pre-reads block 1 during selection.
    BasicCardSelector mifareUltralightSelector =
        readerApiFactory.createBasicCardSelector().filterByCardProtocol(MIFARE_ULTRALIGHT_PROTOCOL);
    StorageCardSelectionExtension storageExtensionMifareUltraLight =
        StorageCardExtensionService.getInstance()
            .getStorageCardApiFactory()
            .createStorageCardSelectionExtension(ProductType.MIFARE_ULTRALIGHT)
            .prepareReadBlocks(1, 1);
    manager.prepareSelection(mifareUltralightSelector, storageExtensionMifareUltraLight);

    // --- Case 3: MIFARE Classic 1K (protocol-based) ---
    // Block 1 belongs to sector 0 (blocks 0–3); authenticates sector 0 with default Key A
    // (FF:FF:FF:FF:FF:FF) before reading block 1.
    BasicCardSelector mifareClassicSelector =
        readerApiFactory.createBasicCardSelector().filterByCardProtocol(MIFARE_CLASSIC_PROTOCOL);
    StorageCardSelectionExtension storageExtensionMifareClassic =
        StorageCardExtensionService.getInstance()
            .getStorageCardApiFactory()
            .createStorageCardSelectionExtension(ProductType.MIFARE_CLASSIC_1K)
            .prepareMifareClassicAuthenticate(
                0,
                MifareClassicKeyType.KEY_A,
                new byte[] {
                  (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
                })
            .prepareReadBlocks(1, 1);
    manager.prepareSelection(mifareClassicSelector, storageExtensionMifareClassic);

    // --- Case 4: ST25/SRT512 (protocol-based) ---
    // Pre-reads block 1 during selection.
    BasicCardSelector st25Selector =
        readerApiFactory.createBasicCardSelector().filterByCardProtocol(ST25_SRT512_PROTOCOL);
    StorageCardSelectionExtension storageExtensionSt25 =
        StorageCardExtensionService.getInstance()
            .getStorageCardApiFactory()
            .createStorageCardSelectionExtension(ProductType.ST25_SRT512)
            .prepareReadBlocks(1, 1);
    manager.prepareSelection(st25Selector, storageExtensionSt25);

    return manager;
  }

  // ===============================================================================================
  // SYNCHRONIZATION
  // ===============================================================================================

  /**
   * Used to park the main thread while card events are handled by the observer.
   *
   * <p>A call to {@code notify()} on this object would unblock the main thread and terminate the
   * program (not demonstrated here; use CTRL-C instead).
   */
  private static final Object waitForEnd = new Object();
}
