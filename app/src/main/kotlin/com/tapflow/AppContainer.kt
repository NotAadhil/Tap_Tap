package com.tapflow

import android.content.Context
import com.tapflow.automation.SafeActionExecutor
import com.tapflow.data.db.AppDatabase
import com.tapflow.data.repository.TagRepository
import com.tapflow.nfc.NfcDispatchCoordinator
import com.tapflow.nfc.NfcManager

/**
 * Manual Dependency Injection container holding application-wide singletons.
 * Clean, lightweight, and debuggable without complex annotation processors.
 */
class AppContainer(context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.create(context)
    }

    val tagRepository: TagRepository by lazy {
        TagRepository(database.registeredTagDao())
    }

    val actionExecutor: SafeActionExecutor by lazy {
        SafeActionExecutor(context)
    }

    val coordinator: NfcDispatchCoordinator by lazy {
        NfcDispatchCoordinator(tagRepository, actionExecutor)
    }

    val nfcManager: NfcManager by lazy {
        NfcManager(context, coordinator)
    }
}
