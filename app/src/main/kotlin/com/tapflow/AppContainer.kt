package com.tapflow

import android.content.Context
import com.tapflow.automation.AutomationExecutor
import com.tapflow.automation.SafeActionExecutor
import com.tapflow.data.db.AppDatabase
import com.tapflow.data.repository.AutomationRepository
import com.tapflow.data.repository.ExecutionLogRepository
import com.tapflow.data.repository.TagRepository
import com.tapflow.nfc.NfcDispatchCoordinator
import com.tapflow.nfc.NfcManager

/**
 * Manual Dependency Injection container holding application-wide singletons.
 * Clean, lightweight, and debuggable without complex annotation processors.
 */
class AppContainer(val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.create(context)
    }

    val tagRepository: TagRepository by lazy {
        TagRepository(database.registeredTagDao())
    }

    val automationRepository: AutomationRepository by lazy {
        AutomationRepository(
            automationDao = database.automationDao(),
            actionDao = database.actionDao(),
            tagDao = database.registeredTagDao()
        )
    }

    val executionLogRepository: ExecutionLogRepository by lazy {
        ExecutionLogRepository(database.executionLogDao())
    }

    val automationExecutor: AutomationExecutor by lazy {
        AutomationExecutor(context)
    }

    val actionExecutor: SafeActionExecutor by lazy {
        SafeActionExecutor(context)
    }

    val coordinator: NfcDispatchCoordinator by lazy {
        NfcDispatchCoordinator(
            tagRepository = tagRepository,
            automationRepository = automationRepository,
            automationExecutor = automationExecutor,
            executionLogRepository = executionLogRepository,
            context = context
        )
    }

    val nfcManager: NfcManager by lazy {
        NfcManager(context, coordinator)
    }
}
