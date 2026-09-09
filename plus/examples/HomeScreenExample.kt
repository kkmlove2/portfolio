package examples

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Reconstructed example for the MyTVOnline+ portfolio.
 * Simplified Home UI only; no real service/data implementation is exposed.
 */
@Composable
fun HomeScreenExample(
    onLiveClick: () -> Unit,
) {
    Column {
        Text("Banner")
        Text("Trending")
        Text("Live Recent")
        Text("Recent Content")
        Text("Notice")

        // Navigation event is intentionally represented as a callback.
        Text("Open Live")
        onLiveClick
    }
}
