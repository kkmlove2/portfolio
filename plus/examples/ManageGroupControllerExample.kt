package portfolio.plus.examples

/**
 * MyTVOnline+의 Group 관리 흐름을 설명하기 위해 재구성한 Skeleton입니다.
 * 실제 서비스의 원본 코드가 아닙니다.
 */
interface ManageGroupController {

    fun addGroup(group: Group)

    fun renameGroup(
        group: Group,
        name: String
    )

    fun deleteGroup(group: Group)

    fun moveGroup(
        group: Group,
        position: Int
    )
}

data class Group(
    val id: String,
    val name: String
)

class ManageGroupManager : ManageGroupController {

    private val groups = mutableListOf<Group>()

    override fun addGroup(group: Group) {
        groups += group
    }

    override fun renameGroup(
        group: Group,
        name: String
    ) {
        val index = groups.indexOfFirst { it.id == group.id }
        if (index >= 0) {
            groups[index] = groups[index].copy(name = name)
        }
    }

    override fun deleteGroup(group: Group) {
        groups.removeAll { it.id == group.id }
    }

    override fun moveGroup(
        group: Group,
        position: Int
    ) {
        val index = groups.indexOfFirst { it.id == group.id }
        if (index >= 0) {
            val target = groups.removeAt(index)
                .let { it }
            groups.add(position.coerceIn(0, groups.size), target)
        }
    }
}
