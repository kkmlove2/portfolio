# FormulerLauncher — Android TV Launcher Portfolio

> Android TV / Set-top Box Launcher
> **Interface / Abstract Class / Inheritance / UI 코드 재사용 / Android TV UX**

---

## 프로젝트 소개

**FormulerLauncher**는 Android TV 기반 Set-top Box에서 사용되는 Launcher 애플리케이션입니다.

홈 화면과 앱 Grid UI를 중심으로 **D-pad 입력, Focus 처리, Focus Animation, Item View 재사용**을 고려한 UI 구조를 구현했습니다.

이 프로젝트에서 강조할 수 있는 핵심은 **공통 UI 동작과 화면별 차이를 분리하여 재사용 가능한 구조를 만든 것**입니다.

---

## Architecture

```text
                    FormulerLauncher
                           │
                           ▼
                       UI / Grid
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        GridAdapter   GridItemView   ItemView
              │
              ▼
     GridAdapterContent
          │         │
          ▼         ▼
       Apps      Lock Apps
```

### 핵심 설계

| 설계 포인트 | 적용 사례 |
|---|---|
| Interface | `ItemView` |
| Abstract Class | `GridAdapter`, `GridAdapterContent`, `GridItemView` |
| Inheritance | `GridAdapterContentApps`, `GridAdapterLockApps` |
| UI Reuse | RecyclerView / Item Binding / Focus 처리 공통화 |
| Android TV UX | D-pad / Focus / Focus Animation |

---

## 핵심 코드 ① — Interface로 UI Item 계약 정의

### `ItemView`

```kotlin
interface ItemView {
    fun onItemSet(item: Any)
}
```

Adapter와 실제 View 사이에 최소한의 계약을 정의하고, 여러 Grid Item View가 동일한 방식으로 데이터를 전달받도록 구성했습니다.

```text
GridAdapter
     │
     ▼
  ItemView
   │  │  │
   ▼  ▼  ▼
 AppView / LargeView / CustomView
```

**포인트:** 구현체에 직접 의존하기보다 Interface를 통해 UI Item의 역할을 정의했습니다.

---

## 핵심 코드 ② — Abstract Class로 RecyclerView 공통 로직 재사용

### `GridAdapter`

```kotlin
abstract class GridAdapter :
    RecyclerView.Adapter<GridAdapter.ViewHolder>() {

    open val items = ArrayList<Any>()

    abstract fun getItemView(context: Context): View

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.item = getItem(position).also {
            (holder.itemView as ItemView).onItemSet(it)
        }
    }

    override fun getItemCount(): Int = items.size
}
```

`GridAdapter`가 Grid UI에서 반복되는 Item 관리와 Binding 흐름을 담당하고, 실제 Item View 선택은 하위 클래스가 결정합니다.

```text
공통 알고리즘
     │
     ▼
GridAdapter
     │
     └── getItemView()
             │
       ┌─────┴─────┐
       ▼           ▼
   App View     Lock View
```

**공통 알고리즘은 부모가 가지고, 변경되는 View만 자식이 결정하는 구조**입니다.

---

## 핵심 코드 ③ — 상속을 이용한 Adapter 확장

### `GridAdapterContent`

```kotlin
abstract class GridAdapterContent : GridAdapter() {

    override fun onCreateItemView(
        parent: ViewGroup,
        viewType: Int
    ): View {
        return getItemView(parent.context).also { view ->
            view.onFocusChangeListener =
                View.OnFocusChangeListener { v, hasFocus ->
                    (v as GridItemView).updateFocusedView(hasFocus)
                }
        }
    }
}
```

`GridAdapterContent`에서 Android TV Grid의 Focus 처리를 공통화하고, 실제 화면별 Adapter에서는 필요한 View만 선택합니다.

```text
GridAdapter
     │
     ▼
GridAdapterContent
     │
     ├── GridAdapterContentApps
     │       └── GridItemViewAppLarge
     │
     └── GridAdapterLockApps
             └── GridItemViewApp
```

### `GridAdapterContentApps`

```kotlin
class GridAdapterContentApps : GridAdapterContent() {
    override fun getItemView(context: Context): View {
        return GridItemViewAppLarge(context)
    }
}
```

### `GridAdapterLockApps`

```kotlin
class GridAdapterLockApps : GridAdapterContent() {
    override fun getItemView(context: Context): View {
        return GridItemViewApp(context)
    }
}
```

### Before / After

```text
[중복 구현]
Apps       ─ RecyclerView + Focus + Binding + View 생성
Lock Apps  ─ RecyclerView + Focus + Binding + View 생성

                ↓

[현재 구조]
GridAdapter        ─ RecyclerView 공통 처리
GridAdapterContent ─ Focus 공통 처리
각 Adapter         ─ 필요한 View만 선택
```

---

## 핵심 코드 ④ — 공통 View 동작도 Abstract Class로 재사용

### `GridItemView`

```kotlin
abstract class GridItemView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ItemView {

    private var mItem: Any? = null

    open fun updateFocusedView(focus: Boolean) {
        // Android TV Focus Animation 공통 처리
    }

    fun setItem(item: Any?) {
        mItem = item
    }

    fun getItem(): Any? = mItem

    open fun updateDim(show: Boolean) {}
}
```

TV UI에서 반복되는 Item 저장과 Focus Animation 동작, Dim 처리의 확장 지점을 부모 클래스에서 제공합니다.

---

# 내가 기여한 부분

## ① Grid UI 공통 코드 추상화

- `GridAdapter`를 통한 RecyclerView 공통 처리
- `GridAdapterContent`를 통한 Focus 처리 공통화
- `GridItemView`를 통한 Item View 공통 동작 정의

## ② Interface를 통한 역할 계약 정의

- `ItemView`를 통해 Adapter와 Item View 사이의 공통 계약 정의
- 서로 다른 Item View 구현체에서도 동일한 방식으로 Item 전달 처리

## ③ 상속을 이용한 기능 확장

공통 동작은 부모 클래스에서 제공하고, Apps / Lock Apps처럼 화면별 차이는 자식 Adapter에서 필요한 View만 선택하도록 구성했습니다.

## ④ Android TV UX 구현

- D-pad 기반 Focus 이동을 고려한 Grid UI
- Focus 상태 변화 처리
- Focus Animation 공통화
- TV 화면에서 반복되는 Item UI 동작 재사용

---

# 기술적 성과

### Architecture / OOP

- Interface 기반 역할 설계
- Abstract Class 기반 공통 UI 로직 재사용
- 상속과 Override를 통한 화면별 확장
- 부모 클래스와 자식 클래스의 책임 분리

### Code Quality

- RecyclerView 처리 로직 중복 최소화
- Focus 처리 로직 공통화
- 공통 View 동작 중앙화
- 새로운 Grid Item View 추가 시 기존 공통 로직 재사용

### Android TV

- Android TV / Set-top Box Launcher UI
- D-pad Focus 중심 UX
- Focus Animation
- Grid 기반 앱 UI
- TV 환경에 맞춘 화면 탐색 경험

---

# Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin, Java |
| Platform | Android TV / Set-top Box |
| UI | Android View, RecyclerView, Leanback |
| Architecture | Interface / Abstract Class / Inheritance |
| UX | D-pad / Focus / Focus Animation |
