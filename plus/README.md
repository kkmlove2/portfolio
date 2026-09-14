# MyTVOnline+ — Compose UI / Live / Profile Portfolio

> Android 애플리케이션에서 **Home UI, Live, Setting UI, Profile**을 중심으로 화면과 사용자 흐름을 구현한 프로젝트입니다.

---

## 프로젝트 소개

MyTVOnline+에서는 **Jetpack Compose 기반 UI 구현과 화면 간 사용자 흐름 구성**을 중심으로 작업했습니다.

- Home → UI 구현 중심
- Live → Channel / Group / EPG 탐색 UI
- Setting → UI 구현 중심
- Profile → `profile/` 폴더 내 구현
- InAppPip → Live 화면에서 사용하는 Mini Player UI / 동작 구현

VOD, Search, TV Series, Player 자체 구현 및 Member / Account 관련 기능은 담당 범위에서 제외했습니다.

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
- InAppPip Mini Player UI / 사용자 조작

Player 자체 구현은 담당 범위에서 제외했으며, **Live 화면에서 동작하는 InAppPip 기능은 별도 기능으로 정리했습니다.**

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
  ↓
InAppPip
  ├─ Move / Resize
  ├─ Corner Snap
  └─ Full Screen / Play / Mute / Close
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

## InAppPip — Live 화면 내 Mini Player

InAppPip는 Live 화면 위에서 영상을 작은 Player 형태로 유지하면서 다른 UI를 탐색할 수 있도록 만든 **In-App Picture-in-Picture 형태의 UI 기능**입니다.

실제 코드에서는 단순히 작은 Player를 표시하는 것에 그치지 않고, **크기 / 위치 / 화면 크기 변화 / Drag / Fling / Corner Snap / Animation / Control UI**를 하나의 기능으로 관리했습니다.

### 주요 기능

- InAppPip Enable / Disable
- 16:9 영상 비율 유지
- 최소 Player 크기 계산
- 화면 크기 변화에 따른 위치 재계산
- Drag를 통한 위치 이동
- Fling 방향에 따른 Corner Snap
- 세로 / 가로 Fling threshold 분리
- Zoom / Resize
- Bottom Navigation 영역을 고려한 최대 위치 계산
- Enable / Disable Animation
- Play / Pause / Mute / Full Screen / Close Control UI

### InAppPip 상태 구조

`InAppPip`에서는 실제 코드에서 `MutableStateFlow`를 이용해 화면에 필요한 상태를 관리합니다.

```kotlin
class InAppPip(
    density: Density,
    displayShorterSide: Dp,
    bottomBarHeight: Int
) {
    private val _screen: MutableStateFlow<Size> =
        MutableStateFlow(Size(0f, 0f))
    val screen = _screen.asStateFlow()

    private val _scale: MutableStateFlow<Float> =
        MutableStateFlow(1f)
    val scale = _scale.asStateFlow()

    private val _enabled: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()

    fun enable(rate: Float = 1f) {
        _enabled.value = true
        animate(true, rate)
    }

    fun disable(withAnim: Boolean, rate: Float = 1f) {
        _enabled.value = false

        if (withAnim) {
            animate(false, rate)
        } else {
            disableImmediately()
        }
    }
}
```

### 위치 상태와 Drag / Fling 처리

PIP 위치는 별도의 `Position` Interface와 내부 구현체에서 `StateFlow`로 노출하고, 이동과 Corner Snap을 분리해서 처리합니다.

```kotlin
interface Position {
    val x: StateFlow<Float>
    val y: StateFlow<Float>

    fun addY(y: Float)
    fun setY(y: Float)
}
```

사용자가 Drag하면 화면 영역 안에서 위치를 제한하고, Fling이 발생하면 속도에 따라 가까운 Corner로 이동합니다.

```kotlin
fun move(offset: Offset) {
    val maxPosition = getMaxPositionVariable()

    _position.set(
        x = getSafePosition(
            _position.x.value,
            offset.x,
            pipMarginPx,
            maxPosition.x
        ),
        y = getSafePosition(
            _position.y.value,
            offset.y,
            pipMarginPx,
            maxPosition.y
        )
    )
}
```

```text
Drag
  ↓
현재 위치 + Offset
  ↓
Container 영역 내 위치 보정
  ↓
PIP 위치 변경

Fling
  ↓
Velocity 분석
  ↓
Horizontal / Vertical 방향 판단
  ↓
가까운 Corner 계산
  ↓
Animation으로 Corner Snap
```

특히 실제 코드에서는 세로 방향으로 빠르게 Drag하는 상황에서도 X velocity가 함께 크게 들어올 수 있는 문제를 고려하여, **Y velocity가 큰 경우 X 방향 threshold를 별도로 높이는 방식**으로 Corner Snap 동작을 보정했습니다.

### 화면 크기 / 비율 대응

PIP 크기는 16:9 비율을 기준으로 계산하며, 화면의 짧은 변과 Container 크기, Bottom Navigation 높이를 함께 고려하여 최소 크기와 최대 위치를 계산합니다.

```kotlin
companion object {
    const val PIP_MARGIN_DP = 10

    fun toPlayerViewHeight(width: Float): Float =
        width / 16 * 9

    fun toPlayerViewWidth(height: Float): Float =
        height * 16 / 9
}
```

Container 크기가 변경되는 경우에는 기존 PIP 위치를 비율로 변환한 뒤 새로운 화면 크기에 맞춰 다시 위치를 계산합니다.

```text
기존 Container Size
       ↓
현재 PIP 위치를 비율로 계산
       ↓
Container Size 변경
       ↓
새로운 최대 위치 계산
       ↓
기존 위치 비율 유지
       ↓
PIP 위치 재배치
```

이를 통해 Tablet Portrait / Landscape 전환이나 화면 크기 변화와 같은 상황에서도 PIP가 화면 밖으로 벗어나지 않도록 처리했습니다.

### Animation

InAppPip의 Enable / Disable 및 Corner 이동에는 별도의 `Animator`를 사용했습니다.

`Animator`에서는 위치뿐 아니라 Player의 Width / Height까지 함께 애니메이션하여 **PIP 진입 / 종료 시 크기와 위치가 동시에 자연스럽게 변경**되도록 구성했습니다.

```kotlin
fun start(
    fromPosition: Offset,
    fromSize: SizeF,
    toPosition: Offset,
    toSize: SizeF,
    rate: Float,
    update: (x: Float, y: Float, width: Float, height: Float) -> Unit,
    onEnd: () -> Unit,
) {
    setValues(
        PropertyValuesHolder.ofFloat(X, fromPosition.x, toPosition.x),
        PropertyValuesHolder.ofFloat(Y, fromPosition.y, toPosition.y),
        PropertyValuesHolder.ofFloat(WIDTH, fromSize.width, toSize.width),
        PropertyValuesHolder.ofFloat(HEIGHT, fromSize.height, toSize.height)
    )

    duration = (ANIM_DURATION.toFloat() * rate).toLong()
    // ...
}
```

### InAppPip Control UI

Control UI는 Compose로 구성하고, PIP 위에서 필요한 사용자 동작을 callback으로 전달하도록 구현했습니다.

```kotlin
@Composable
fun InAppPipControlUiScreen(
    playButtonState: PipPlayBtnState,
    isMuted: Boolean,
    onFullScreen: () -> Unit,
    onPlayClick: () -> Unit,
    onMuteClick: () -> Unit,
    onClose: () -> Unit
) {
    // Full Screen / Play / Mute / Close
}
```

- PIP 화면 Tap → Full Screen
- Close → PIP 종료
- Play / Pause → 재생 상태에 따른 버튼 표시
- Mute → 음소거 상태에 따른 아이콘 변경

### InAppPip 구조

```text
                    InAppPip
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
       Position       Screen        Scale
       StateFlow      StateFlow     StateFlow
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                  PIP Layout/UI
                        │
             ┌──────────┼──────────┐
             ▼          ▼          ▼
           Drag       Resize     Fling
                        │          │
                        └────┬─────┘
                             ▼
                       Corner Snap
                             │
                             ▼
                          Animator
```

> InAppPip는 실제 `plus/live/player/inapppip/` 코드의 구조를 바탕으로 정리했으며, Player 자체 구현이나 서비스 고유 로직은 공개하지 않고 PIP 기능의 UI / 상태 / 사용자 입력 / 위치 계산 구조를 중심으로 표현했습니다.

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

## 2. Live — 실제 StateFlow 기반 상태 전달

실제 MyTVOnline+ 코드에서는 `MutableStateFlow`를 이용해 Live 화면에서 변경되는 데이터를 보관하고, 외부에는 `StateFlow` / `asStateFlow()` 형태로 노출하는 구조를 사용했습니다.

대표적으로 `LiveData`에서 현재 Group과 Playback 데이터를 상태로 관리합니다.

```kotlin
class LiveData {
    private val _group: MutableStateFlow<Group?> = MutableStateFlow(null)
    val group = _group.asStateFlow()

    private val _playback: MutableStateFlow<PlaybackData?> = MutableStateFlow(null)
    val playback = _playback.asStateFlow()

    fun setGroup(group: Group) {
        _group.value = group
    }

    fun setPlaybackData(playbackData: PlaybackData) {
        _playback.value = playbackData
    }

    fun clear() {
        _group.value = null
        _playback.value = null
    }
}
```

이를 통해 Group이나 Playback 데이터가 변경될 때 이를 관찰하는 쪽에서 최신 상태를 받을 수 있도록 구성했습니다.

---

## 3. Live — 사용자 선택 / 화면 흐름

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

> `UiState`라는 별도 패턴을 임의로 추가하지 않고, 실제 코드에서 사용한 `MutableStateFlow` / `StateFlow`와 callback / Compose 구조를 중심으로 표현합니다.

---

## 4. ManageGroup — Interface 기반 구조

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

## 5. Profile — 실제 StateFlow 기반 상태 관리

Profile 영역에서도 실제 코드에서 `MutableStateFlow`를 사용하여 선택된 Profile과 Profile 이름을 상태로 관리했습니다.

```kotlin
private val _selectedProfileFlow = MutableStateFlow(profileMgr.getProfile())
val selectedProfileFlow: StateFlow<ProfileBox?> =
    _selectedProfileFlow.asStateFlow()

private val _profileNameFlow = MutableStateFlow(
    profileMgr.getProfile()?.name ?: ""
)
val profileNameFlow: StateFlow<String> =
    _profileNameFlow.asStateFlow()
```

Profile이 선택되거나 수정되면 내부 `MutableStateFlow` 값을 변경합니다.

```kotlin
fun updateProfile(profile: ProfileBox) {
    profileMgr.updateProfile(profile)
    _selectedProfileFlow.value = profile
    _profileNameFlow.value = profile.name
}
```

Profile 선택 과정에서도 실제 선택 결과를 Flow에 반영하여 관련 화면에서 상태를 관찰할 수 있도록 구성했습니다.

### Profile 사용자 흐름

```text
Profile
  ↓
Add / Select / Edit / Delete
  ↓
Profile 상태 변경
  ↓
MutableStateFlow 값 변경
  ↓
StateFlow를 관찰하는 화면에 반영
```

> 실제 서비스의 Profile 저장 구조나 계정 관련 고유 로직은 공개하지 않고, 실제 코드에서 확인되는 상태 관리 구조만 포트폴리오에 표현했습니다.

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
          │                   ├─ EPG
          │                   └─ InAppPip
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
                         화면 / 상태 반영
```

---

# 내가 보여주고 싶은 개발 역량

### UI 구현

실제 서비스 화면을 Compose 기반으로 구성하고 기능별 UI를 컴포넌트 단위로 나누어 관리했습니다.

### Reactive State 관리

실제 코드에서 `MutableStateFlow`와 `StateFlow`를 활용하여 Live / Profile / InAppPip 등 화면에 필요한 상태를 관리하고 변경 사항을 관찰할 수 있도록 구성했습니다.

### 사용자 흐름 구현

Live와 Profile에서 사용자 선택과 이벤트를 연결하고, InAppPip에서는 Drag / Fling / Full Screen / Close 등의 사용자 동작을 연결하여 화면 흐름을 구성했습니다.

### Interface 기반 기능 구조

ManageGroup 영역에서는 실제 기능에 필요한 역할을 Interface로 정의하고 ViewModel 및 UI와 연결되는 구조를 구성했습니다.

### UI Interaction

InAppPip에서 Drag, Fling, Resize, Corner Snap, Animation을 조합하여 작은 화면에서도 자연스럽게 조작할 수 있는 UI Interaction을 구현했습니다.

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
| State | MutableStateFlow / StateFlow / Flow |
| Async | Kotlin Coroutines |
| UI Interaction | Drag / Fling / Resize / Animation |
| Design | Interface / Callback / Component Reusability |
| Key Point | UI Implementation / User Flow / State Management / UI Interaction / Abstraction |

---

# 담당 범위 외

아래 기능은 담당 범위가 아니므로 포트폴리오의 구현 내용에서 제외했습니다.

- VOD
- Search
- TV Series
- Player 자체 구현
- Member / Account
- `member/` 패키지
- `UserMgr`
- 기타 담당하지 않은 기능 및 모듈

> Profile은 **`profile/` 폴더 내 구현만** 포함하며, InAppPip는 Player 전체 구현이 아닌 **`plus/live/player/inapppip/` 영역의 기능 구조와 UI / Interaction 구현**을 별도로 정리했습니다.

---

# 📌 Portfolio Note

실제 서비스 소스 전체를 공개하는 대신, 포트폴리오에서는 **담당 영역의 구조와 구현 방식이 드러나는 코드 Skeleton / Interface / StateFlow 예시**를 중심으로 정리했습니다.

서비스 고유 데이터, 내부 비즈니스 로직, 계정 및 Player 핵심 구현은 공개하지 않습니다.
