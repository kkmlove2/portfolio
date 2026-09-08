# MOL4 — Maintenance / Live / Group Management / Profile Portfolio

> 기존 Android TV 애플리케이션의 **유지보수와 Live 영역 개선, Group / Server 관리 기능 및 Profile UI**를 중심으로 작업한 프로젝트

---

## 프로젝트 소개

**MOL4** 프로젝트에서 제가 담당한 작업을 정리한 포트폴리오입니다.

이 프로젝트에서는 신규 기능을 대규모로 개발하기보다는 기존 서비스의 **유지보수와 기능 개선**을 중심으로 작업했으며, 특히 **Live UI, Group 관리, Server 관리, Favorite Group / Channel, Pinned Group, Profile UI 전체**와 관련된 영역을 주로 담당했습니다.

기존 코드 구조를 이해하고 영향 범위를 고려하면서 Android TV 환경의 UI와 사용자 흐름을 개선하는 작업에 집중했습니다.

---

# 주요 담당 범위

## 1. 유지보수 및 기능 개선

기존 Android TV 애플리케이션의 기능과 UI를 유지보수하고, 기존 구조를 기반으로 필요한 기능을 수정 및 개선했습니다.

### 주요 업무

- 기존 기능 유지보수
- 기존 UI 및 사용자 흐름 개선
- 기능 변경에 따른 관련 화면 수정
- 기존 코드 구조를 고려한 기능 수정
- Android TV 환경에서의 UI/Focus 동작 개선
- Live 및 Profile 영역의 지속적인 버그 수정 및 개선

> 신규 기능 구현뿐만 아니라 **기존 서비스의 안정적인 운영과 기능 개선을 위한 유지보수 경험**을 주요 작업 범위로 포함했습니다.

---

# 2. Live UI

Live 영역에서는 Android TV에서 채널을 탐색하고 Group과 Channel을 관리할 수 있도록 **Live UI 전반과 관련 화면 흐름**을 담당했습니다.

### 주요 구현 영역

- Live Channel List UI
- Group List UI
- Channel List UI
- EPG 관련 UI
- Live 정보 화면 UI
- Channel 선택 및 탐색 UI
- Android TV Focus 기반 UI 동작 개선
- Context Menu / Dialog UI
- Live 화면의 기존 기능 유지보수 및 개선

### 주요 파일

```text
live/
├── chlist/
│   ├── LiveVerticalGridView.java
│   ├── ChNumInputLayout.kt
│   ├── ContextPopupDialog.kt
│   ├── group/
│   │   ├── ManageGroupDialog.kt
│   │   ├── GroupListAdapter.kt
│   │   ├── GroupListPresenter.kt
│   │   ├── GroupListItemView.kt
│   │   └── GroupListHeaderItemView.kt
│   ├── channel/
│   │   ├── ChannelListItem.kt
│   │   ├── ChannelListPresenter.kt
│   │   ├── ChannelListAdapter.java
│   │   ├── ChannelLogoImageView.kt
│   │   └── ManageChannelDialog.kt
│   └── server/
│       ├── ManageServerDialog.kt
│       ├── ServerListAdapter.kt
│       ├── ServerGroupListLayout.kt
│       └── LiveServerListItemView.kt
│
├── live/
│   ├── LiveFragment.java
│   ├── LiveFragmentTv.java
│   ├── LiveFragmentRadio.java
│   ├── LiveChannelListLayout.java
│   └── info/
│       ├── LiveInfoContainer.java
│       ├── LiveActionButtonLayout.kt
│       ├── LiveNavigationButtonLayout.java
│       └── InfoEpgProgressView.kt
│
├── epggrid/
│   ├── EpgGridFragment.java
│   ├── EpgGridAdapter.java
│   ├── EpgGridRowView.java
│   └── EpgGridItemView.java
│
└── epgsingle/
    ├── EpgSingleFragment.kt
    ├── EpgSingleAdapter.java
    └── EpgSingleDialog.java
```

### Live UI 구조

```text
Live
 ├─ Group List
 │   ├─ Favorite Group
 │   ├─ Pinned Group
 │   └─ Server Group
 │
 ├─ Channel List
 │   ├─ Favorite Channel
 │   └─ Channel Navigation
 │
 └─ EPG
     ├─ Grid EPG
     └─ Single EPG
```

---

# 3. ManageGroup

Live Group 관리 기능에서는 기본 Group과 사용자 관리 Group을 Android TV UI에서 편리하게 관리할 수 있도록 관련 화면과 동작을 유지보수 및 개선했습니다.

### 주요 구현 영역

- Manage Group UI
- Group 목록 UI
- Group 선택 / 이동
- Group 순서 변경
- Favorite Group 관리
- Favorite Group 이름 변경
- Favorite Group 삭제
- Group Pin / Unpin
- Pinned Group 순서 관리
- Group 변경에 따른 Live Channel List 갱신
- Group 관리 Context Menu / Dialog

### 관련 코드

```text
live/chlist/group/
├── ManageGroupDialog.kt
├── GroupListAdapter.kt
├── GroupListPresenter.kt
├── GroupListItemView.kt
├── GroupListHeaderPresenter.kt
└── GroupListHeaderItemView.kt
```

### Group 관리 흐름

```text
Group Management
 ├─ Group List
 │   ├─ Server Group
 │   ├─ Favorite Group
 │   └─ Pinned Group
 │
 ├─ Favorite Group
 │   ├─ Add
 │   ├─ Rename
 │   ├─ Delete
 │   └─ Reorder
 │
 └─ Pinned Group
     ├─ Pin
     ├─ Unpin
     └─ Reorder
```

---

# 4. Favorite Group / Channel

사용자가 자주 사용하는 Group과 Channel을 빠르게 접근할 수 있도록 **Favorite Group 및 Favorite Channel 관련 기능**을 유지보수하고 개선했습니다.

### Favorite Group

- Favorite Group 생성
- Favorite Group 이름 변경
- Favorite Group 삭제
- Favorite Group 순서 변경
- Favorite Group 선택 및 탐색 UI

### Favorite Channel

- Favorite Channel 표시
- Favorite Channel 관리 UI
- Favorite Channel 순서 변경
- Favorite Channel 필터
- Favorite Channel 상태에 따른 UI 처리

### 관련 구현

```text
live/chlist/
├── group/
│   └── ManageGroupDialog.kt
│
├── channel/
│   ├── ManageChannelDialog.kt
│   ├── ChannelListItem.kt
│   ├── ChannelListAdapter.java
│   └── ChSort.kt
│
└── LiveChannelListPolicy.kt
```

Favorite Group과 Channel의 변경 사항이 Live Channel List에 반영되도록 기존 화면 흐름과 연결하여 관리했습니다.

---

# 5. Pinned Group

Live 화면에서 자주 사용하는 Group을 상단에서 빠르게 접근할 수 있도록 **Pinned Group 기능**을 관리했습니다.

### 주요 구현 영역

- Group Pin / Unpin
- Pinned Group 표시
- Pinned Group 순서 변경
- Pin 상태에 따른 Group UI 처리
- Group 이동에 따른 목록 위치 갱신
- Android TV 리모컨 기반 Focus / 이동 흐름 대응

### 동작 흐름

```text
Group
  ↓
Pin / Unpin
  ↓
Pinned Group List
  ↓
Reorder
  ↓
Live Group List 반영
```

---

# 6. Server 관리

Live에서 사용하는 Server를 관리하기 위한 **Server 목록 및 설정 화면**의 UI와 기존 기능을 유지보수했습니다.

### 주요 구현 영역

- Server 목록 UI
- Server 선택 UI
- Server 관리 Dialog
- Server 상세 화면
- Server 추가 / 수정 화면 흐름
- Server 연결 관련 화면 유지보수
- Server와 Group / Channel 목록의 연결 흐름 개선

### 주요 파일

```text
live/chlist/server/
├── ManageServerDialog.kt
├── ServerGroupListLayout.kt
├── ServerListAdapter.kt
└── LiveServerListItemView.kt
```

관련 Server 등록/수정 화면은 다음 영역에서 관리됩니다.

```text
register/server/
├── ServerFragment.kt
├── ServerEnableFragment.kt
├── detail/
│   ├── DetailFragment.kt
│   ├── ServerDetailLayout.kt
│   └── SettingsContainer.kt
└── add/
    ├── AddServerDetailFragment.kt
    ├── ModifyServerFragment.kt
    ├── ModifyPortalFragment.kt
    ├── AddServerContainerFragment.kt
    └── EditServerContainerFragment.kt
```

---

# 7. Profile UI 전체

Profile 영역에서는 **Profile UI 전체 흐름**을 담당했습니다.

Profile 생성부터 선택, 수정, 잠금, 민감 콘텐츠 설정, 삭제까지 Android TV 환경에서 사용할 수 있도록 Profile 관련 화면을 구성하고 유지보수했습니다.

### 주요 구현 영역

- Profile 선택 화면
- Profile 생성 UI
- Avatar 선택 UI
- Profile 이름 입력 UI
- Profile 수정 UI
- Profile PIN 변경 UI
- Profile Lock UI
- Sensitive Content 설정 UI
- Profile 삭제 UI
- Profile Management UI
- Profile 상태 및 화면 흐름 유지보수

### 주요 파일

```text
main/profile/
├── ProfileActivity.kt
├── ProfileMgr.kt
├── ChooseProfileFragment.kt
├── ProfileItemView.kt
├── ProfileBadgeView.kt
├── ProfileImageView.kt
├── SensitiveContentsFragment.kt
│
├── add/
│   ├── AddProfileDialog.kt
│   ├── ChooseAvatarFragment.kt
│   ├── ChooseAvatarDialog.kt
│   ├── NameInputFragment.kt
│   ├── NameInputDialog.kt
│   └── ProfileAddView.kt
│
├── edit/
│   ├── EditProfileEntryFragment.kt
│   ├── EditProfileSectionActivity.kt
│   ├── EditProfileSectionHostFragment.kt
│   ├── shared/
│   │   └── EditProfileDisplayFragment.kt
│   ├── avatar/
│   │   └── AvatarSectionElement.kt
│   ├── profilename/
│   │   └── ProfileNameSectionElement.kt
│   ├── lockprofile/
│   │   └── EditLockProfileFragment.kt
│   ├── changeprofilepin/
│   │   └── EditChangeProfilePinFragment.kt
│   ├── deleteprofile/
│   │   └── EditDeleteProfileSectionFragment.kt
│   └── sensitivecontent/
│       ├── SettingSensitiveCategoriesFragment.kt
│       └── hidesensitivecontent/
│           └── SettingHideSensitiveContentFragment.kt
│
└── managev2/
    ├── selection/
    │   ├── ProfileSelectionSectionFragment.kt
    │   └── sensitivecontent/
    │       └── SensitiveContentSectionFragment.kt
    └── add/
        └── AddProfileSectionFragment.kt
```

### Profile 화면 흐름

```text
Profile
 ├─ Choose Profile
 │
 ├─ Add Profile
 │   ├─ Choose Avatar
 │   └─ Enter Name
 │
 ├─ Edit Profile
 │   ├─ Avatar
 │   ├─ Profile Name
 │   ├─ Lock Profile
 │   ├─ Change PIN
 │   ├─ Sensitive Content
 │   └─ Delete Profile
 │
 └─ Profile Management
```

---

# Android TV UI 경험

이 프로젝트에서는 일반 모바일 UI가 아닌 **Android TV 환경을 고려한 화면 유지보수 및 개선** 경험을 쌓았습니다.

### 주요 경험

- D-pad 기반 Focus 이동
- Focus 상태에 따른 UI 표시
- TV 화면에 적합한 Grid / List UI
- Remote Controller 중심의 사용자 흐름
- Dialog / Context Menu 기반 관리 UI
- 기존 Leanback 스타일 UI 구조 유지보수
- 화면 전환 및 목록 갱신에 따른 Focus 흐름 개선

특히 Group / Channel / Profile처럼 리모컨으로 많은 항목을 탐색해야 하는 화면에서 **Focus와 선택 상태가 자연스럽게 이어지는 UI 흐름**을 고려했습니다.

---

# Architecture & Implementation

## 기존 코드 구조 기반 유지보수

신규 프로젝트를 처음부터 구성하기보다 기존 Android 애플리케이션의 구조와 공통 컴포넌트를 이해한 뒤 필요한 부분을 수정하고 확장하는 방식으로 작업했습니다.

```text
Existing Architecture
        ↓
Analyze Existing Flow
        ↓
Modify / Extend UI & Logic
        ↓
Validate Related Screens
        ↓
Maintenance / Bug Fix
```

## Android View 기반 UI

프로젝트 특성상 기존 Android View 및 Leanback 계열 UI 구조가 함께 사용되며, 기존 Adapter / Presenter / Fragment 구조를 기반으로 화면을 유지보수했습니다.

- Fragment
- Adapter
- Presenter
- Custom View
- Dialog
- Android TV Focus
- Grid / List UI

## Kotlin / Java 혼합 환경

기존 프로젝트 구조에 맞춰 Kotlin과 Java 코드를 함께 유지보수하고 기능을 수정했습니다.

---

# 내가 기여한 부분

## ① 프로젝트 유지보수

- 기존 Android TV 애플리케이션 유지보수
- 기존 화면 및 기능 수정
- 버그 수정 및 동작 개선
- 관련 기능 간 영향 범위 확인 및 연동 수정

## ② Live UI

- Live Channel List UI
- Group List UI
- EPG UI
- Live 정보 화면 UI
- Android TV Focus / Navigation 개선

## ③ Group / Channel 관리

- ManageGroup
- Favorite Group
- Favorite Channel
- Pinned Group
- Group / Channel 순서 관리
- Group / Channel 목록 UI 및 상태 반영

## ④ Server 관리

- Server 목록 및 관리 UI
- Server 추가 / 수정 화면 유지보수
- Server와 Live Group / Channel 흐름 연동

## ⑤ Profile UI

- Profile UI 전체
- Profile 선택
- Profile 생성
- Avatar / Name
- Profile 수정
- Profile Lock / PIN
- Sensitive Content
- Profile 삭제

---

# 기술적 성과

### 유지보수 역량

- 기존 대규모 Android 코드베이스 구조 파악
- 기존 컴포넌트와 화면 흐름을 이해한 후 기능 수정
- 기능 변경에 따른 연관 화면 영향 범위 관리
- 기존 구조를 최대한 유지하면서 필요한 부분을 개선

### Android TV UI

- D-pad / Focus 기반 UI 흐름 이해
- TV 환경의 Grid / List 탐색 UX 개선
- Dialog 및 Context Menu 기반 관리 UI 구현 및 유지보수
- Profile / Group / Channel처럼 복잡한 탐색 화면의 UI 흐름 개선

### Live / Group Management

- Favorite Group / Channel 관리
- Pinned Group 관리
- Group 순서 변경 및 목록 반영
- Server → Group → Channel로 이어지는 관리 흐름 유지보수

---

# Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin, Java |
| Platform | Android TV |
| UI | Android View, Leanback-style UI, Custom View |
| Architecture | Fragment, Adapter, Presenter, ViewModel / Manager 구조 |
| Navigation | Fragment 기반 Navigation |
| UI Interaction | D-pad, Focus, Dialog, Context Menu |

---

# 담당 범위 외

본 포트폴리오에서는 제가 주요하게 담당한 **유지보수 / Live / Group 관리 / Server / Profile UI** 영역을 중심으로 정리했습니다.

다음 영역은 주요 담당 범위에서 제외했습니다.

- VOD / Playback 자체 구현
- Search 자체 구현
- Player / ExoPlayer 자체 구현
- 기타 직접 담당하지 않은 기능 및 모듈

---

# Portfolio Summary

> **MOL4 프로젝트에서는 기존 Android TV 애플리케이션을 기반으로 유지보수와 기능 개선을 수행했으며, 특히 Live UI, ManageGroup, Server 관리, Favorite Group / Channel, Pinned Group, Profile UI 전체 영역을 중심으로 작업했습니다.**
>
> 기존 코드 구조를 빠르게 파악하고 Adapter / Presenter / Fragment / Custom View 등의 구조를 활용하여 필요한 기능을 수정하고 화면 흐름을 개선한 경험을 쌓은 프로젝트입니다.
