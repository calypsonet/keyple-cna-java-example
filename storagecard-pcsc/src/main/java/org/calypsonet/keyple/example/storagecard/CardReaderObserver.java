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

import static org.eclipse.keypop.reader.CardReaderEvent.Type.CARD_INSERTED;
import static org.eclipse.keypop.reader.CardReaderEvent.Type.CARD_MATCHED;

import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;
import org.eclipse.keypop.storagecard.card.StorageCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A reader Observer handles card event such as CARD_INSERTED, CARD_MATCHED, CARD_REMOVED */
class CardReaderObserver
    implements CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private static final Logger logger = LoggerFactory.getLogger(CardReaderObserver.class);
  // File identifiers
  private static final byte SFI_ENVIRONMENT_AND_HOLDER = (byte) 0x07;
  private final CardReader reader;
  private final CardSelectionManager cardSelectionManager;

  /**
   * Constructor.
   *
   * <p>Note: the reader is provided here for convenience but could also be retrieved from the
   * {@link SmartCardService} with its name and that of the plugin both present in the {@link
   * CardReaderEvent}.
   *
   * @param reader The card reader.
   * @param cardSelectionManager The card selection manager.
   */
  CardReaderObserver(CardReader reader, CardSelectionManager cardSelectionManager) {
    this.reader = reader;
    this.cardSelectionManager = cardSelectionManager;
  }

  /** {@inheritDoc} */
  @Override
  public void onReaderEvent(CardReaderEvent event) {
    switch (event.getType()) {
      case CARD_MATCHED:
        SmartCard smartCard =
            cardSelectionManager
                .parseScheduledCardSelectionsResponse(event.getScheduledCardSelectionsResponse())
                .getActiveSmartCard();

        if (smartCard instanceof CalypsoCard) {
          CalypsoCard calypsoCard = (CalypsoCard) smartCard;
          logger.info("Observer notification: Calypso card selected = {}", calypsoCard);
          logger.info(
              "Calypso Serial Number = {}",
              HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));
          logger.info("Data read during the scheduled selection process:");
          logger.info(
              "File {}h, rec 1: FILE_CONTENT = {}",
              SFI_ENVIRONMENT_AND_HOLDER,
              calypsoCard.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER));

        } else if (smartCard instanceof StorageCard) {
          StorageCard storageCard = (StorageCard) smartCard;
          logger.info(
              "Observer notification: Storage card selected, product = {}",
              storageCard.getProductType());
          byte[] block1 = storageCard.getBlock(1);
          logger.info("Block 1 content: {}", block1 != null ? HexUtil.toHex(block1) : "<not read>");
        }

        logger.info("= #### End of the card processing.");

        break;

      case CARD_INSERTED:
        logger.error(
            "CARD_INSERTED event: should not have occurred because of the MATCHED_ONLY selection mode chosen.");
        break;

      case CARD_REMOVED:
        logger.trace("There is no card inserted anymore. Return to the waiting state...");
        break;
      default:
        break;
    }

    if (event.getType() == CARD_INSERTED || event.getType() == CARD_MATCHED) {

      // Informs the underlying layer of the end of the card processing, in order to manage the
      // removal sequence.
      ((ObservableCardReader) (reader)).finalizeCardProcessing();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
    logger.error("An exception occurred in plugin '{}', reader '{}'.", pluginName, readerName, e);
  }
}
