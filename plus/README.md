# MyTVOnline+ — Home / Live Setting / Profile

> Android 애플리케이션에서 **Home, Live Setting, Profile 기능**을 중심으로 UI와 사용자 흐름을 구현한 프로젝트

---

## 프로젝트 소개

MyTVOnline+ 프로젝트에서 제가 담당한 영역을 정리한 포트폴리오입니다.

전체 프로젝트 중 제가 직접 작업한 범위인 **Home / Live Setting / Profile** 기능을 중심으로 정리했으며,
VOD, Search, TV Series, Player 등 제가 담당하지 않은 기능은 본 문서의 구현 범위에서 제외했습니다.

---

## 담당 범위

### 1. Home

메인 진입 화면과 콘텐츠 탐색을 위한 Home 영역을 구현했습니다.

- Home 화면 구성 및 UI 구현
- Home Navigation 구성
- Banner 영역
- Trending 콘텐츠 영역
- 최근 시청 콘텐츠 영역
- Home 상태 관리
- Compose 기반 화면 구성
- 설정 화면으로의 Navigation 연동
- 반응형 화면 크기를 고려한 UI 구성

**주요 파일**

- `home/HomeScreen.kt`
- `home/HomeViewModel.kt`
- `home/HomeNavigation.kt`
- `home/LiveRecentList.kt`
- `home/TrendingList.kt`
- `home/VodRecentList.kt`
- `home/banner/`
- `home/notice/`

---

### 2. Live Setting

Live 시청 환경을 구성하고 관리하기 위한 설정 영역을 구현했습니다.

- Live 관련 설정 화면 구성
- EPG 관련 설정
- Audio / Subtitle 설정
- Live 서버 및 Portal 설정 UI
- Live Group 관리 UI
- 설정 화면 Navigation 구성
- 설정 상태 및 사용자 설정값 관리
- 설정 변경 후 화면 흐름 처리

**주요 파일**

- `setting/SettingsScreen.kt`
- `setting/SettingsViewModel.kt`
- `setting/EPGDataScreen.kt`
- `setting/EpgOffset.kt`
- `setting/AudioSubtitleScreen.kt`
- `setting/register/server/`
- `setting/register/groups/`
- `setting/navigation/`

> 설정 모듈 전체가 아닌, 제가 담당한 **Live 관련 설정 영역**을 중심으로 작성했습니다.

---

### 3. Profile

Profile 폴더 내 기능을 기준으로 프로필 생성부터 관리, 전환까지의 사용자 흐름을 구현했습니다.

- Profile 생성
- Avatar 선택
- Profile 이름 입력
- Profile 수정
- Profile 설정
- Profile PIN / 보호 기능
- Profile 전환
- Profile 삭제
- Profile 선택 상태 관리
- Profile Navigation 구성
- 최대 Profile 개수 관리

**주요 파일**

- `profile/ProfileMgr.kt`
- `profile/ProfileManagementScreen.kt`
- `profile/ProfileHubAdaptiveScreen.kt`
- `profile/ProfileHubNavigation.kt`
- `profile/ProfileNavigation.kt`
- `profile/SwitchProfileScreen.kt`
- `profile/EditProfileScreen.kt`
- `profile/ProfileSettingsScreen.kt`
- `profile/ChooseAvatarScreen.kt`
- `profile/NameInputScreen.kt`
- `profile/ProfilePinDialog.kt`
- `profile/ProtectionScreen.kt`
- `profile/SensitiveCategoriesScreen.kt`
- `profile/HelpAndInfoScreen.kt`
- `profile/AddProfileNavigation.kt`

### Profile 관리 흐름

```text
Profile
 ├─ Profile Management
 │   ├─ Edit Profile
 │   │   ├─ Avatar
 │   │   ├─ Name
 │   │   └─ PIN
 │   ├─ Profile Settings
 │   └─ Switch Profile
 │
 ├─ Add Profile
 │   ├─ Choose Avatar
 │   └─ Enter Name
 │
 └─ Delete Profile
```

Profile 선택 시 현재 Profile 상태를 갱신하고, 선택된 Profile에 맞는 Live 데이터 초기화 흐름과 연결되도록 구성했습니다.

---

## Architecture & Implementation

### Jetpack Compose

화면 UI를 Jetpack Compose 기반으로 구성했습니다.

- `@Composable` 기반 UI
- `remember / rememberSaveable`을 활용한 UI 상태 관리
- `collectAsState`를 활용한 StateFlow 상태 관찰
- Compose Navigation을 활용한 화면 전환
- Material 3 Adaptive API를 활용한 화면 대응

### ViewModel

화면의 상태와 비즈니스 로직을 ViewModel 단위로 분리했습니다.

```text
HomeScreen
   ↓
HomeViewModel
   ↓
Home State / Data
```

```text
SettingsScreen
   ↓
SettingsViewModel
   ↓
Settings State / Preferences
```

### Navigation

기능별 Navigation Graph를 분리해 화면 간 이동과 하위 화면 구조를 관리했습니다.

```text
Home
 ├─ Home Dashboard
 └─ Settings

Profile
 ├─ Profile Hub
 ├─ Profile Management
 ├─ Add Profile
 ├─ Edit Profile
 └─ Switch Profile
```

---

## 기술 스택

- Kotlin
- Android
- Jetpack Compose
- Material 3
- Compose Navigation
- ViewModel
- StateFlow / Flow
- Hilt
- Material 3 Adaptive

---

## 주요 구현 포인트

### 1. 기능별 화면 구조 분리

Home, Setting, Profile 기능을 각각의 패키지와 Navigation 구조로 분리하여 기능 단위의 유지보수가 가능하도록 구성했습니다.

### 2. 상태 기반 UI

Flow / StateFlow를 Compose UI와 연결하여 현재 Profile 및 화면 상태에 따라 UI가 변경되도록 구현했습니다.

### 3. Adaptive UI

화면 크기와 디바이스 형태에 따라 UI 레이아웃이 달라질 수 있도록 Material 3 Adaptive API와 공통 UI 컴포넌트를 활용했습니다.

### 4. Profile 사용자 흐름

Profile 추가 → 선택 → 수정 → 설정 → 전환 → 삭제까지 하나의 사용자 흐름으로 연결하여 Profile 관리 기능을 구성했습니다.

### 5. Live 설정과 화면 흐름 연동

Live 관련 설정과 서버/그룹 관리 화면을 Navigation 구조로 연결하여 설정 변경과 화면 이동이 자연스럽게 이어지도록 구성했습니다.

---

## 담당 범위 외

아래 기능은 제가 담당한 범위가 아니므로 이 포트폴리오의 구현 내용에서는 제외했습니다.

- VOD
- Search
- TV Series
- Player
- 기타 담당하지 않은 기능 및 모듈

또한 **Member 관련 기능은 담당 범위에 포함하지 않았으며**, Profile 기능은 `profile/` 폴더에 포함된 구현만 기준으로 정리했습니다.
