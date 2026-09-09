package examples

/**
 * Reconstructed example for the MOL4 portfolio.
 * Simplified representation of pin/unpin and ordering behavior.
 */
interface PinnedGroupController {
    fun pin(groupId: String)
    fun unpin(groupId: String)
    fun move(groupId: String, position: Int)
}

class PinnedGroupManager : PinnedGroupController {
    private val pinnedGroups = mutableListOf<String>()

    override fun pin(groupId: String) {
        if (groupId !in pinnedGroups) pinnedGroups += groupId
    }

    override fun unpin(groupId: String) {
        pinnedGroups.remove(groupId)
    }

    override fun move(groupId: String, position: Int) {
        pinnedGroups.remove(groupId)
        pinnedGroups.add(position.coerceIn(0, pinnedGroups.size), groupId)
    }
}
