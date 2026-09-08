# 🏟️ Sports Data & UI Architecture

> **스포츠 종목별 차이를 확장 가능한 구조로 분리하고, 공통 로직은 재사용하는 Android/Kotlin 프로젝트**

<div align="center">

**Abstraction · Reusability · Polymorphism · Extensibility**

</div>

---

## 📌 프로젝트에서 보여주고 싶은 핵심 역량

이 프로젝트에서는 스포츠 종목마다 반복되는 처리 로직을 무작정 복사하지 않고, **공통적인 책임과 변경되는 책임을 구분하여 추상화**하는 것을 중심으로 설계했습니다.

| 역량 | 적용 사례 |
|---|---|
| **Abstract Class** | `MatchesMgr`, `StandingsMgr`, `StatisticsMgr` |
| **Inheritance / Polymorphism** | 스포츠별 Manager 구현체 |
| **Interface** | `IndividualSports`, `ItemView` |
| **Code Reusability** | 공통 경기/순위/통계/UI 처리 |
| **Extensibility** | 스포츠 종목별 구현체 추가 |
| **UI 공통화** | `BaseGridFragment` |

---

# 🏗️ 01. Architecture Structure

```text
                         ┌──────────────────────┐
                         │     Sports UI         │
                         │ Fragment / ItemView   │
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┼────────────────┐
                    │               │                │
                    ▼               ▼                ▼
              MatchesMgr      StandingsMgr     StatisticsMgr
                    │               │                │
          ┌─────────┼───────┐   ┌───┼───────┐    ┌───┼────────┐
          ▼         ▼       ▼   ▼   ▼       ▼    ▼   ▼        ▼
      Football  Baseball  ... Football ...  ... Football  ...
          │         │       │
          └─────────┴───────┘
                    │
             공통 처리 재사용
                    │
             종목별 차이만 구현
```

### UI 공통화

```text
                       BaseGridFragment
                              │
          ┌───────────┬───────┼────────┬────────────┐
          ▼           ▼       ▼        ▼            ▼
       Sport       National   Root    League     Statistics
      Fragment     Fragment Fragment Fragment    Fragment
```

여러 화면에서 반복되는 Loading / Empty / Error / Focus 처리 등을 Base Fragment에서 재사용하도록 구성했습니다.

---

# 🔥 02. Core Case — `MatchesMgr`

## 문제 상황

스포츠마다 경기 데이터를 처리하는 기본 흐름은 유사하지만 경기 시간, 쿼터/피리어드, 스코어 표현, 경기 통계 등은 서로 다릅니다.

각 스포츠 구현체에서 전체 로직을 반복한다면 공통 코드가 여러 곳에 복제될 수 있습니다.

## 해결

`MatchesMgr`를 공통 추상화로 두고 **공통 처리와 스포츠별 변경점을 분리**했습니다.

### 실제 프로젝트 코드 일부

```kotlin
abstract class MatchesMgr : TeamDependencyMgr() {
    // 공통 경기 데이터 처리 및 스포츠별 확장 지점
    // 프로젝트의 실제 구현에서는 종목별 차이를 하위 클래스가 오버라이딩
}
```

스포츠별 구현체는 `MatchesMgr`를 상속합니다.

```kotlin
class MatchesMgrFootball : MatchesMgr() {
    // Football에 특화된 경기 처리
}
```

### 구조

```text
                 MatchesMgr
                     │
       ┌─────────────┼──────────────┐
       ▼             ▼              ▼
   Football       Baseball      Basketball
       │             │              │
       └──── 공통 흐름 재사용 ───────┘
                     │
             스포츠별 차이 구현
```

### 설계 포인트

**공통적인 알고리즘은 상위 클래스에서 재사용하고, 스포츠별로 달라지는 부분은 하위 클래스가 구현합니다.**

따라서 스포츠가 추가될 때 기존 공통 코드를 그대로 활용하면서 해당 종목에 필요한 부분만 구현할 수 있습니다.

---

# 📊 03. Core Case — `StandingsMgr`

순위 데이터 역시 공통 처리 흐름은 존재하지만 스포츠별로 표시해야 하는 통계와 순위 기준에는 차이가 있습니다.

### 실제 프로젝트 코드 일부

```kotlin
abstract fun getDrawType()

abstract fun getStandingsStats(
    value: TeamSportsModel.TableRoundListTableValues
): ArrayList<String>

abstract fun getStandingsHeaderType(): ArrayList<String>
```

## 구조

```text
                       StandingsMgr
                            │
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
        Football        Baseball       Basketball
            │               │               │
            └────── 공통 순위 처리 ──────────┘
                            │
                    종목별 정책 구현
```

### 설계 포인트

`StandingsMgr`는 **공통 상태와 기본 처리 로직을 공유하면서도 스포츠별로 달라지는 정책을 추상 메서드로 분리**합니다.

즉, 단순한 상속이 아니라 **공통 알고리즘과 변경되는 정책을 분리하는 구조**로 활용했습니다.

---

# 📈 04. Core Case — `StatisticsMgr`

통계 영역에서도 같은 설계 방향을 일관되게 적용했습니다.

```text
                      StatisticsMgr
                            │
       ┌─────────┬──────────┼──────────┬─────────┐
       ▼         ▼          ▼          ▼         ▼
   Football  Baseball  Basketball   Cricket     IH
```

### 대표 구현체

```text
StatisticsMgrFootball
StatisticsMgrBaseball
StatisticsMgrBasketball
StatisticsMgrCricket
StatisticsMgrAM
StatisticsMgrAS
StatisticsMgrIH
```

공통 통계 처리 기능은 부모 클래스에서 제공하고, 스포츠별로 다른 데이터 표현이나 통계 항목은 각각의 구현체에서 처리하도록 구성했습니다.

### 설계 효과

동일한 통계 처리 구조를 여러 스포츠에서 재사용하면서도, 스포츠별 특수성을 독립적으로 구현할 수 있습니다.

---

# 🔌 05. Interface Case — `IndividualSports`

### 실제 프로젝트 코드 일부

```kotlin
interface IndividualSports {
    // 개별 스포츠가 따라야 하는 공통 역할을 정의
}
```

구현체:

```kotlin
class Golf : IndividualSports {
    // Golf 구현
}

class MotorSports : IndividualSports {
    // MotorSports 구현
}
```

### 구조

```text
                 IndividualSports
                        │
              ┌─────────┴─────────┐
              ▼                   ▼
            Golf             MotorSports
```

### 왜 Interface인가?

이 영역에서는 부모 클래스의 공통 구현을 공유하는 것보다, 서로 다른 구현체가 동일한 역할을 수행한다는 **계약(contract)을 정의하는 것**에 초점을 두었습니다.

따라서 공통 구현이 필요한 `MatchesMgr` 계열과 달리 Interface를 사용하여 역할과 구현을 분리했습니다.

---

# 🧩 06. Interface Case — `ItemView`

UI 컴포넌트에서도 공통된 역할을 Interface로 추상화했습니다.

```text
                          ItemView
                             │
       ┌───────────┬─────────┼───────────┐
       ▼           ▼         ▼           ▼
     Match      Standings  Statistics  Competition
```

서로 다른 Item View가 동일한 계약을 구현하도록 하여, 사용하는 측에서는 구체적인 View 타입보다 **공통된 역할을 기준으로 처리**할 수 있도록 했습니다.

---

# 🖥️ 07. UI Reusability — `BaseGridFragment`

여러 화면에서 반복적으로 사용되는 Fragment 동작은 `BaseGridFragment`에서 공통으로 처리하도록 구성했습니다.

### 구조

```text
                    BaseGridFragment
                           │
        ┌──────────┬───────┼────────┬────────────┐
        ▼          ▼       ▼        ▼            ▼
     Sport      National  Root     League     Statistics
    Fragment    Fragment Fragment Fragment    Fragment
```

### 공통적으로 재사용되는 대표 동작

- Loading 상태
- Empty 상태
- Load 실패 상태
- Load 성공 상태
- Focus 탐색

특히 TV 환경의 Android UI에서 화면마다 반복될 수 있는 Focus 처리와 상태 처리를 공통화하여 각 Fragment의 역할을 단순화했습니다.

---

# 🔄 08. Before / After

## Before — 공통 로직을 각 스포츠에서 반복 구현하는 경우

```text
MatchesFootball
 ├── 경기 조회
 ├── 공통 데이터 처리
 ├── 시간 처리
 └── 스코어 처리

MatchesBaseball
 ├── 경기 조회
 ├── 공통 데이터 처리  ← 중복
 ├── 시간 처리
 └── 스코어 처리

MatchesBasketball
 ├── 경기 조회
 ├── 공통 데이터 처리  ← 중복
 ├── 시간 처리
 └── 스코어 처리
```

## After — 공통 흐름과 변경점을 분리

```text
                         MatchesMgr
                              │
                    ┌─────────┴─────────┐
                    │                   │
              공통 처리 로직        변경 지점
                    │                   │
                    │          ┌────────┼────────┐
                    │          ▼        ▼        ▼
                    │      Football  Baseball Basketball
                    │
                    └────── 재사용 ──────┘
```

> **모든 것을 공통화한 것이 아니라, 공통인 부분과 스포츠별로 달라지는 부분을 구분했습니다.**

---

# 👨‍💻 09. My Contribution

### ① 스포츠 도메인의 공통 책임 추상화

`MatchesMgr`, `StandingsMgr`, `StatisticsMgr`를 중심으로 스포츠별 기능에서 반복될 수 있는 공통 처리 로직을 상위 추상화에 구성했습니다.

### ② 스포츠별 변경점 분리

각 스포츠의 특성에 따라 달라지는 경기 시간, 스코어, 순위 통계, 통계 데이터 등의 처리를 개별 구현체에서 담당하도록 분리했습니다.

### ③ Interface를 통한 역할 정의

`IndividualSports`, `ItemView`와 같이 서로 다른 구현체가 동일한 역할을 수행해야 하는 영역은 Interface를 활용하여 구현과 역할을 분리했습니다.

### ④ UI 공통 로직 재사용

`BaseGridFragment`를 통해 여러 화면에서 반복되는 상태 처리 및 Focus 관련 동작을 공통화했습니다.

### ⑤ 확장 가능한 구조를 고려한 구현

새로운 스포츠 종목이 추가될 때 기존 공통 로직을 재사용하고, 종목에 특화된 부분만 구현할 수 있도록 구조를 구성했습니다.

---

# 🏆 10. Technical Achievements

## 코드 중복 최소화

스포츠별로 반복되는 공통 로직을 상위 추상화에 모아 여러 구현체에서 재사용할 수 있도록 구성했습니다.

## 변경 범위 최소화

스포츠별 규칙을 개별 구현체로 분리하여 특정 스포츠의 변경이 다른 스포츠 구현에 직접 영향을 주는 범위를 줄였습니다.

## 확장성

새로운 스포츠 종목을 추가할 때 기존 공통 처리 구조를 활용하고 필요한 차이점만 구현할 수 있는 구조를 만들었습니다.

## 역할과 구현의 분리

Interface를 활용하여 사용하는 코드가 구체적인 구현보다 추상화된 역할을 기준으로 동작할 수 있도록 했습니다.

## UI 재사용성

여러 화면에서 공통적으로 필요한 Loading, Empty, Error, Focus 등의 처리를 Base Fragment에서 재사용하도록 구성했습니다.

---

# 🎯 11. Portfolio Summary

> ### **스포츠 종목별 차이를 추상화하여 중복 코드를 줄이고 확장 가능한 구조를 설계했습니다.**
>
> 스포츠 서비스 특성상 경기, 순위, 통계 데이터의 기본적인 처리 흐름은 유사하지만 세부 규칙은 종목마다 달라집니다.
>
> 이를 `MatchesMgr`, `StandingsMgr`, `StatisticsMgr` 등의 추상 클래스를 중심으로 공통 로직과 종목별 변경점을 분리하고, 스포츠별 구현체에서 필요한 차이만 확장하도록 구성했습니다.
>
> 또한 `IndividualSports`, `ItemView`와 같이 공통 구현보다 역할에 대한 계약이 중요한 영역에서는 Interface를 활용하여 구현과 역할을 분리했습니다.
>
> 이를 통해 **코드 재사용성, 종목별 독립성, 확장성**을 높이고, 새로운 스포츠가 추가되더라도 기존 공통 로직을 최대한 활용할 수 있는 구조를 구현했습니다.

---

## 🛠️ Tech Stack

`Kotlin` · `Android` · `Interface` · `Abstract Class` · `Inheritance` · `Polymorphism` · `Abstraction` · `Code Reusability` · `Extensibility`

---

## 💡 Key Takeaway

```text
단순히 "상속을 사용했다"
            ↓
공통 책임과 변경 책임을 분리했다
            ↓
공통 로직은 재사용하고
종목별 차이는 독립적으로 구현했다
            ↓
새로운 종목을 추가할 수 있는 확장 가능한 구조
```

> **어떤 문법을 사용했는가보다 왜 그 구조가 필요한가를 기준으로 설계했습니다.**
