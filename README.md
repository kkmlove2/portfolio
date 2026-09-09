# 👨‍💻 Android / Android TV Developer Portfolio

> **Android 및 Android TV 환경에서 UI 구현, 유지보수, 공통화, 추상화, 사용자 흐름 구현을 경험한 프로젝트 포트폴리오입니다.**

---

## 🚀 Projects

### 📺 FormulerLauncher

**Android TV Launcher / UI Architecture**

Android TV Launcher 환경에서 공통 UI 구조를 추상화하고 재사용성을 높인 프로젝트입니다.

**Key Points**
- `ItemView` Interface
- `GridAdapter` / `GridAdapterContent` / `GridItemView` 추상화
- `GridAdapterContentApps` / `GridAdapterLockApps` 구현
- UI 코드 공통화
- Android TV D-pad / Focus UX
- Focus Animation

👉 **[FormulerLauncher 상세 보기](./formulerLauncher/)**

---

### 🛠️ MOL4

**Android TV Application / Maintenance & Feature Improvement**

기존 Android TV 애플리케이션을 유지보수하면서 Live, Group, Server, Profile 영역을 중심으로 UI와 관리 기능을 개선한 프로젝트입니다.

**Key Points**
- 기존 기능 유지보수 및 버그 수정
- Live UI 전체
- `ManageGroup`
- Server 관리
- Favorite Group / Channel
- Pinned Group
- Profile UI 전체
- Android TV D-pad / Focus UX

👉 **[MOL4 상세 보기](./mol4/)**

---

### 📱 MyTVOnline+

**Android Application / Home · Live · Setting · Profile**

Home과 Setting은 UI 구현을 중심으로 담당하고, Live 및 Profile 영역의 화면과 사용자 흐름을 구현한 프로젝트입니다.

**Key Points**
- Home UI
- Live Channel / Group / EPG UI
- Setting UI
- Profile 생성 / 수정 / 삭제 / 전환
- PIN / Protection UI
- Jetpack Compose
- Navigation / Adaptive UI

👉 **[MyTVOnline+ 상세 보기](./plus/)**

---

### 🏟️ Sports

**Sports Data & UI Architecture**

축구, 야구, 농구 등 스포츠 종목별로 달라지는 경기·순위·통계 처리를 공통 추상화와 스포츠별 구현체로 분리한 프로젝트입니다.

**Key Points**
- `MatchesMgr` / `StandingsMgr` / `StatisticsMgr` 추상화
- 스포츠별 상속 및 Polymorphism
- `IndividualSports` / `ItemView` Interface
- `BaseGridFragment` UI 공통화
- 스포츠 추가를 고려한 확장 가능한 구조
- 공통 로직과 변경되는 정책의 분리

👉 **[Sports 상세 보기](./sports/)**

---

# 🧠 Core Skills

| Category | Skills |
|---|---|
| **Language** | Kotlin · Java |
| **Platform** | Android · Android TV |
| **UI** | Jetpack Compose · RecyclerView · Fragment · Material 3 |
| **TV UX** | D-pad · Focus · Focus Animation · TV Launcher UI |
| **Architecture** | ViewModel · StateFlow / Flow · Navigation · Hilt |
| **OOP / Design** | Interface · Abstract Class · Inheritance · Polymorphism |
| **Code Quality** | Abstraction · Reusability · Separation of Responsibility |
| **Maintenance** | 기존 코드 분석 · 기능 개선 · Bug Fix · UI 개선 |

---

# 🎯 Development Highlights

### Android TV UI

Android TV 환경에서 리모컨 기반 D-pad / Focus UX를 고려한 Launcher, Live, Profile 등의 UI를 구현했습니다.

### UI 공통화

반복되는 UI 동작을 Interface, Abstract Class, Base Component 등의 형태로 분리하여 재사용성을 높였습니다.

### 유지보수

기존 Android TV 코드베이스를 분석하고 기존 사용자 흐름을 유지하면서 필요한 기능과 UI를 개선했습니다.

### 복잡한 사용자 흐름

Home, Live, Setting, Profile과 같이 서로 연결되는 화면에서 Navigation과 상태를 연결하여 사용자 흐름을 구현했습니다.

### 확장 가능한 구조

스포츠 프로젝트에서는 공통 처리와 종목별 차이를 분리하고, Launcher 프로젝트에서는 공통 UI 계층을 추상화하여 확장 가능한 구조를 구성했습니다.

---

# 📂 Repository Structure

```text
portfolio/
│
├── README.md
│
├── formulerLauncher/
│   └── README.md
│
├── mol4/
│   └── README.md
│
├── plus/
│   └── README.md
│
└── sports/
    └── README.md
```

각 프로젝트는 별도의 README에서 담당 범위와 핵심 구현 내용을 확인할 수 있습니다.
