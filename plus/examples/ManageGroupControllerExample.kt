package portfolio.plus.examples

import androidx.compose.runtime.Composable

/**
 * MyTVOnline+의 ManageGroup 구조를 설명하기 위해 재구성한 Skeleton입니다.
 * 실제 서비스의 원본 코드가 아니며, Interface와 주요 역할만 표현합니다.
 */
interface ManageGroup : TabModule {
    /** Pinned group data + all group data(include hidden groups). */
    fun getViewModel(): ManageGroupViewModel

    @Composable
    fun reqGroupGridData(
        onResponse: (ArrayList<GroupData>) -> Unit,
        onLoading: (Boolean) -> Unit,
    )

    fun getServerName(item: Any): String?
    fun setShownGroup(item: Any, isShown: Boolean)
    fun setShownGroupAll(items: ArrayList<Any>, isShown: Boolean)
    fun setPinnedGroup(item: Any, isPinned: Boolean)
    fun changePinnedGroupPosition(
        fromItem: Any,
        toItem: Any,
        fromPosition: Int,
        toPosition: Int,
    )

    fun getPinnedIndex(item: Any): Int
}

interface TabModule

class ManageGroupViewModel

data class GroupData(
    val id: String,
    val name: String,
    val isShown: Boolean = true,
    val isPinned: Boolean = false,
)

/** Simplified implementation used only to demonstrate the responsibilities. */
class ManageGroupManager : ManageGroup {
    private val viewModel = ManageGroupViewModel()

    override fun getViewModel(): ManageGroupViewModel = viewModel

    @Composable
    override fun reqGroupGridData(
        onResponse: (ArrayList<GroupData>) -> Unit,
        onLoading: (Boolean) -> Unit,
    ) {
        // Load / transform group data for the grid.
    }

    override fun getServerName(item: Any): String? = null

    override fun setShownGroup(item: Any, isShown: Boolean) {
        // Update a group's visibility.
    }

    override fun setShownGroupAll(items: ArrayList<Any>, isShown: Boolean) {
        // Update visibility for multiple groups.
    }

    override fun setPinnedGroup(item: Any, isPinned: Boolean) {
        // Update pinned state.
    }

    override fun changePinnedGroupPosition(
        fromItem: Any,
        toItem: Any,
        fromPosition: Int,
        toPosition: Int,
    ) {
        // Reorder pinned groups.
    }

    override fun getPinnedIndex(item: Any): Int = -1
}
