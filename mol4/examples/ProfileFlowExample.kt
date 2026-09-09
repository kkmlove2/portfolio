package examples

/** Reconstructed and simplified profile flow example. */
data class Profile(val id: String, val name: String)

class ProfileFlowExample {
    private val profiles = mutableListOf<Profile>()
    var selectedProfileId: String? = null
        private set

    fun add(profile: Profile) {
        profiles += profile
        if (selectedProfileId == null) selectedProfileId = profile.id
    }

    fun select(profileId: String) {
        if (profiles.any { it.id == profileId }) {
            selectedProfileId = profileId
        }
    }

    fun update(profile: Profile) {
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) profiles[index] = profile
    }

    fun delete(profileId: String) {
        profiles.removeAll { it.id == profileId }
        if (selectedProfileId == profileId) {
            selectedProfileId = profiles.firstOrNull()?.id
        }
    }
}
