# MyTVOnline+ — Compose UI / Live / Profile Portfolio

> Android 애플리케이션에서 **Home UI, Live, Setting UI, Profile**을 중심으로 화면과 사용자 흐름을 구현한 프로젝트입니다.

---

## 프로젝트 소개

MyTVOnline+에서는 **Jetpack Compose 기반 UI 구현과 화면 간 사용자 흐름 구성**을 중심으로 작업했습니다.

- Home → UI 구현 중심
- Live → Channel / Group / EPG 탐색 UI
- Setting → UI 구현 중심
- Profile → `profile/` 폴더 내 구현

VOD, Search, TV Series, Player 및 Member / Account 관련 기능은 담당 범위에서 제외했습니다.

---

# 담당 범위

## 1. Home — UI 구현

Home 메인 화면에서 사용자가 보는 콘텐츠 영역과 화면 구성을 구현했습니다.

- Banner UI
- Trending UI
- Live Recent UI
- Recent Content UI
- Notice UI
- Compose 기반 UI
- Adaptive UI
- Home Navigation UI 흐름

```text
Home
 └─ Dashboard
     ├─ Banner
     ├─ Trending
     ├─ Live Recent
     ├─ Recent Content
     └─ Notice
```

> Home은 **UI 구현 중심**으로 기술하며 데이터 처리나 서비스 로직을 주요 담당으로 주장하지 않습니다.

---

# 2. Live

Live에서는 채널과 Group을 탐색하고 EPG 정보를 확인할 수 있는 화면을 구현했습니다.

- Live Navigation
- Channel List
- Group / Favorite Group UI
- EPG List
- Grid EPG
- EPG Detail Dialog
- Channel Logo UI
- Live History 관련 화면
- Sport Mode 관련 UI / 상태

Player 자체 구현은 담당 범위에서 제외했습니다.

### 화면 흐름

```text
Live
  ↓
Group 선택
  ↓
Channel List
  ↓
EPG 확인
  ↓
Program Detail
```

### ManageGroup — Interface 기반 구조

Group 관리 영역에서는 Interface를 통해 ViewModel, Group 데이터 조회, 표시 여부, Pinned 상태 및 순서 변경과 같은 기능의 역할을 정의했습니다.

```kotlin
interface ManageGroup : TabModule {

    /**
     * Pinned group data + all group data(include hidden groups).
     */
    fun getViewModel(): ManageGroupViewModel

    @Composable
    fun ReqGroupGridData(
        onResponse: (ArrayList<GroupData>) -> Unit,
        onLoading: (Boolean) -> Unit
    )

    fun getServerName(item: Any): String?

    fun setShownGroup(
        item: Any,
        isShown: Boolean
    )

    fun setShownGroupAll(
        items: ArrayList<Any>,
        isShown: Boolean
    )

    fun setPinnedGroup(
        item: Any,
        isPinned: Boolean
    )

    fun changePinnedGroupPosition(
        fromItem: Any,
        toItem: Any,
        fromPosition: Int,
        toPosition: Int
    )

    fun getPinnedIndex(item: Any): Int
}
```

**의도**

- `ManageGroup` → Group 관리 기능의 역할 정의
- `getViewModel()` → ViewModel과 기능 영역 연결
- `ReqGroupGridData()` → Compose UI에서 Group Grid 데이터를 요청하고 Loading / Response를 callback으로 전달
- `setShownGroup()` / `setShownGroupAll()` → Group 표시 여부 관리
- `setPinnedGroup()` → Pinned Group 상태 관리
- `changePinnedGroupPosition()` → Pinned Group 순서 변경
- `getPinnedIndex()` → Pinned 상태에서 현재 위치 확인

> 위 Interface는 실제 서비스 코드의 구조를 바탕으로 포트폴리오용으로 공개한 예시이며, 실제 구현부와 서비스 고유 로직은 포함하지 않습니다.

---

# 3. Setting — UI 구현

Setting에서는 설정 화면과 세부 화면의 UI를 구현했습니다.

- EPG Data UI
- Audio / Subtitle UI
- Subtitle Appearance UI
- App Settings UI
- About Dialog UI
- Setting Navigation

> 설정 데이터 처리나 백엔드 기능 자체를 주요 담당으로 기술하지 않고 **화면 UI 구현**에 초점을 맞췄습니다.

---

# 4. Profile

Profile은 **`profile/` 폴더 내 구현만** 담당 범위로 정리했습니다.

- Profile 생성
- Name 입력
- Avatar 선택
- Profile 수정
- Profile Settings
- PIN / Protection UI
- Sensitive Categories
- Profile 전환
- Profile 삭제
- Profile Navigation
- 선택 Profile에 따른 화면 흐름 연결

### Profile 흐름

```text
Profile
  │
  ├─ Add Profile
  │    ├─ Avatar
  │    └─ Name
  │
  ├─ Edit Profile
  │    ├─ Avatar
  │    ├─ Name
  │    └─ PIN / Protection
  │
  ├─ Switch Profile
  │
  └─ Delete Profile
```

---

# 🏗️ Architecture & Implementation

## 1. Home — Compose UI 구조

Home은 기능별 UI를 Composable 단위로 분리하고, 사용자 이벤트는 callback을 통해 상위 화면 흐름으로 전달하는 구조로 구성했습니다.

```kotlin
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
fun HomeScreen(
    onLiveClick: () -> Unit
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
```

**의도**

- Home 화면의 기능별 UI를 Composable로 분리
- UI와 화면 이벤트를 callback으로 연결
- Home → Live Navigation 흐름을 UI 이벤트와 분리
- 실제 데이터 처리 및 서비스 로직은 공개하지 않고 UI 구조만 표현

---

## 2. Live — 사용자 선택 / 화면 흐름

Live는 Group → Channel → EPG로 이어지는 사용자 흐름을 이벤트와 화면 구성으로 표현했습니다.

```kotlin
interface LiveScreenController {
    fun getViewModel(): LiveViewModel
    fun selectGroup(group: Group)
    fun selectChannel(channel: Channel)
}

class LiveViewModel {
    private var selectedGroup: Group? = null
    private var selectedChannel: Channel? = null

    fun selectGroup(group: Group) {
        selectedGroup = group
    }

    fun selectChannel(channel: Channel) {
        selectedChannel = channel
    }
}

@Composable
fun LiveScreen(
    selectedGroup: Group?,
    selectedChannel: Channel?,
    onGroupSelected: (Group) -> Unit,
    onChannelSelected: (Channel) -> Unit
) {
    GroupList(selectedGroup, onGroupSelected)
    ChannelList(selectedChannel, onChannelSelected)
    GridEpg()
    EpgDetail()
}
```

**의도**

- Group → Channel → EPG 탐색 흐름을 표현
- 사용자 선택과 화면 동작의 역할을 분리
- Compose UI는 필요한 값을 전달받아 화면을 구성
- Android TV / 다양한 화면 크기에서의 탐색 UX를 고려

> 실제 프로젝트에서 사용하지 않은 `UiState` 패턴을 임의로 추가하지 않고, 포트폴리오에서는 실제 담당 영역에서 확인할 수 있는 ViewModel / callback / Compose 구조를 중심으로 표현합니다.

---

## 3. ManageGroup — Interface 기반 구조

ManageGroup에서는 실제 코드에서 사용한 Interface 구조를 중심으로 Group 관리 기능의 역할을 표현했습니다.

```kotlin
interface ManageGroup : TabModule {

    fun getViewModel(): ManageGroupViewModel

    @Composable
    fun ReqGroupGridData(
        onResponse: (ArrayList<GroupData>) -> Unit,
        onLoading: (Boolean) -> Unit
    )

    fun getServerName(item: Any): String?
    fun setShownGroup(item: Any, isShown: Boolean)
    fun setShownGroupAll(items: ArrayList<Any>, isShown: Boolean)
    fun setPinnedGroup(item: Any, isPinned: Boolean)

    fun changePinnedGroupPosition(
        fromItem: Any,
        toItem: Any,
        fromPosition: Int,
        toPosition: Int
    )

    fun getPinnedIndex(item: Any): Int
}
```

**의도**

- Interface를 통해 Group 관리 영역의 역할과 책임을 정의
- ViewModel과 Compose UI의 연결 구조 표현
- Group Show / Hide 관리
- Pinned Group 관리
- Pinned Group 순서 변경 및 위치 확인
- 실제 서비스의 내부 구현은 공개하지 않고 Interface 수준의 설계만 표현

---

## 4. Profile — 사용자 흐름

Profile은 생성 → 선택 → 수정 → 삭제의 사용자 흐름을 ViewModel의 기능으로 표현했습니다.

```kotlin
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
    val avatar: String
)

class ProfileViewModel {
    private val profiles = mutableListOf<Profile>()
    private var selectedProfileId: String? = null

    fun addProfile(profile: Profile) {
        profiles += profile
        if (selectedProfileId == null) {
            selectedProfileId = profile.id
        }
    }

    fun selectProfile(profileId: String) {
        if (profiles.any { it.id == profileId }) {
            selectedProfileId = profileId
        }
    }

    fun updateProfile(profile: Profile) {
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            profiles[index] = profile
        }
    }

    fun deleteProfile(profileId: String) {
        profiles.removeAll { it.id == profileId }

        if (selectedProfileId == profileId) {
            selectedProfileId = profiles.firstOrNull()?.id
        }
    }
}
```

**의도**

- Profile 생성 / 선택 / 수정 / 삭제 책임을 기능별로 분리
- 선택 Profile 변경에 따른 사용자 흐름을 표현
- 실제 서비스의 저장소 및 인증/보안 구현은 공개하지 않음

---

# 🔄 UI Flow

```text
                         MyTVOnline+
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
        Home                Live               Setting
          │                   │
          │                   ├─ Group
          │                   ├─ Channel
          │                   └─ EPG
          │
          └──────────────► Navigation
                              │
                              ▼
                           Profile
                              │
                 ┌────────────┼────────────┐
                 ▼            ▼            ▼
                Add         Edit        Switch
                 │            │            │
                 └────────────┴────────────┘
                              │
                         화면 / 데이터 반영
```

---

# 내가 보여주고 싶은 개발 역량

### UI 구현

실제 서비스 화면을 Compose 기반으로 구성하고 기능별 UI를 컴포넌트 단위로 나누어 관리했습니다.

### 사용자 흐름 구현

Live와 Profile에서 사용자 선택과 이벤트를 연결하여 화면 흐름을 구성했습니다.

### Interface 기반 기능 구조

ManageGroup 영역에서는 실제 기능에 필요한 역할을 Interface로 정의하고 ViewModel 및 UI와 연결되는 구조를 구성했습니다.

### Navigation

기능별 화면을 분리하고 Home / Live / Setting / Profile의 사용자 흐름을 연결했습니다.

### Adaptive UI

다양한 화면 크기에서 사용할 수 있도록 화면 구성과 레이아웃을 대응시켰습니다.

---

# 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin / Java |
| Platform | Android |
| UI | Jetpack Compose / Material 3 |
| Architecture | ViewModel / Navigation |
| Design | Interface / Callback / Component Reusability |
| Key Point | UI Implementation / User Flow / Abstraction |

---

# 담당 범위 외

아래 기능은 담당 범위가 아니므로 포트폴리오의 구현 내용에서 제외했습니다.

- VOD
- Search
- TV Series
- Player
- Member / Account
- `member/` 패키지
- `UserMgr`
- 기타 담당하지 않은 기능 및 모듈

> Profile은 **`profile/` 폴더 내 구현만** 담당 범위로 포함했습니다.
