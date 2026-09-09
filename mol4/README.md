# MOL4 — Android TV Maintenance & Feature Improvement

> 기존 Android TV 애플리케이션을 유지보수하면서 **Live UI, Group / Server 관리, Favorite / Pinned Group, Profile UI**를 중심으로 기능과 화면을 개선한 프로젝트입니다.

---

## 프로젝트 소개

MOL4에서는 신규 서비스를 처음부터 개발하기보다 **기존 코드 구조와 사용자 흐름을 분석하고 필요한 기능을 수정·개선하는 유지보수 업무**를 중심으로 작업했습니다.

특히 Android TV에서 리모컨으로 사용하는 Live / Group / Channel / Profile 화면의 UI와 흐름을 개선했습니다.

---

## 담당 범위

### Live UI
- Live Channel List UI
- Group / Channel UI
- EPG UI
- Live 정보 화면
- Context Menu / Dialog
- Android TV D-pad / Focus UX
- 기존 Live 기능 유지보수 및 Bug Fix

### Group / Channel 관리
- ManageGroup
- Favorite Group 추가 / 이름 변경 / 삭제 / 순서 변경
- Favorite Channel 관리 및 순서 변경
- Pinned Group Pin / Unpin / 순서 변경
- Group 변경에 따른 Live Channel List 반영

### Server 관리
- Server 목록 및 관리 UI
- Server 추가 / 수정 화면 유지보수
- Server와 Live Group / Channel 흐름 연결

### Profile
- Profile 선택 / 생성 / 수정 / 삭제
- Avatar / Name 설정
- PIN / Lock
- Sensitive Content 설정
- Profile Management 화면

---

# 핵심 구조

MOL4에서 보여주고 싶은 부분은 **기존 구조를 분석한 뒤 기능별 책임을 나누고, 기능 변경 결과가 관련 UI와 사용자 흐름에 이어지도록 연결한 경험**입니다.

```text
Existing Code
     ↓
Flow / Responsibility 분석
     ↓
Feature 수정 및 확장
     ↓
관련 UI / 데이터 반영
     ↓
Bug Fix & Regression Check
```

### Group Management

```text
Group Management
       │
       ├── Favorite Group
       │     ├── Add / Rename
       │     ├── Delete
       │     └── Reorder
       │
       ├── Favorite Channel
       │     └── Reorder
       │
       └── Pinned Group
             ├── Pin / Unpin
             └── Reorder
                    ↓
             Live Group / Channel List
```

---

# Code Skeleton

> 아래 코드는 실제 서비스 소스를 공개한 것이 아니라, **실제 담당 영역의 구조와 설계 의도를 보여주기 위해 재구성한 Skeleton**입니다.

### 1. Favorite Group — 관리 역할 분리

```kotlin
interface FavoriteGroupController {
    fun addGroup(name: String)
    fun renameGroup(groupId: String, name: String)
    fun deleteGroup(groupId: String)
    fun moveGroup(groupId: String, position: Int)
}

class FavoriteGroupManager : FavoriteGroupController {
    private val groups = mutableListOf<String>()

    override fun addGroup(name: String) {
        groups += name
    }

    override fun renameGroup(groupId: String, name: String) {
        // Resolve the target group and update its name.
    }

    override fun deleteGroup(groupId: String) {
        // Remove the selected group.
    }

    override fun moveGroup(groupId: String, position: Int) {
        // Reorder the group and reflect the change in the related UI flow.
    }
}
```

**의도**

- Group 관리 기능의 역할을 Interface로 분리
- Add / Rename / Delete / Reorder 책임을 명확하게 정의
- 실제 서비스 구현은 공개하지 않고 관리 구조만 표현

---

### 2. Pinned Group — 상태 및 순서 변경 관리

```kotlin
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
```

**의도**

Pinned 상태 변경과 순서 변경을 하나의 관리 흐름으로 구성하고, 변경 결과가 Live Group List에 반영되는 구조를 보여줍니다.

---

### 3. Profile — 사용자 흐름

```kotlin
data class Profile(
    val id: String,
    val name: String,
)

class ProfileViewModel {
    private val profiles = mutableListOf<Profile>()
    private var selectedProfileId: String? = null

    fun add(profile: Profile) {
        profiles += profile
        if (selectedProfileId == null) {
            selectedProfileId = profile.id
        }
    }

    fun select(profileId: String) {
        if (profiles.any { it.id == profileId }) {
            selectedProfileId = profileId
        }
    }

    fun update(profile: Profile) {
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            profiles[index] = profile
        }
    }

    fun delete(profileId: String) {
        profiles.removeAll { it.id == profileId }

        if (selectedProfileId == profileId) {
            selectedProfileId = profiles.firstOrNull()?.id
        }
    }
}
```

**의도**

Profile 생성 → 선택 → 수정 → 삭제의 사용자 흐름을 ViewModel에서 관리하는 형태로 표현했습니다. 실제 서비스의 저장 구조나 고유 로직은 공개하지 않습니다.

---

### 4. Live — 사용자 선택 흐름

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
```

**의도**

- Group → Channel → EPG로 이어지는 Live 탐색 흐름을 표현
- 사용자 선택과 화면 동작의 역할을 분리
- Android TV에서 D-pad로 탐색하는 화면 흐름을 고려

---

# Android TV UX

일반 모바일 UI와 달리 Android TV에서는 **D-pad / Focus / 선택 상태**가 사용자 경험에 직접적인 영향을 줍니다.

```text
Remote D-pad
     ↓
Focus 이동
     ↓
Group / Channel 선택
     ↓
관련 데이터 / UI 변경
     ↓
목록 / 화면 갱신
     ↓
다음 Focus 위치 유지
```

주요 경험:

- D-pad 기반 Focus 이동
- Focus 상태에 따른 UI 변화
- Grid / List 탐색 UX
- Dialog / Context Menu
- 목록 갱신 후 Focus 흐름 유지
- TV 화면에 맞는 사용자 흐름 개선

---

# 유지보수 경험

MOL4에서 중요한 부분은 **기존 코드에 기능을 추가하면서 기존 사용자 흐름을 깨뜨리지 않는 것**이었습니다.

```text
기존 기능 분석
      ↓
영향 범위 확인
      ↓
필요한 코드 수정
      ↓
관련 화면 확인
      ↓
Bug Fix / 동작 검증
```

단순 UI 구현뿐 아니라 **기존 서비스 코드베이스를 이해하고 안전하게 수정하는 경험**을 중심으로 작업했습니다.

---

# 내가 보여주고 싶은 개발 역량

### 기존 코드 분석 및 유지보수

기존 구조와 사용자 흐름을 파악한 뒤 영향 범위를 확인하고 필요한 부분을 수정했습니다.

### 책임 분리

Group 관리, Profile, Live 등 기능별 책임을 분리하고 변경이 필요한 영역을 명확하게 구성했습니다.

### 사용자 흐름 구현

Group / Channel / EPG와 Profile처럼 사용자 선택과 변경이 다음 화면과 연결되는 기능을 구현했습니다.

### Android TV UX

D-pad, Focus, Grid / List 탐색을 고려하여 TV 환경에서 자연스러운 사용자 흐름을 구현했습니다.

---

# Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin, Java |
| Platform | Android TV |
| UI | Android View, RecyclerView, Fragment, Custom View |
| TV UX | D-pad, Focus, Focus Animation |
| Structure | Interface, Adapter, Presenter, Fragment, Dialog |
| Development | Maintenance, Bug Fix, UI Improvement |

---

# 담당 범위 요약

| 영역 | 담당 내용 |
|---|---|
| **Live** | Channel / Group / EPG UI 및 유지보수 |
| **Group** | ManageGroup / Favorite / Pinned Group |
| **Channel** | Favorite Channel 및 목록 UI |
| **Server** | Server 관리 UI 및 관련 화면 유지보수 |
| **Profile** | Profile UI 전체 흐름 |
| **TV UX** | D-pad / Focus 기반 UI 개선 |
| **Maintenance** | 기존 기능 수정 / Bug Fix / 사용자 흐름 개선 |

> 실제 서비스 소스 전체를 공개하지 않고, 포트폴리오에서는 **담당 기능의 구조와 개발 방식이 드러나는 Skeleton Code**를 제공합니다.
