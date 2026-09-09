package examples

/** Reconstructed and simplified Profile flow example. */
data class Profile(val id: String, val name: String, val avatar: String)

class ProfileFlowExample {
    private val profiles = mutableListOf<Profile>()
    var selectedProfile: Profile? = null
        private set

    fun add(profile: Profile) {
        profiles += profile
    }

    fun select(profileId: String) {
        selectedProfile = profiles.firstOrNull { it.id == profileId }
    }

    fun update(profile: Profile) {
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) profiles[index] = profile
        if (selectedProfile?.id == profile.id) selectedProfile = profile
    }

    fun delete(profileId: String) {
        profiles.removeAll { it.id == profileId }
        if (selectedProfile?.id == profileId) selectedProfile = null
    }
}
