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
package org.calypsonet.keyple.example.storagecard;

import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.plugin.pcsc.*;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.CalypsoCardSelectionExtension;
import org.eclipse.keypop.reader.*;
import org.eclipse.keypop.reader.selection.BasicCardSelector;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.reader.selection.IsoCardSelector;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.eclipse.keypop.storagecard.MifareClassicKeyType;
import org.eclipse.keypop.storagecard.card.ProductType;
import org.eclipse.keypop.storagecard.card.StorageCard;
import org.eclipse.keypop.storagecard.card.StorageCardSelectionExtension;
import org.eclipse.keypop.storagecard.transaction.StorageCardTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MultiTechTransaction - Comprehensive demonstration of multi-technology card handling
 *
 * <p>This class demonstrates how to:
 *
 * <ul>
 *   <li>Configure PC/SC readers for multiple card technologies
 *   <li>Set up card selection strategies for different protocols
 *   <li>Perform technology-specific operations on detected cards
 *   <li>Manage card memory operations for storage cards
 * </ul>
 *
 * <p><b>Supported Technologies:</b>
 *
 * <ul>
 *   <li><b>Calypso cards</b> (ISO 14443-4) - Complex file structures, cryptographic operations
 *   <li><b>MIFARE Ultralight</b> - Simple memory cards with block-based access
 *   <li><b>MIFARE Classic</b> - Sector-based memory cards with authentication
 *   <li><b>ST25/SRT512</b> - STMicroelectronics memory tags with block-based access
 * </ul>
 *
 * <p><b>Execution Flow:</b>
 *
 * <pre>
 * 1. Initialize PC/SC plugin and reader
 * 2. Configure protocols for each card technology
 * 3. Set up multi-protocol card selection
 * 4. Wait for card presence
 * 5. Execute selection scenario (first match wins)
 * 6. Perform technology-specific operations
 * 7. Log results and cleanup
 * </pre>
 *
 * <p><b>Prerequisites:</b>
 *
 * <ul>
 *   <li>PC/SC compatible contactless reader connected
 *   <li>Official keyple-card-cna-storagecard-java-lib from CNA (for storage cards)
 * </ul>
 *
 * @author Calypso Networks Association
 * @since 1.0.0
 */
public class MultiTechTransaction {
  private static final Logger logger = LoggerFactory.getLogger(MultiTechTransaction.class);

  // ===============================================================================================
  // CONFIGURATION CONSTANTS
  // ===============================================================================================

  /**
   * Card reader identification pattern.
   *
   * <p>This regex pattern is used to find compatible PC/SC readers. It matches:
   *
   * <ul>
   *   <li>"ASK LoGO" - Specific reader model known to work well with this demo
   *   <li>"Contactless" - Any reader with "Contactless" in its name
   * </ul>
   *
   * <p><b>Note:</b> Modify this pattern based on your available readers. Use {@code
   * plugin.getReaderNames()} to see available reader names.
   */
  private static final String READER_REGEX = ".*ASK LoGO.*|.*GEN5XX.*|.*Contactless.*";

  /**
   * Logical protocol identifiers used by the application.
   *
   * <p>These logical names map physical card protocols to application-level identifiers. Keyple
   * uses these to route card commands to appropriate protocol handlers.
   *
   * <p>The mapping is established in {@link #initializeReader()} using {@code
   * activateProtocol(physicalProtocol, logicalProtocol)}.
   */
  private static final String ISO_14443_4_LOGICAL_PROTOCOL = "ISO_14443_4"; // For Calypso cards

  private static final String MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL =
      "MIFARE_ULTRALIGHT"; // For MIFARE UL cards
  private static final String MIFARE_CLASSIC_LOGICAL_PROTOCOL =
      "MIFARE_CLASSIC"; // For MIFARE Classic cards
  private static final String ST25_SRT512_LOGICAL_PROTOCOL = "ST25_SRT512"; // For ST25 memory tags

  /**
   * Calypso Application Identifier (AID).
   *
   * <p>This AID identifies a specific Calypso application on the card. The value "A000000291FF9101"
   * corresponds to the Keyple test kit AID.
   */
  private static final String AID = "A000000291FF9101";

  /**
   * Short File Identifier (SFI) for Calypso Environment and Holder file.
   *
   * <p>SFI 0x07 typically contains:
   *
   * <ul>
   *   <li>Environment data (transport network, application version)
   *   <li>Holder information (cardholder data, validity dates)
   * </ul>
   *
   * <p>This file is commonly read during card selection for identification purposes.
   */
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;

  // ===============================================================================================
  // INSTANCE VARIABLES
  // ===============================================================================================

  private final Plugin plugin; // PC/SC plugin instance
  private final CardReader cardReader; // Configured card reader
  private final ReaderApiFactory readerApiFactory; // Factory for reader-related objects
  private final CalypsoCardApiFactory calypsoCardApiFactory; // Factory for Calypso-specific objects

  /**
   * Constructs a new instance of the MultiTechTransaction class.
   *
   * <p>This constructor initializes all necessary components for multi-technology card operations:
   *
   * <ol>
   *   <li>Retrieves the SmartCardService from the provider
   *   <li>Configures and registers the PC/SC plugin
   *   <li>Initializes and configures the card reader
   *   <li>Sets up the Calypso extension for advanced card operations
   * </ol>
   *
   * @throws RuntimeException if initialization fails (reader not found, driver issues, etc.)
   */
  public MultiTechTransaction() {
    // Step 1: Get the core smart card service
    SmartCardService service = SmartCardServiceProvider.getService();

    // Step 2: Configure and register PC/SC plugin
    PcscPluginFactory pluginFactory = configurePcscPlugin();
    this.plugin = service.registerPlugin(pluginFactory);

    // Step 3: Get reader API factory for creating selectors and managers
    this.readerApiFactory = service.getReaderApiFactory();

    // Step 4: Initialize and configure the card reader
    this.cardReader = initializeReader();

    // Step 5: Initialize Calypso extension for advanced card operations
    this.calypsoCardApiFactory = initializeCalypsoExtension();
  }

  /**
   * Configures the PC/SC plugin with default settings.
   *
   * @return Configured PC/SC plugin factory ready for registration
   */
  private PcscPluginFactory configurePcscPlugin() {
    PcscPluginFactoryBuilder.Builder pcscPluginFactoryBuilder = PcscPluginFactoryBuilder.builder();
    // Using default configuration - suitable for most use cases
    // For production, consider adding error handling and custom timeouts
    return pcscPluginFactoryBuilder.build();
  }

  /**
   * Main execution method that orchestrates the complete card interaction flow.
   *
   * <p><b>Execution Sequence:</b>
   *
   * <ol>
   *   <li>Check if a card is present in the reader
   *   <li>Execute the multi-technology card processing workflow
   * </ol>
   *
   * <p>This method handles the high-level flow while delegating specific operations to specialized
   * methods for better code organization and error handling.
   *
   * @throws InvalidCardResponseException if card returns unexpected status codes
   * @throws ReaderCommunicationException if reader communication fails
   * @throws CardCommunicationException if card communication fails
   */
  public void execute()
      throws InvalidCardResponseException,
          ReaderCommunicationException,
          CardCommunicationException {
    // Pre-condition: Ensure a card is present before attempting operations
    checkCardPresent();

    // Main processing: Handle the detected card based on its technology
    processCard();
  }

  /**
   * Processes the detected card using technology-specific operations.
   *
   * <p><b>Processing Flow:</b>
   *
   * <pre>
   * Card Selection → Technology Detection → Specific Operations
   *       ↓                    ↓                    ↓
   * Try all protocols    Analyze result type    Execute workflow
   * </pre>
   *
   * <p><b>Selection Strategy:</b> The selection manager tries each configured protocol in order
   * until one succeeds. This allows the same code to handle different card technologies
   * transparently.
   *
   * <p><b>Technology-Specific Operations:</b>
   *
   * <ul>
   *   <li><b>Calypso:</b> File-based operations, cryptographic transactions
   *   <li><b>Storage Cards:</b> Block-based memory operations, simple read/write
   * </ul>
   *
   * @throws InvalidCardResponseException if card operations fail
   * @throws ReaderCommunicationException if reader communication fails
   * @throws CardCommunicationException if card communication fails
   */
  private void processCard()
      throws InvalidCardResponseException,
          ReaderCommunicationException,
          CardCommunicationException {

    // STEP 1: Card Selection - Try all configured protocols until one succeeds
    logger.info("Starting multi-technology card selection...");
    CardSelectionManager selectionManager = prepareCardSelection();
    CardSelectionResult result = selectionManager.processCardSelectionScenario(cardReader);

    // STEP 2: Analyze selection result and determine card type
    SmartCard smartCard = result.getActiveSmartCard();
    if (smartCard == null) {
      throw new IllegalStateException(
          "Card selection failed - no supported technology detected. "
              + "Ensure card is compatible with configured protocols.");
    }

    logger.info("Card selected successfully: {}", smartCard.getClass().getSimpleName());

    // STEP 3: Execute technology-specific operations based on detected card type
    if (smartCard instanceof CalypsoCard) {
      // Calypso cards support complex file structures and cryptographic operations
      logger.info("Processing Calypso card...");
      processCalypsoCard((CalypsoCard) smartCard);
    } else if (smartCard instanceof StorageCard) {
      // Storage Cards (MIFARE Ultralight, Classic, ST25...) share a common block-based memory model
      // The API abstracts differences, allowing unified processing once access control is handled.
      logger.info("Processing Storage card...");
      processStorageCard((StorageCard) smartCard);
    } else {
      logger.warn("Unknown card type: {}", smartCard.getClass().getName());
    }
  }

  /**
   * Processes Calypso cards with file-based operations.
   *
   * <p>Calypso cards organize data in a hierarchical file system with:
   *
   * <ul>
   *   <li><b>Applications:</b> Identified by AID (Application Identifier)
   *   <li><b>Files:</b> Identified by SFI (Short File Identifier)
   *   <li><b>Records:</b> Individual data records within files
   * </ul>
   *
   * <p>This method demonstrates basic Calypso operations by reading and logging essential card
   * information including serial number and environment data.
   *
   * @param card The selected Calypso card instance
   */
  private void processCalypsoCard(CalypsoCard card) {
    logger.info("=== Calypso Card Operations ===");

    // Extract and log card identification information
    String csn = HexUtil.toHex(card.getApplicationSerialNumber());
    String sfiEnvHolder = HexUtil.toHex(SFI_ENVIRONMENT_AND_HOLDER);

    logger.info("Card details: {}", card);
    logger.info("Calypso Serial Number (CSN): {}", csn);
    logger.info(
        "Environment & Holder file (SFI {}h, record 1): {}",
        sfiEnvHolder,
        card.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER));

    // Note: In real applications, you would typically:
    // 1. Authenticate with the card using security keys
    // 2. Read/write specific application data
    // 3. Perform cryptographic operations (MAC verification, etc.)
    // 4. Handle card lifecycle operations (reload, invalidate, etc.)
  }

  /**
   * Orchestrates the processing of any Storage Card (MIFARE Ultralight, Classic, ST25, etc.).
   *
   * <p><b>Unified Workflow:</b>
   *
   * <ol>
   *   <li><b>Transaction Setup:</b> Create a unified transaction manager.
   *   <li><b>Access Control:</b> Handle technology-specific authentication (e.g., MIFARE Classic).
   *   <li><b>Data Operations:</b> Perform common read/write operations using the unified block API.
   * </ol>
   *
   * @param card The detected storage card.
   */
  private void processStorageCard(StorageCard card)
      throws InvalidCardResponseException,
          ReaderCommunicationException,
          CardCommunicationException {

    logger.info("=== Storage Card Operations ===");
    logger.info("Card Product: {}", card.getProductType());

    // 1. Transaction Setup
    StorageCardExtensionService storageCardExtensionService =
        StorageCardExtensionService.getInstance();
    StorageCardTransactionManager transaction =
        storageCardExtensionService
            .getStorageCardApiFactory()
            .createStorageCardTransactionManager(cardReader, card);

    // 2. Access Control (Technology Specific)
    handleStorageAccessControl(card, transaction);

    // 3. Data Operations (Common to all Storage Cards)
    performStorageDataOperations(card, transaction);

    logger.info("Storage card operations completed successfully.");
  }

  /**
   * Handles access control logic specific to certain storage card technologies.
   *
   * <p>While the Memory API is unified, some cards (like MIFARE Classic) require authentication
   * before sectors can be accessed.
   *
   * @param card The storage card.
   * @param transaction The active transaction manager.
   */
  private void handleStorageAccessControl(
      StorageCard card, StorageCardTransactionManager transaction) {
    if (card.getProductType().hasAuthentication()) {
      logger.info("-> Performing Access Control for MIFARE Classic...");
      // Authenticate to Sector 1 (Block 4) using Key A (Default: FF...FF)
      byte[] defaultKey =
          new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

      // The transaction manager buffers this command to be executed with the next batch
      transaction.prepareMifareClassicAuthenticate(4, MifareClassicKeyType.KEY_A, defaultKey);
      logger.info("   Authentication command added to transaction.");
    } else {
      logger.info("-> No specific access control required for this card type.");
    }
  }

  /**
   * Performs common data operations (Read/Write) applicable to all Storage Cards.
   *
   * <p><b>Abstraction Power:</b> This method works identically for MIFARE Ultralight, Classic,
   * ST25, etc. The API abstracts the underlying protocol differences.
   *
   * <p><b>Keyple Transaction Pattern (Buffering):</b> <br>
   * Keyple operations are not executed immediately. Instead, they are "prepared" (buffered) in the
   * transaction manager. The <code>processCommands()</code> method then sends all buffered commands
   * to the card in a single efficient batch (or sequentially).
   *
   * @param card The storage card (for capacity info).
   * @param transaction The transaction manager to execute commands.
   */
  private void performStorageDataOperations(
      StorageCard card, StorageCardTransactionManager transaction)
      throws InvalidCardResponseException,
          ReaderCommunicationException,
          CardCommunicationException {

    // Metadata retrieval from ProductType
    ProductType productType = card.getProductType();
    int blockCount = productType.getBlockCount();
    int blockSize = productType.getBlockSize();
    int totalCapacity = blockCount * blockSize;

    logger.info(
        "Memory Analysis: {} blocks of {} bytes each. Total capacity: {} bytes.",
        blockCount,
        blockSize,
        totalCapacity);

    // --- OPERATION 1: Memory Read ---
    logger.info("-> Reading card memory...");

    int startBlock = 0;
    int endBlock = blockCount - 1;

    // Specific logic for MIFARE Classic to focus on the authenticated sector in this demo
    if (productType == ProductType.MIFARE_CLASSIC_1K) {
      startBlock = 4;
      endBlock = 7; // Sector 1 (blocks 4 to 7)
      logger.info("   (MIFARE Classic: targeting authenticated Sector 1, blocks 4 to 7)");
    }

    transaction.prepareReadBlocks(startBlock, endBlock);
    transaction.processCommands(ChannelControl.KEEP_OPEN);

    logger.info("   Data successfully synchronized in local cache.");
    logCardMemoryContent(card);

    // --- OPERATION 2: Write / Modify Data ---
    // We target a "User Block" (Block 4 is usually safe for all supported cards)
    int targetBlock = 4;

    logger.info("-> Modifying Block {} (writing {} bytes)...", targetBlock, blockSize);
    byte[] incrementedData = incrementBlock(card, targetBlock);

    transaction.prepareWriteBlocks(targetBlock, incrementedData);
    transaction.processCommands(ChannelControl.KEEP_OPEN);
    logger.debug("   Write command confirmed by card.");

    // --- OPERATION 3: Verify and Display Final State ---
    logger.info("-> Verifying final state of Block {}...", targetBlock);
    transaction.prepareReadBlocks(targetBlock, targetBlock);
    transaction.processCommands(ChannelControl.CLOSE_AFTER);

    byte[] finalData = card.getBlock(targetBlock);
    logger.info(
        "   Block {} Content: {}", HexUtil.toHex((byte) targetBlock), HexUtil.toHex(finalData));
  }

  /**
   * Logs the complete memory content of a storage card.
   *
   * <p>This utility method iterates through all blocks of the card and displays the content of
   * those that have been read. This handles cards with different block sizes and counts (e.g.,
   * MIFARE Ultralight vs MIFARE Classic) and partial reads correctly.
   *
   * @param card The storage card to read memory from
   */
  private static void logCardMemoryContent(StorageCard card) {
    try {
      ProductType productType = card.getProductType();
      int blockCount = productType.getBlockCount();
      int blockSize = productType.getBlockSize();

      logger.info("Card Memory Dump ({} blocks of {} bytes):", blockCount, blockSize);

      for (int i = 0; i < blockCount; i++) {
        try {
          // Retrieve the block data from the card object (local cache)
          byte[] data = card.getBlock(i);

          // Only log blocks that have actually been read/populated
          // Note: getBlock() might return null or throw exception if data is not available
          if (data != null && data.length > 0) {
            logger.info("  Block {} Content: {}", HexUtil.toHex((byte) i), HexUtil.toHex(data));
          }
        } catch (Exception e) {
          // Ignore errors for blocks that haven't been read or are inaccessible
        }
      }
    } catch (Exception e) {
      logger.error("Failed to process card memory content: {}", e.getMessage());
    }
  }

  /**
   * Increments each byte value in a storage card block.
   *
   * <p>This method demonstrates block-level data manipulation by:
   *
   * <ol>
   *   <li>Reading the current block content
   *   <li>Incrementing each byte value by 1 (with overflow wrapping)
   *   <li>Returning the modified block for writing
   * </ol>
   *
   * <p><b>Byte Arithmetic:</b> Java bytes are signed (-128 to +127), so increment operations wrap
   * around:
   *
   * <ul>
   *   <li>0x7F (127) + 1 = 0x80 (-128)
   *   <li>0xFF (-1) + 1 = 0x00 (0)
   * </ul>
   *
   * <p><b>Note:</b> This is a demonstration operation. Real applications should implement proper
   * data validation and error handling.
   *
   * @param card The storage card containing the block
   * @param blockNumber The block number to increment (0-based)
   * @return New block content with incremented byte values
   */
  private byte[] incrementBlock(StorageCard card, int blockNumber) {
    // Get current block content (typically 4 bytes for most storage cards)
    byte[] block = card.getBlock(blockNumber);

    // Create a copy to avoid modifying the original card data
    byte[] incrementedBlock = new byte[block.length];
    System.arraycopy(block, 0, incrementedBlock, 0, block.length);

    // Increment each byte in the block
    // Process from end to beginning for consistent behavior
    for (int i = incrementedBlock.length - 1; i >= 0; i--) {
      incrementedBlock[i] = (byte) (incrementedBlock[i] + 1);
      // Note: Byte overflow is handled automatically by Java
      // (e.g., (byte)256 becomes (byte)0)
    }

    return incrementedBlock;
  }

  /**
   * Prepares the card selection strategy for multi-technology support.
   *
   * <p><b>Selection Manager Pattern:</b> The CardSelectionManager allows configuring multiple
   * selection strategies upfront. During execution, it tries each strategy in order until one
   * succeeds, enabling transparent handling of different card technologies with a single code path.
   *
   * <p><b>Selection Order Importance:</b> The order of selection registration affects performance:
   *
   * <ol>
   *   <li><b>Calypso (ISO 14443-4):</b> Most specific, checked first
   *   <li><b>MIFARE Ultralight:</b> Common storage cards
   *   <li><b>ST25:</b> Specialized memory tags
   * </ol>
   *
   * <p><b>Selection Strategies:</b>
   *
   * <ul>
   *   <li><b>AID-based:</b> For application-specific cards (Calypso)
   *   <li><b>Protocol-based:</b> For technology-specific cards (Storage cards)
   * </ul>
   *
   * @return Configured selection manager ready for card detection
   */
  private CardSelectionManager prepareCardSelection() {
    logger.info("Configuring multi-technology card selection...");
    CardSelectionManager manager = readerApiFactory.createCardSelectionManager();

    // ===========================================================================================
    // SELECTION STRATEGY 1: Calypso cards using AID-based selection
    // ===========================================================================================

    // Calypso cards are identified by their Application Identifier (AID)
    // This is the most specific selection method and should be tried first
    logger.debug("Configuring Calypso card selection (AID-based)...");

    IsoCardSelector isoSelector =
        readerApiFactory
            .createIsoCardSelector()
            .filterByCardProtocol(ISO_14443_4_LOGICAL_PROTOCOL) // Require ISO 14443-4 protocol
            .filterByDfName(AID); // Require specific application AID

    // Calypso selection extension allows advanced operations during selection
    CalypsoCardSelectionExtension calypsoExtension =
        calypsoCardApiFactory
            .createCalypsoCardSelectionExtension()
            .acceptInvalidatedCard() // Accept cards in any state
            .prepareReadRecord(SFI_ENVIRONMENT_AND_HOLDER, 1); // Pre-read environment data

    manager.prepareSelection(isoSelector, calypsoExtension);

    // ===========================================================================================
    // STORAGE CARD SELECTION STRATEGIES
    // ===========================================================================================
    // Strategy: We register multiple selectors. The CardSelectionManager will automatically
    // detect the card's protocol and execute the corresponding selection extension.

    // --- STRATEGY 2: MIFARE Ultralight (NFC Type 2) ---
    // If the card uses the MIFARE_ULTRALIGHT protocol, we treat it as a MIFARE_ULTRALIGHT product.
    logger.debug("Configuring MIFARE Ultralight selection (protocol-based)...");

    BasicCardSelector mifareUltralightSelector =
        readerApiFactory
            .createBasicCardSelector()
            .filterByCardProtocol(MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL);

    StorageCardSelectionExtension storageExtensionMifareUltraLight =
        StorageCardExtensionService.getInstance()
            .getStorageCardApiFactory()
            .createStorageCardSelectionExtension(ProductType.MIFARE_ULTRALIGHT)
            .prepareReadBlocks(0, ProductType.MIFARE_ULTRALIGHT.getBlockCount() - 1);

    manager.prepareSelection(mifareUltralightSelector, storageExtensionMifareUltraLight);

    // --- STRATEGY 3: MIFARE Classic (NFC Type 2*) ---
    // If the card uses the MIFARE_CLASSIC protocol, we treat it as a MIFARE_CLASSIC_1K product.
    logger.debug("Configuring MIFARE Classic 1K selection (protocol-based)...");

    BasicCardSelector mifareClassicSelector =
        readerApiFactory
            .createBasicCardSelector()
            .filterByCardProtocol(MIFARE_CLASSIC_LOGICAL_PROTOCOL);

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
            .prepareReadBlocks(0, 0); // Pre-read only block 0 (UID/Manufacturer data)
    manager.prepareSelection(mifareClassicSelector, storageExtensionMifareClassic);

    // --- STRATEGY 4: ST25/SRT512 (NFC Type 4/B) ---
    // If the card uses the ST25_SRT512 protocol, we treat it as an ST25_SRT512 product.
    logger.debug("Configuring ST25 card selection (protocol-based)...");

    BasicCardSelector st25Selector =
        readerApiFactory
            .createBasicCardSelector()
            .filterByCardProtocol(ST25_SRT512_LOGICAL_PROTOCOL);

    StorageCardSelectionExtension storageExtensionSt25 =
        StorageCardExtensionService.getInstance()
            .getStorageCardApiFactory()
            .createStorageCardSelectionExtension(ProductType.ST25_SRT512)
            .prepareReadBlocks(0, ProductType.ST25_SRT512.getBlockCount() - 1);

    manager.prepareSelection(st25Selector, storageExtensionSt25);

    logger.info("Card selection configured.");
    return manager;
  }

  /**
   * Initializes and configures the card reader for multi-technology support.
   *
   * <p><b>Reader Configuration Steps:</b>
   *
   * <ol>
   *   <li>Find a compatible reader using the configured regex pattern
   *   <li>Configure PC/SC-specific settings (contactless mode, protocol, sharing)
   *   <li>Activate protocol mappings for each supported card technology
   * </ol>
   *
   * <p><b>Understanding Protocol Mapping:</b> <br>
   * Keyple uses a "Logical Protocol" abstraction to decouple the application from physical reader
   * capabilities. Here, we map physical protocols (as defined by the PC/SC standard or reader
   * driver) to application-specific logical names. This allows the same application code to work
   * with different readers by simply adjusting these mappings, without changing the core business
   * logic.
   *
   * <p><b>Reader Settings:</b>
   *
   * <ul>
   *   <li><b>Protocol T=1:</b> Block-oriented transmission protocol (expected for contactless
   *       cards)
   * </ul>
   *
   * @return Configured card reader ready for multi-technology operations
   * @throws RuntimeException if no compatible reader is found or configuration fails
   */
  private CardReader initializeReader() {
    logger.info("Initializing card reader...");

    // Find a reader matching our criteria
    CardReader reader = plugin.findReader(READER_REGEX);
    if (reader == null) {
      throw new RuntimeException(
          "No compatible reader found. Pattern: "
              + READER_REGEX
              + ". Available readers: "
              + plugin.getReaderNames());
    }

    logger.info("Found reader: {}", reader.getName());

    // Get PC/SC plugin extension for advanced configuration
    plugin.getExtension(PcscPlugin.class);

    // Configure PC/SC reader settings
    PcscReader pcscReader = plugin.getReaderExtension(PcscReader.class, reader.getName());
    pcscReader
        .setContactless(true) // Indicates contactless reader
        .setIsoProtocol(PcscReader.IsoProtocol.T1);

    // Configure protocol mappings for each supported card technology
    ConfigurableCardReader configReader = (ConfigurableCardReader) reader;

    // Map physical PC/SC protocols to logical application protocols
    logger.debug("Activating protocol mappings...");

    // ISO 14443-4 protocol for Calypso and other ISO compliant cards
    configReader.activateProtocol(
        PcscCardCommunicationProtocol.ISO_14443_4.name(), // Physical protocol name
        ISO_14443_4_LOGICAL_PROTOCOL); // Logical protocol name

    // MIFARE Ultralight protocol for NXP storage cards
    configReader.activateProtocol(
        PcscCardCommunicationProtocol.MIFARE_ULTRALIGHT.name(), // Physical protocol name
        MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL); // Logical protocol name

    // MIFARE Classic protocol for NXP storage cards
    configReader.activateProtocol(
        PcscCardCommunicationProtocol.MIFARE_CLASSIC_1K.name(), // Physical protocol name
        MIFARE_CLASSIC_LOGICAL_PROTOCOL); // Logical protocol name

    // ST25 protocol for STMicroelectronics memory tags
    configReader.activateProtocol(
        PcscCardCommunicationProtocol.ST25_SRT512.name(), // Physical protocol name
        ST25_SRT512_LOGICAL_PROTOCOL); // Logical protocol name

    logger.info("Reader initialized successfully with {} protocols", 3);
    return reader;
  }

  /**
   * Initializes the Calypso card extension for advanced operations.
   *
   * <p><b>Extension Role:</b> The Calypso extension provides specialized APIs for:
   *
   * <ul>
   *   <li>Cryptographic operations (authentication, secure messaging)
   *   <li>File system management (DF, EF, records)
   *   <li>Transaction management (Calypso secure session)
   *   <li>Security level management (access conditions, keys)
   * </ul>
   *
   * <p><b>Extension Registration:</b> The extension must be registered with the SmartCardService to
   * enable Calypso-specific card selection and transaction capabilities.
   *
   * <p><b>API Factory:</b> The returned factory creates Calypso-specific objects like:
   *
   * <ul>
   *   <li>CalypsoCardSelectionExtension - For AID-based selection
   *   <li>CalypsoCardTransactionManager - For secure transactions
   * </ul>
   *
   * @return Configured Calypso API factory ready for card operations
   * @throws RuntimeException if extension registration fails
   */
  private CalypsoCardApiFactory initializeCalypsoExtension() {
    logger.debug("Initializing Calypso card extension...");

    // Get the singleton instance of the Calypso extension service
    CalypsoExtensionService extension = CalypsoExtensionService.getInstance();

    // Log any version inconsistencies
    SmartCardServiceProvider.getService().checkCardExtension(extension);

    // Return the API factory for creating Calypso-specific objects
    CalypsoCardApiFactory factory = extension.getCalypsoCardApiFactory();

    logger.debug("Calypso extension initialized successfully");
    return factory;
  }

  /**
   * Checks if a card is present in the reader before attempting operations.
   *
   * <p>This pre-condition check prevents unnecessary selection attempts and provides clear error
   * messages when no card is available.
   *
   * <p><b>Card Presence Detection:</b> The reader continuously monitors for card presence through:
   *
   * <p><b>Best Practice:</b> Always check card presence before selection to avoid:
   *
   * <ul>
   *   <li>Unnecessary error handling complexity
   *   <li>Confusing timeout exceptions
   *   <li>Poor user experience in interactive applications
   * </ul>
   *
   * @throws IllegalStateException if no card is present in the reader
   */
  private void checkCardPresent() {
    if (!cardReader.isCardPresent()) {
      throw new IllegalStateException(
          "No card present in reader '"
              + cardReader.getName()
              + "'. "
              + "Please place a compatible card on the reader and try again.");
    }
    logger.debug("Card presence confirmed in reader: {}", cardReader.getName());
  }

  /**
   * Application entry point demonstrating multi-technology card handling.
   *
   * <p><b>Execution Flow:</b>
   *
   * <ol>
   *   <li>Create and initialize MultiTechTransaction instance
   *   <li>Execute the complete card interaction workflow
   *   <li>Handle any errors with appropriate logging
   *   <li>Ensure clean application shutdown
   * </ol>
   *
   * <p><b>Error Handling Strategy:</b>
   *
   * <ul>
   *   <li><b>Initialization errors:</b> Configuration or hardware issues
   *   <li><b>Card operation errors:</b> Communication or protocol issues
   *   <li><b>Application errors:</b> Logic or state management issues
   * </ul>
   *
   * <p><b>Production Considerations:</b>
   *
   * <ul>
   *   <li>Add configuration file support for flexible deployment
   *   <li>Implement retry mechanisms for transient errors
   *   <li>Add metrics and monitoring for production environments
   *   <li>Consider graceful shutdown hooks for cleanup operations
   * </ul>
   *
   * @param args Command line arguments (currently unused)
   */
  public static void main(String[] args) {
    logger.info("=== MultiTechTransaction Demo Starting ===");
    logger.info("This demo supports: Calypso, MIFARE Ultralight, MIFARE Classic, ST25/SRT512");
    logger.info("Please ensure a compatible card is placed on the reader...");

    try {
      // Create and execute the multi-technology transaction
      MultiTechTransaction demo = new MultiTechTransaction();
      demo.execute();

      logger.info("=== Demo completed successfully ===");

    } catch (IllegalStateException e) {
      // Handle configuration or setup errors
      logger.error("Setup error: {}", e.getMessage());
      logger.info("Please check:");
      logger.info("1. PC/SC middleware is running");
      logger.info("2. Compatible reader is connected");
      logger.info("3. Card is properly placed on reader");

    } catch (InvalidCardResponseException e) {
      // Handle card command errors
      logger.error("Card command failed: {}", e.getMessage());
      logger.info("This may indicate:");
      logger.info("1. Card is not compatible with the operation");
      logger.info("2. Card is damaged or corrupted");
      logger.info("3. Security conditions are not met");

    } catch (ReaderCommunicationException e) {
      // Handle reader communication errors
      logger.error("Reader communication error: {}", e.getMessage());
      logger.info("Please check:");
      logger.info("1. Reader is properly connected");
      logger.info("2. PC/SC service is running");
      logger.info("3. No other application is blocking the reader");

    } catch (CardCommunicationException e) {
      // Handle card communication errors
      logger.error("Card communication error: {}", e.getMessage());
      logger.info("This may indicate:");
      logger.info("1. Card was removed during operation");
      logger.info("2. RF interference in contactless communication");
      logger.info("3. Card is not responding properly");

    } catch (Exception e) {
      // Handle unexpected errors
      logger.error("Unexpected error during execution: {}", e.getMessage(), e);
      logger.info("Please report this issue with the complete error details");

    } finally {
      // Ensure clean shutdown regardless of execution outcome
      logger.info("Application shutting down...");

      // Note: In a production environment, you might want to:
      // 1. Properly cleanup resources (close readers, unregister plugins)
      // 2. Save transaction logs or audit trails
      // 3. Send monitoring data to external systems
      // 4. Implement graceful shutdown with timeout handling
    }

    // Exit the application
    // IMPORTANT: Explicit exit is often required in PC/SC applications to ensure that
    // native resources (smart card context) are released immediately and that any
    // lingering driver threads do not prevent the JVM from terminating.
    System.exit(0);
  }
}
