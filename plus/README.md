# MyTVOnline+ — Home / Live / Setting / Profile Portfolio

> Android 애플리케이션에서 **Home, Live, Setting, Profile 기능**을 중심으로 화면 구성과 사용자 흐름을 구현한 프로젝트

---

## 프로젝트 소개

**MyTVOnline+** 프로젝트에서 제가 담당한 영역을 정리한 포트폴리오입니다.

전체 프로젝트 중 제가 직접 작업한 범위인 **Home / Live / Setting / Profile**을 중심으로 정리했으며,
VOD, Search, TV Series, Player 등 제가 담당하지 않은 기능은 구현 범위에서 제외했습니다.

특히 Profile은 `profile/` 패키지 내 구현만 담당 범위로 포함했으며, **Member 관련 기능은 담당 범위에 포함하지 않았습니다.**

---

# 담당 범위

## 1. Home

앱의 메인 진입 화면으로 콘텐츠와 주요 기능을 한눈에 확인할 수 있도록 Home UI와 화면 흐름을 구성했습니다.

- Home 화면 UI 구성
- Home Navigation 구성
- Banner 영역 구성
- Trending 콘텐츠 영역
- 최근 시청 콘텐츠 영역
- 공지/Notice 영역
- Home 상태 관리
- Compose 기반 UI 구성
- 설정 화면 Navigation 연동
- 화면 크기에 대응하는 Adaptive UI 구성

### 주요 파일

```text
home/
├── HomeScreen.kt
├── HomeViewModel.kt
├── HomeNavigation.kt
├── LiveRecentList.kt
├── TrendingList.kt
├── VodRecentList.kt
├── ExpireReminder.kt
├── banner/
│   ├── Banner.kt
│   ├── BannerData.kt
│   └── BannerConfig.kt
└── notice/
    ├── NoticeData.kt
    └── NoticeConfig.kt
```

### Home Navigation

```text
Home
 └─ Dashboard
     ├─ Banner
     ├─ Trending
     ├─ Live Recent
     └─ Recent Content
```

---

# 2. Live

Live 콘텐츠를 탐색하고 채널 및 EPG 정보를 확인할 수 있는 Live 영역을 구현했습니다.

Player 자체 구현은 담당 범위에서 제외하고, **Live 화면의 채널/그룹/EPG 탐색과 UI 흐름**을 중심으로 정리했습니다.

- Live Navigation 구성
- Channel List 화면
- Channel Group / Favorite Group UI
- EPG List 화면
- Group 선택 UI
- Channel Logo UI
- 채널 목록 탐색 및 화면 상태 관리
- Grid EPG UI
- EPG 시간/프로그램 영역 구성
- EPG 상세 정보 Dialog
- Live History 관리
- Sport Mode 관련 UI/상태 관리
- Live ViewModel 기반 상태 관리

### 주요 파일

```text
live/
├── LiveNavigation.kt
├── LiveViewModel.kt
├── LiveData.kt
├── HistoryMgr.kt
├── TimeTickViewModel.kt
├── list/
│   ├── ChannelListScreen.kt
│   ├── ChannelListPage.kt
│   ├── ChannelList.kt
│   ├── GroupList.kt
│   ├── GroupChannelListPage.kt
│   ├── EpgList.kt
│   ├── EpgListPage.kt
│   ├── GroupDropdownButton.kt
│   └── ChannelLogoImage.kt
├── grid/
│   ├── GridEpgScreen.kt
│   ├── GridEpgContent.kt
│   ├── GridEpgViewModel.kt
│   ├── GridEpgState.kt
│   ├── GridEpgTimeHeader.kt
│   ├── GridEpgChannelColumn.kt
│   ├── GridEpgProgramCell.kt
│   └── EpgDetailDialog.kt
└── sportmode/
    └── SportModeMgr.kt
```

### Live 화면 구조

```text
Live
 ├─ Channel List
 │   ├─ Group
 │   ├─ Channel
 │   └─ EPG
 │
 └─ Grid EPG
     ├─ Time Header
     ├─ Channel Column
     ├─ Program Cell
     └─ EPG Detail
```

---

# 3. Setting

앱과 Live 시청 환경을 사용자가 원하는 형태로 설정할 수 있도록 Setting 화면과 세부 설정 화면을 구성했습니다.

### 주요 구현 영역

- Setting 화면 구성
- Setting Navigation 구성
- EPG 업데이트 설정
- EPG 데이터 보관 기간 설정
- Audio Language 설정
- Subtitle Language 설정
- Subtitle Appearance 설정
- App 관련 설정
- Portal / Server 관리 화면 흐름
- Live Group 관리 화면 흐름
- 설정 상태 및 사용자 설정값 관리
- 설정 변경에 따른 화면 흐름 처리

### 주요 파일

```text
setting/
├── SettingsViewModel.kt
├── EPGDataScreen.kt
├── EpgOffset.kt
├── AudioSubtitleScreen.kt
├── SubtitleAppearanceScreen.kt
├── AppSettingsScreen.kt
├── AboutDialog.kt
├── navigation/
│   └── ManagePortalNavigation.kt
└── register/
    ├── server/
    │   ├── ManagePortalScreen.kt
    │   ├── PortalViewModel.kt
    │   ├── ServerDetailScreen.kt
    │   ├── InputServerDataScreen.kt
    │   ├── ConnectingScreen.kt
    │   └── ConnectResultScreen.kt
    │
    └── groups/
        ├── ManageGroupScreen.kt
        ├── ManageGroupViewModel.kt
        ├── ManageGroupImpl.kt
        └── ManageGroup.kt
```

### EPG Setting

EPG 데이터 설정에서는 사용자가 EPG 자동 업데이트 여부와 데이터 보관 기간을 선택할 수 있도록 구성했습니다.

```text
EPG Data Setting
 ├─ EPG Update
 └─ EPG Data Storage
      └─ Storage Days
```

### Audio / Subtitle Setting

Audio 및 Subtitle 설정을 분리하여 언어 선택과 Subtitle 관련 표시 옵션을 관리할 수 있도록 구성했습니다.

```text
Audio / Subtitle
 ├─ Audio Language
 ├─ Subtitle Language
 └─ Subtitle Appearance
```

---

# 4. Profile

Profile 기능은 **`profile/` 폴더 내 구현만** 담당 범위로 정리했습니다.

여러 사용자가 하나의 앱 환경에서 각각의 Profile을 생성하고 선택할 수 있도록 Profile 생성, 수정, 보호, 전환 및 관리 화면을 구성했습니다.

- Profile 생성
- Profile 이름 입력
- Avatar 선택
- Profile 수정
- Profile Settings
- Profile PIN / 보호 기능
- Sensitive Categories 설정
- Profile 전환
- Profile 삭제
- Profile 선택 상태 관리
- Profile Navigation 구성
- Profile 최대 개수 관리
- Profile 변경에 따른 Live 데이터 초기화 흐름 연동

### 주요 파일

```text
profile/
├── ProfileMgr.kt
├── ProfileManagementScreen.kt
├── ProfileHubAdaptiveScreen.kt
├── ProfileHubNavigation.kt
├── ProfileNavigation.kt
├── SwitchProfileScreen.kt
├── EditProfileScreen.kt
├── ProfileSettingsScreen.kt
├── ChooseAvatarScreen.kt
├── NameInputScreen.kt
├── NameInputDialog.kt
├── ProfilePinDialog.kt
├── ProtectionScreen.kt
├── SensitiveCategoriesScreen.kt
├── HelpAndInfoScreen.kt
└── AddProfileNavigation.kt
```

### Profile 관리 흐름

```text
Profile
 ├─ Switch Profile
 │
 ├─ Profile Hub
 │   ├─ Profile Management
 │   │   └─ Edit Profile
 │   │       ├─ Avatar
 │   │       ├─ Name
 │   │       └─ PIN / Protection
 │   │
 │   ├─ Profile Settings
 │   └─ Sensitive Categories
 │
 ├─ Add Profile
 │   ├─ Choose Avatar
 │   └─ Enter Name
 │
 └─ Delete Profile
```

### Profile 상태 관리

`ProfileMgr`를 중심으로 현재 선택된 Profile을 `StateFlow`로 관리하고, Profile 추가/수정/삭제 및 선택 상태를 화면과 연결했습니다.

또한 Profile을 변경하면 Live 영역의 기존 상태를 정리하고 선택된 Profile에 맞는 Live 데이터를 초기화하는 흐름으로 연결했습니다.

---

# Architecture & Implementation

## Jetpack Compose

화면 UI를 Jetpack Compose 기반으로 구성했습니다.

- `@Composable` 기반 UI
- `remember / mutableStateOf`를 활용한 UI 상태 관리
- `StateFlow / Flow` 기반 상태 관찰
- Compose Navigation을 활용한 화면 전환
- Material 3 기반 UI
- Adaptive UI 구성

## ViewModel

화면별 상태와 로직을 ViewModel 단위로 분리했습니다.

```text
HomeScreen
   ↓
HomeViewModel
   ↓
Home State / Data
```

```text
Live Screen
   ↓
LiveViewModel
   ↓
Channel / Group / EPG State
```

```text
Setting Screen
   ↓
SettingsViewModel
   ↓
Setting State / Preferences
```

## Navigation

기능별 Navigation Graph를 분리하여 각 영역의 화면 흐름을 관리했습니다.

```text
App
 ├─ Home
 │   └─ Dashboard
 │
 ├─ Live
 │   └─ Channel List
 │
 ├─ Setting
 │   ├─ EPG
 │   ├─ Audio / Subtitle
 │   ├─ Portal / Server
 │   └─ Group
 │
 └─ Profile
     ├─ Profile Hub
     ├─ Add Profile
     ├─ Edit Profile
     └─ Switch Profile
```

---

# 내가 기여한 부분

## ① Home UI 및 Navigation

- Home 메인 화면 구성
- Banner / Trending / Recent 콘텐츠 영역 구성
- Home 상태 관리
- Home ↔ Setting Navigation 연동

## ② Live 화면 구성

- Channel List 및 Group UI
- EPG List 및 Grid EPG UI
- 채널/EPG 탐색을 위한 화면 구조 구성
- Live 상태 관리 및 History 연동

## ③ Setting 기능 구현

- EPG 데이터 설정
- Audio / Subtitle 설정
- Subtitle Appearance 설정
- Portal / Server 관리 화면 흐름
- Live Group 관리 화면 흐름
- Setting Navigation 구성

## ④ Profile 기능 구현

- Profile 생성 / 수정 / 삭제
- Avatar / Name 관리
- Profile PIN 및 Protection
- Profile Settings
- Profile 전환
- Profile 상태 관리
- Profile Navigation 구성

---

# 기술적 성과

### UI / Architecture

- 기능별 패키지 및 Navigation 구조 분리
- Compose 기반 상태 중심 UI 구성
- ViewModel을 통한 화면 상태 관리
- StateFlow를 활용한 상태 전달
- 공통 Setting UI 컴포넌트 활용

### 사용자 경험

- Home → Live → Setting → Profile로 이어지는 기능별 화면 흐름 구성
- Profile 전환 시 선택된 Profile에 맞는 Live 상태 연동
- EPG / Channel / Group을 단계적으로 탐색할 수 있는 Live UX 구성
- 화면 크기를 고려한 Adaptive UI 적용

### Profile

- Profile 생성부터 삭제까지 전체 관리 흐름 구현
- Avatar / Name / PIN / Protection을 분리된 화면으로 구성
- Profile 상태를 StateFlow로 관리하여 화면 간 상태 동기화

---

# Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin, Java |
| Platform | Android |
| UI | Jetpack Compose, Material 3 |
| Architecture | ViewModel, StateFlow / Flow |
| Navigation | Compose Navigation |
| DI | Hilt |
| UI Adaptation | Material 3 Adaptive |

---

# 담당 범위 외

아래 기능은 제가 담당한 범위가 아니므로 본 포트폴리오의 구현 내용에서 제외했습니다.

- VOD
- Search
- TV Series
- Player
- Member / Account 관련 기능
- 기타 담당하지 않은 기능 및 모듈

> **Profile 기능은 `profile/` 폴더에 포함된 구현만 기준으로 정리했습니다.**

---

# Portfolio Summary

> **MyTVOnline+는 Home, Live, Setting, Profile 영역을 중심으로 Jetpack Compose 기반 UI와 Navigation, ViewModel, StateFlow를 활용하여 실제 사용자 흐름을 구현한 프로젝트입니다.**
>
> 특히 **Live의 Channel / EPG 탐색, Setting의 세부 설정, Profile의 생성·수정·전환·보호 흐름**을 기능별로 분리하고 서로 연결되는 사용자 경험을 구현한 프로젝트입니다.
