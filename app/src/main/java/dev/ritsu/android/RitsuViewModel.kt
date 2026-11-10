package dev.ritsu.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.ritsu.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RitsuViewModel(private val gateway: CoreGateway) : ViewModel() {
    val snapshot = gateway.snapshot
    private val notifications = Channel<String>(Channel.BUFFERED)
    val notices = notifications.receiveAsFlow()

    private fun perform(command: suspend CoreGateway.() -> CommandResult) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = gateway.command()
            notifications.send(result.message)
        }
    }

    fun sendMessage(text: String) = perform { sendMessage(text) }
    fun stop() = perform { stop() }
    fun pause() = perform { pause() }
    fun resume(acknowledgeStop: Boolean = false) = perform { resume(acknowledgeStop) }
    fun grant(capability: Capability, durationMinutes: Int = 15, maxActions: Int = 3) =
        perform { grant(capability, durationMinutes, maxActions) }
    fun revoke(capability: Capability) = perform { revoke(capability) }
    fun advanceGoal(goalId: String) = perform { advanceGoal(goalId) }
    fun exploreInterest(interestId: String) = perform { exploreInterest(interestId) }
    fun refresh() = perform { refresh() }

    suspend fun refreshSilently() = gateway.refresh()

    class Factory(private val gateway: CoreGateway) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RitsuViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return RitsuViewModel(gateway) as T
        }
    }
}
