# 👨‍💻 Android Developer Portfolio

> **Android / Android TV 환경에서 공통 로직을 추상화하고, 재사용성과 확장성을 고려해 개발해온 프로젝트 포트폴리오입니다.**

---

## 🚀 Projects

### 📺 FormulerLauncher

**Android TV / Set-top Box Launcher**

Android TV Launcher 환경에서 Interface, Abstract Class, Inheritance, Generic 등을 활용해 공통 기능을 재사용하고 기능별 책임을 분리한 프로젝트입니다.

**Key Points**
- Interface 기반 역할 분리
- Abstract Class 기반 공통 로직 재사용
- 상속 / Override를 통한 기능 확장
- Generic 기반 DAO / Adapter 공통화
- Android TV Focus / D-pad UX
- Network / Ethernet / Setup Wizard

👉 **[FormulerLauncher 상세 보기](./formulerLauncher/)**

---

### 🏟️ Sports

**Sports Data & UI Application**

축구, 야구, 농구 등 스포츠 종목별로 달라지는 경기·순위·통계 처리를 공통 추상화와 스포츠별 구현체로 분리한 프로젝트입니다.

**Key Points**
- `MatchesMgr` / `StandingsMgr` / `StatisticsMgr` 추상화
- 스포츠별 상속 및 Polymorphism
- `IndividualSports` / `ItemView` Interface
- `BaseGridFragment`를 통한 UI 공통화
- 스포츠 추가를 고려한 확장 가능한 구조
- 공통 로직과 변경되는 정책의 분리

👉 **[Sports 상세 보기](./sports/)**

---

## 🧠 Core Skills

| Category | Skills |
|---|---|
| **OOP** | Interface · Abstract Class · Inheritance · Polymorphism |
| **Design** | Abstraction · Composition · Separation of Responsibility |
| **Reuse** | Generic · Common Component · Base Class |
| **Android** | Kotlin · Java · RecyclerView · Fragment |
| **Android TV** | Leanback · D-pad · Focus UI · Launcher |
| **Data** | Room · DAO · Generic DAO |
| **Network** | OkHttp · Network State · Ethernet / Wi-Fi |

---

## 🎯 Development Philosophy

### 01. 공통인 것은 한 번만 구현한다

여러 기능에서 반복되는 로직은 공통 계층으로 올려 중복 구현을 줄입니다.

### 02. 변경되는 부분은 분리한다

스포츠별 규칙이나 화면별 차이처럼 변경 가능성이 높은 부분은 Interface, Abstract Method, 하위 구현체 등으로 분리합니다.

### 03. 구현보다 역할을 먼저 정의한다

필요한 경우 Interface를 활용하여 사용하는 코드가 구체적인 구현체에 직접 의존하지 않도록 합니다.

### 04. 확장을 기존 코드의 복사로 해결하지 않는다

새로운 기능이나 스포츠가 추가될 때 기존 공통 로직을 재사용하고, 필요한 차이점만 구현할 수 있는 구조를 지향합니다.

---

## 📂 Repository Structure

```text
portfolio/
│
├── README.md
│
├── formulerLauncher/
│   └── README.md
│
├── sports/
│   └── README.md
│
└── ...
```

각 프로젝트는 별도의 README에서 **Architecture → 핵심 코드 → Before / After → 기여 내용 → 기술적 성과** 순서로 자세하게 확인할 수 있습니다.

---

## 📌 Portfolio Summary

**단순히 기능을 구현하는 것을 넘어, 반복되는 문제를 공통화하고 변경되는 부분을 분리하여 유지보수성과 확장성을 고려한 Android 개발을 지향합니다.**
