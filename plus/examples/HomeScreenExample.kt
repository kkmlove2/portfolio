package portfolio.plus.examples

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * MyTVOnline+ Home 화면 구조를 설명하기 위해 재구성한 Skeleton입니다.
 * 실제 서비스의 원본 코드가 아니며, UI 구성과 화면 흐름만 표현합니다.
 */
interface HomeScreenController {
    fun getViewModel(): HomeViewModel
    fun onLiveClick()
}

class HomeViewModel {
    fun requestHomeData() {
        // Home data request
    }
}

@Composable
fun HomeScreenExample(
    onLiveClick: () -> Unit,
) {
    Column {
        Banner()
        Trending()
        LiveRecent()
        RecentContent()
        Notice()
        LiveButton(onClick = onLiveClick)
    }
}

@Composable private fun Banner() = Text("Banner")
@Composable private fun Trending() = Text("Trending")
@Composable private fun LiveRecent() = Text("Live Recent")
@Composable private fun RecentContent() = Text("Recent Content")
@Composable private fun Notice() = Text("Notice")
@Composable private fun LiveButton(onClick: () -> Unit) { Text("Open Live"); onClick }
