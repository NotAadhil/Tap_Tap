package com.tapflow.automation

/**
 * Outcome of an individual automation action execution.
 */
sealed class ActionResult {
    abstract val actionId: Long
    abstract val description: String

    data class Success(
        override val actionId: Long = 0,
        override val description: String = "",
        val message: String = description
    ) : ActionResult() {
        constructor(message: String) : this(actionId = 0, description = message, message = message)
    }

    data class Failure(
        override val actionId: Long = 0,
        override val description: String = "",
        val error: String = description,
        val reason: String = error
    ) : ActionResult() {
        constructor(reason: String) : this(actionId = 0, description = reason, error = reason, reason = reason)
    }

    data class Skipped(
        override val actionId: Long = 0,
        override val description: String = "",
        val reason: String = description
    ) : ActionResult() {
        constructor(reason: String) : this(actionId = 0, description = "Skipped: $reason", reason = reason)
    }
}
