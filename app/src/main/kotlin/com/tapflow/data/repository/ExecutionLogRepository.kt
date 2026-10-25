package com.tapflow.data.repository

import com.tapflow.automation.ActionResult
import com.tapflow.data.db.dao.ExecutionLogDao
import com.tapflow.data.db.entity.ExecutionLogEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository managing history of NFC tag activations and action results.
 */
class ExecutionLogRepository(
    private val logDao: ExecutionLogDao
) {

    fun getRecentLogsFlow(): Flow<List<ExecutionLogEntity>> {
        return logDao.getRecentLogsFlow()
    }

    fun getLatestLogFlow(): Flow<ExecutionLogEntity?> {
        return logDao.getLatestLogFlow()
    }

    suspend fun recordExecution(
        tagKey: String,
        tagDisplayName: String,
        automationName: String,
        durationMs: Long,
        results: List<ActionResult>
    ): Long {
        val overallSuccess = results.isNotEmpty() && results.none { it is ActionResult.Failure }

        val jsonArray = JSONArray()
        results.forEach { result ->
            val obj = JSONObject()
            obj.put("actionId", result.actionId)
            obj.put("description", result.description)
            when (result) {
                is ActionResult.Success -> {
                    obj.put("status", "SUCCESS")
                }
                is ActionResult.Failure -> {
                    obj.put("status", "FAILURE")
                    obj.put("error", result.error)
                }
                is ActionResult.Skipped -> {
                    obj.put("status", "SKIPPED")
                    obj.put("reason", result.reason)
                }
            }
            jsonArray.put(obj)
        }

        val entity = ExecutionLogEntity(
            tagKey = tagKey,
            tagDisplayName = tagDisplayName,
            automationName = automationName,
            timestamp = System.currentTimeMillis(),
            durationMs = durationMs,
            overallSuccess = overallSuccess,
            actionResultsJson = jsonArray.toString()
        )
        return logDao.insert(entity)
    }

    suspend fun clearHistory() {
        logDao.clearAll()
    }
}
