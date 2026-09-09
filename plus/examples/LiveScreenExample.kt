package portfolio.plus.examples

import androidx.compose.runtime.Composable

/**
 * MyTVOnline+ Live 화면의 상태와 사용자 흐름을 설명하기 위해 재구성한 Skeleton입니다.
 * 실제 서비스의 원본 코드가 아니며, Group / Channel / EPG 흐름만 표현합니다.
 */
interface LiveScreenController {
    fun getViewModel(): LiveViewModel
    fun selectGroup(group: Group)
    fun selectChannel(channel: Channel)
}

data class Group(val id: String, val name: String)
data class Channel(val id: String, val name: String)

data class LiveUiState(
    val selectedGroup: Group? = null,
    val selectedChannel: Channel? = null,
    val showEpg: Boolean = false,
)

class LiveViewModel {
    var uiState: LiveUiState = LiveUiState()
        private set

    fun selectGroup(group: Group) {
        uiState = uiState.copy(selectedGroup = group)
    }

    fun selectChannel(channel: Channel) {
        uiState = uiState.copy(selectedChannel = channel)
    }
}

@Composable
fun LiveScreenExample(
    state: LiveUiState,
    onGroupSelected: (Group) -> Unit,
    onChannelSelected: (Channel) -> Unit,
) {
    GroupList(state.selectedGroup, onGroupSelected)
    ChannelList(state.selectedChannel, onChannelSelected)

    if (state.showEpg) {
        GridEpg()
        EpgDetail()
    }
}

@Composable private fun GroupList(selected: Group?, onSelected: (Group) -> Unit) { /* Group UI */ }
@Composable private fun ChannelList(selected: Channel?, onSelected: (Channel) -> Unit) { /* Channel UI */ }
@Composable private fun GridEpg() { /* EPG UI */ }
@Composable private fun EpgDetail() { /* EPG detail dialog UI */ }
