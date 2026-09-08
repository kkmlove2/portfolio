# MyTVOnline+ — Home / Live / Setting / Profile Portfolio

> Android 애플리케이션에서 **Home, Live, Setting, Profile 영역**을 중심으로 UI와 사용자 흐름을 구현한 프로젝트

---

## 프로젝트 소개

**MyTVOnline+** 프로젝트에서 제가 담당한 영역을 정리한 포트폴리오입니다.

제가 담당한 범위는 **Home UI / Live / Setting UI / Profile**이며, 각 영역의 실제 구현 내용을 중심으로 정리했습니다.

특히 **Home과 Setting은 UI 구현을 중심으로 담당**했으며, Profile은 **`profile/` 폴더 내 구현만** 담당 범위로 포함했습니다.

VOD, Search, TV Series, Player 및 Member 관련 기능은 담당 범위에서 제외했습니다.

---

# 담당 범위

## 1. Home — UI 구현

앱의 메인 화면인 Home 영역의 **화면 UI 구성과 사용자에게 보여지는 콘텐츠 영역의 레이아웃**을 구현했습니다.

### 주요 구현 영역

- Home 메인 화면 UI 구성
- Banner UI 구성
- Trending 콘텐츠 영역 UI
- 최근 시청 콘텐츠 영역 UI
- Notice 영역 UI
- Compose 기반 UI 구성
- 화면 크기에 대응하는 Adaptive UI 구성
- Home 화면 Navigation UI 흐름 구성

### 주요 파일

```text
home/
├── HomeScreen.kt
├── HomeNavigation.kt
├── LiveRecentList.kt
├── TrendingList.kt
├── ExpireReminder.kt
├── banner/
│   ├── Banner.kt
│   ├── BannerData.kt
│   └── BannerConfig.kt
└── notice/
    ├── NoticeData.kt
    └── NoticeConfig.kt
```

### Home 화면 구조

```text
Home
 └─ Dashboard
     ├─ Banner
     ├─ Trending
     ├─ Live Recent
     ├─ Recent Content
     └─ Notice
```

> Home은 **UI 구현 범위**를 기준으로 정리했습니다. 데이터 처리나 기능 로직 자체를 주요 담당 영역으로 기술하지 않았습니다.

---

# 2. Live

Live 콘텐츠를 탐색하고 채널 및 EPG 정보를 확인할 수 있는 Live 영역을 구현했습니다.

Player 자체 구현은 담당 범위에서 제외하고, **Live 화면의 채널/그룹/EPG 탐색과 UI 흐름**을 중심으로 정리했습니다.

### 주요 구현 영역

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

# 3. Setting — UI 구현

Setting 영역에서는 앱 설정 화면과 세부 설정 화면의 **UI 및 화면 구성**을 담당했습니다.

### 주요 구현 영역

- Setting 화면 UI 구성
- EPG Data Setting UI
- Audio / Subtitle Setting UI
- Subtitle Appearance UI
- App Settings UI
- About Dialog UI
- Setting 화면 Navigation 구성
- Compose 기반 설정 화면 구성

### 주요 파일

```text
setting/
├── EPGDataScreen.kt
├── EpgOffset.kt
├── AudioSubtitleScreen.kt
├── SubtitleAppearanceScreen.kt
├── AppSettingsScreen.kt
├── AboutDialog.kt
└── navigation/
    └── ManagePortalNavigation.kt
```

### Setting 화면 구조

```text
Setting
 ├─ EPG Data
 │   ├─ EPG Update
 │   └─ EPG Data Storage
 │
 ├─ Audio / Subtitle
 │   ├─ Audio Language
 │   ├─ Subtitle Language
 │   └─ Subtitle Appearance
 │
 ├─ App Settings
 └─ About
```

> Setting은 **화면 UI 구현을 중심으로 담당**했으며, 설정 데이터 처리나 백엔드 기능 자체를 주요 담당 영역으로 기술하지 않았습니다.

---

# 4. Profile

Profile 기능은 **`profile/` 폴더 내 구현만** 담당 범위로 정리했습니다.

여러 사용자가 하나의 앱 환경에서 각각의 Profile을 생성하고 선택할 수 있도록 Profile 생성, 수정, 보호, 전환 및 관리 화면을 구현했습니다.

### 주요 구현 영역

- Profile 생성
- Profile 이름 입력
- Avatar 선택
- Profile 수정
- Profile Settings
- Profile PIN / Protection UI
- Sensitive Categories 설정
- Profile 전환
- Profile 삭제
- Profile Navigation 구성
- Profile 선택 상태 관리
- Profile 변경에 따른 화면 흐름 연동

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

`ProfileMgr`를 중심으로 현재 선택된 Profile 상태를 관리하고 Profile 추가/수정/삭제 및 선택 화면과 연결했습니다.

Profile 전환 시 선택된 Profile에 맞는 화면 흐름이 이어지도록 관련 영역과 연동했습니다.

---

# Architecture & Implementation

## Jetpack Compose

Home과 Setting을 포함한 주요 화면 UI를 Jetpack Compose 기반으로 구성했습니다.

- `@Composable` 기반 UI
- Compose 상태 기반 화면 구성
- Compose Navigation을 활용한 화면 전환
- Material 3 기반 UI
- Adaptive UI 구성

## ViewModel / State

Live와 Profile 영역에서는 ViewModel 및 상태 관리 구조를 활용하여 화면 상태와 사용자 흐름을 연결했습니다.

```text
Live Screen
   ↓
LiveViewModel
   ↓
Channel / Group / EPG State
```

```text
Profile Screen
   ↓
ProfileMgr
   ↓
Selected Profile State
```

## Navigation

기능별 Navigation 구조를 분리하여 각 영역의 화면 흐름을 관리했습니다.

```text
App
 ├─ Home
 │   └─ Dashboard
 │
 ├─ Live
 │   └─ Channel List / EPG
 │
 ├─ Setting
 │   ├─ EPG
 │   ├─ Audio / Subtitle
 │   └─ App Settings
 │
 └─ Profile
     ├─ Profile Hub
     ├─ Add Profile
     ├─ Edit Profile
     └─ Switch Profile
```

---

# 내가 기여한 부분

## ① Home UI

- Home 메인 화면 UI 구성
- Banner / Trending / Recent 콘텐츠 영역 UI
- Notice 영역 UI
- Compose 기반 화면 구성
- Adaptive UI 적용

## ② Live

- Channel List 및 Group UI
- EPG List 및 Grid EPG UI
- 채널/EPG 탐색을 위한 화면 구조 구성
- Live 화면 상태 및 History 연동

## ③ Setting UI

- Setting 화면 UI 구성
- EPG Data 화면 UI
- Audio / Subtitle 화면 UI
- Subtitle Appearance 화면 UI
- App Settings 화면 UI
- About Dialog UI

## ④ Profile

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

- 기능별 화면 및 Navigation 구조 분리
- Jetpack Compose 기반 UI 구성
- Adaptive UI를 통한 다양한 화면 크기 대응
- 화면별 상태 관리 구조 활용

### 사용자 경험

- Home → Live → Setting → Profile로 이어지는 기능별 화면 흐름 구성
- Live에서 Channel / Group / EPG를 단계적으로 탐색할 수 있는 UI 구성
- Profile 생성부터 전환 및 관리까지 일관된 화면 흐름 구성

### Profile

- Profile 생성부터 삭제까지 전체 관리 흐름 구현
- Avatar / Name / PIN / Protection을 분리된 화면으로 구성
- 선택 Profile 상태를 화면과 연결하여 Profile 전환 흐름 구현

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
- `member/` 패키지
- `UserMgr`
- 기타 담당하지 않은 기능 및 모듈

> **Profile 기능은 `profile/` 폴더에 포함된 구현만 기준으로 정리했습니다.**

---

# Portfolio Summary

> **MyTVOnline+는 Home UI, Live, Setting UI, Profile 영역을 중심으로 Jetpack Compose와 Navigation을 활용하여 실제 Android 애플리케이션의 화면과 사용자 흐름을 구현한 프로젝트입니다.**
>
> 특히 **Home과 Setting의 UI 구현, Live의 Channel / EPG 탐색 UI, Profile의 생성·수정·전환·보호 흐름**을 담당하여 기능별 화면을 구성하고 서로 연결되는 사용자 경험을 구현했습니다.
