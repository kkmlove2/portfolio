package portfolio.plus.examples

/**
 * MyTVOnline+ Profile 기능의 역할과 사용자 흐름을 설명하기 위해 재구성한 Skeleton입니다.
 * 실제 서비스의 원본 코드가 아니며, Profile 상태와 주요 동작만 표현합니다.
 */
interface ProfileController {
    fun getViewModel(): ProfileViewModel
    fun addProfile(profile: Profile)
    fun selectProfile(profileId: String)
    fun updateProfile(profile: Profile)
    fun deleteProfile(profileId: String)
}

data class Profile(
    val id: String,
    val name: String,
    val avatar: String,
)

data class ProfileUiState(
    val profiles: List<Profile> = emptyList(),
    val selectedProfile: Profile? = null,
)

class ProfileViewModel {
    var uiState: ProfileUiState = ProfileUiState()
        private set

    fun addProfile(profile: Profile) {
        uiState = uiState.copy(profiles = uiState.profiles + profile)
    }

    fun selectProfile(profileId: String) {
        uiState = uiState.copy(
            selectedProfile = uiState.profiles.firstOrNull { it.id == profileId }
        )
    }

    fun updateProfile(profile: Profile) {
        val profiles = uiState.profiles.map { if (it.id == profile.id) profile else it }
        val selected = if (uiState.selectedProfile?.id == profile.id) profile else uiState.selectedProfile
        uiState = uiState.copy(profiles = profiles, selectedProfile = selected)
    }

    fun deleteProfile(profileId: String) {
        uiState = uiState.copy(
            profiles = uiState.profiles.filterNot { it.id == profileId },
            selectedProfile = uiState.selectedProfile?.takeUnless { it.id == profileId }
        )
    }
}
