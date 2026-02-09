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
package org.calypsonet.keyple.example.plugin.androidnfc

import android.app.Application
import timber.log.Timber

class DemoApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    Timber.plant(Timber.DebugTree())
  }
}
