package examples

import androidx.compose.runtime.Composable

/** Reconstructed example of a simplified Live screen state flow. */
data class LiveUiState(
    val selectedGroup: String? = null,
    val selectedChannel: String? = null,
    val showEpg: Boolean = false,
)

@Composable
fun LiveScreenExample(
    state: LiveUiState,
    onGroupSelected: (String) -> Unit,
    onChannelSelected: (String) -> Unit,
) {
    // Real project UI is intentionally simplified.
    // Group -> Channel -> EPG is represented by state and callbacks.
    state.selectedGroup
    state.selectedChannel
    state.showEpg
    onGroupSelected
    onChannelSelected
}
