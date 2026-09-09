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

Group 관리 영역에서는 **관리 동작의 역할을 Interface로 분리하고, 실제 구현체에서 각 동작을 처리하는 형태**로 Skeleton을 구성했습니다.

```kotlin
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

class ManageGroupManager : ManageGroupController {

    override fun addGroup(group: Group) {
        // Add group
    }

    override fun renameGroup(
        group: Group,
        name: String
    ) {
        // Rename group
    }

    override fun deleteGroup(group: Group) {
        // Delete group
    }

    override fun moveGroup(
        group: Group,
        position: Int
    ) {
        // Reorder group
    }
}
```

**의도**

- `ManageGroupController`를 통해 Group 관리 기능의 공통 역할 정의
- Add / Rename / Delete / Reorder 책임을 명확하게 분리
- UI에서 관리 로직의 구체적인 구현보다 Interface 기반 역할에 의존할 수 있도록 구성
- 실제 서비스 코드는 공개하지 않고 담당 기능의 구조와 설계 방향만 표현

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
- 선택 Profile 상태와 화면 흐름 연결

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

# Code Skeleton

> 아래 코드는 실제 서비스 소스를 공개한 것이 아니라, **실제 담당 영역의 구조와 구현 방식을 보여주기 위해 재구성한 Skeleton**입니다.

## 1. Home — Compose UI

```kotlin
@Composable
fun HomeScreen(
    onLiveClick: () -> Unit
) {
    Column {
        Banner()
        TrendingList()
        LiveRecentList()
        RecentContentList()
        NoticeList()

        // Navigate to Live
        LiveButton(onClick = onLiveClick)
    }
}
```

**의도**

- 화면을 기능별 Composable로 분리
- 각 UI 영역의 책임을 명확하게 구성
- 화면 이벤트와 Navigation을 callback으로 연결

---

## 2. Live — UI State 연결

```kotlin
data class LiveUiState(
    val selectedGroup: Group? = null,
    val selectedChannel: Channel? = null,
    val showEpg: Boolean = false
)

@Composable
fun LiveScreen(
    state: LiveUiState,
    onGroupSelected: (Group) -> Unit,
    onChannelSelected: (Channel) -> Unit
) {
    GroupList(
        selectedGroup = state.selectedGroup,
        onGroupSelected = onGroupSelected
    )

    ChannelList(
        selectedChannel = state.selectedChannel,
        onChannelSelected = onChannelSelected
    )

    if (state.showEpg) {
        GridEpgScreen()
    }
}
```

**의도**

- UI가 직접 데이터를 관리하기보다 상태를 전달받아 화면을 구성
- Group → Channel → EPG로 이어지는 사용자 흐름을 상태로 표현
- 이벤트는 callback으로 분리하여 UI와 동작을 구분

---

## 3. ManageGroup — Interface / 구현체 분리

Group 관리 기능은 별도의 예제 파일에서도 확인할 수 있도록 분리했습니다.

```text
ManageGroupController
          │
          │ implements
          ▼
  ManageGroupManager
          │
     ┌────┼────┬──────┐
     ▼    ▼    ▼      ▼
    Add Rename Delete Reorder
```

- Interface → Group 관리 기능의 역할과 계약 정의
- Implementation → 실제 상태 변경 및 관리 동작 구현
- UI → Interface를 통해 Group 관리 동작 호출

자세한 Skeleton은 [`examples/ManageGroupControllerExample.kt`](./examples/ManageGroupControllerExample.kt)에서 확인할 수 있습니다.

---

## 4. Profile — 상태와 화면 흐름

```kotlin
class ProfileManager {

    private val profiles = mutableListOf<Profile>()

    var selectedProfile: Profile? = null
        private set

    fun add(profile: Profile) {
        profiles += profile
    }

    fun select(profile: Profile) {
        selectedProfile = profile
    }

    fun update(profile: Profile) {
        // Update profile state
    }

    fun delete(profile: Profile) {
        profiles.remove(profile)
    }
}
```

**의도**

Profile 생성 → 선택 → 수정 → 삭제의 상태를 관리하고, 선택된 Profile이 관련 화면 흐름으로 이어지도록 구성하는 구조를 보여줍니다.

---

# Architecture

### Compose UI

```text
Composable Screen
       ↓
     UI State
       ↓
User Interaction
       ↓
Navigation / State Update
```

### 기능별 Navigation

```text
App
 ├─ Home
 ├─ Live
 │   ├─ Channel
 │   └─ EPG
 ├─ Setting
 └─ Profile
     ├─ Add
     ├─ Edit
     └─ Switch
```

주요 기술:

- Jetpack Compose
- Material 3
- ViewModel
- StateFlow / Flow
- Navigation
- Adaptive UI
- Kotlin / Java

---

# 내가 보여주고 싶은 개발 역량

### UI 구현

실제 서비스 화면을 Compose 기반으로 구성하고 기능별 UI를 컴포넌트 단위로 나누어 관리했습니다.

### 상태 기반 UI

Live와 Profile에서 화면 상태와 사용자 이벤트를 연결하여 화면 흐름을 구성했습니다.

### Interface 기반 기능 구조

ManageGroup 영역에서는 Group 관리 동작을 Interface로 정의하고 구현체에서 실제 동작을 분리하는 구조를 보여줍니다.

### Navigation

기능별 화면을 분리하고 Home / Live / Setting / Profile의 사용자 흐름을 연결했습니다.

### Adaptive UI

다양한 화면 크기에서 사용할 수 있도록 화면 구성과 레이아웃을 대응시켰습니다.

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
