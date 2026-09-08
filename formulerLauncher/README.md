# FormulerLauncher — Android TV Launcher Portfolio

> Android TV / Set-top Box 환경을 위한 Launcher 프로젝트  
> **공통 기능을 추상화하고, 인터페이스·상속·컴포지션을 활용해 기능 확장과 코드 재사용을 고려한 구조**를 구현했습니다.

---

## 프로젝트 소개

**FormulerLauncher**는 Android TV 기반 Set-top Box에서 사용되는 Launcher 애플리케이션입니다.

주요 기능은 홈 화면 및 앱 목록, 최근 사용 콘텐츠, 즐겨찾기/앱 위치 관리, 앱 잠금, 네트워크 상태, Wallpaper, Setup Wizard, APK 설치, Hotkey, Control Center, 시스템 정보, 알림, HTTP/다운로드 처리 등입니다.

이 프로젝트에서 강조할 수 있는 핵심은 **공통 동작과 변경되는 동작을 분리하여 재사용 가능한 구조를 만든 것**입니다.

---

## Architecture

```text
                         FormulerLauncher
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
          UI / Grid          Manager          SetupWizard
              │                 │                 │
              ▼                 ▼                 ▼
       GridAdapter        Network / DB       Wi-Fi / Ethernet
              │                 │                 │
              ▼                 ▼                 ▼
         ItemView          BaseDao<T>      IEthernetManager
              │                 │                 │
              └─────────────────┼─────────────────┘
                                ▼
                     Common / Extensible Logic
```

### 핵심 설계

| 설계 포인트 | 적용 사례 |
|---|---|
| Interface | `ItemView`, `IEthernetManager`, `OnNetworkListener` |
| Abstract Class | `GridAdapter`, `GridAdapterContent`, `GridItemView`, `BaseConnection` |
| Inheritance | `GridAdapterContentApps`, `GridAdapterLockApps`, `PositionedRecentRowItem` |
| Generic | `BaseDao<T>`, `BaseAdapter<T>` |
| Composition | Adapter / NetworkManager / Connection 계층 |
| 역할 분리 | UI / Manager / DB / Network / SetupWizard |

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

**포인트:** 구현체에 직접 의존하기보다 Interface를 통해 역할을 정의했습니다.

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

`GridAdapter`가 Item 관리, ViewHolder 생성/Binding, Count, 입력 처리 등의 공통 흐름을 담당하고, 실제 Item View 선택은 하위 클래스가 결정합니다.

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

이 계층에서 Grid Focus 처리까지 공통화하고 실제 화면별 Adapter에서는 필요한 View만 선택합니다.

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

### Before / After

```text
[중복 구현]
Apps       ─ RecyclerView + Focus + Binding + View 생성
Lock Apps  ─ RecyclerView + Focus + Binding + View 생성

                ↓

[현재 구조]
GridAdapter        ─ RecyclerView 공통 처리
GridAdapterContent ─ Focus 공통 처리
각 Adapter         ─ 필요한 View / 정책만 구현
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

TV UI에서 반복되는 Item 저장, Focus Animation, Dim 처리 확장 지점을 부모 클래스에서 제공했습니다.

---

## 핵심 코드 ⑤ — Generic + Interface로 DAO 공통화

### `BaseDao<T>`

```kotlin
interface BaseDao<T> {

    @Delete
    fun delete(obj: T)

    @Insert
    fun insert(obj: T): Long

    @Insert
    fun insert(vararg obj: T)

    @Insert
    fun insert(obj: List<T>): List<Long>

    @Update
    fun update(obj: T)

    @Update
    fun update(obj: List<T>)
}
```

DAO마다 반복되는 CRUD를 `BaseDao<T>`로 추상화했습니다.

```text
BaseDao<T>
  ├─ insert
  ├─ update
  ├─ delete
  └─ query
       │
       ├── PositionedAppDao
       └── PositionedListDao
```

**Generic을 사용해 공통 기능은 하나로 관리하면서 사용하는 DAO의 타입 안정성도 유지했습니다.**

---

## 핵심 코드 ⑥ — DAO의 공통 + 특화 로직 분리

```kotlin
@Dao
abstract class PositionedAppDao : BaseDao<PositionedAppDaoItem> {

    @Query("SELECT * FROM ${PositionedAppDaoItem.TB_NAME}")
    abstract fun getAll(): List<PositionedAppDaoItem>

    @Query("DELETE FROM ${PositionedAppDaoItem.TB_NAME} WHERE package_name=:packageName")
    abstract fun delete(packageName: String)
}
```

기본 CRUD는 `BaseDao`가 담당하고, `PositionedAppDao`는 앱 위치/잠금이라는 도메인에 필요한 Query만 추가합니다.

---

## 핵심 코드 ⑦ — Entity 상속으로 데이터 모델 확장

```kotlin
open class PositionedListDaoItem(
    val id: Long,
    var name: String,
    val type: String,
    val groupId: Int?,
    position: Int
) : PositionedItem(position) {

    open val isFav = false
    open val isRecent = false

    open fun getDisplayName(): String? = name
}
```

`PositionedRecentRowItem`, `PositionedFavRowItem` 등은 공통 속성과 동작을 상속받고 각 타입의 의미에 필요한 부분만 Override합니다.

```text
PositionedListDaoItem
       │
       ├── PositionedRecentRowItem
       │     └── isRecent / recentName
       │
       └── PositionedFavRowItem
             └── isFav
```

---

## 핵심 코드 ⑧ — Network Interface / 구현체 분리

### `IEthernetManager`

```kotlin
interface IEthernetManager {
    fun init(context: Context)
    fun release(context: Context)
    fun getEthernetType(): EthernetType
    fun getInterfaceName(): String
    fun enableEthernet(enable: Boolean)
    fun setEthernetListener(listener: OnEthernetListener)
    fun startDhcp()
    fun startManual(ipInfo: IpInfo)
}
```

실제 구현은 `EthernetManagerImpl`이 담당합니다.

```text
Client / UI
    │
    ▼
IEthernetManager
    │
    ▼
EthernetManagerImpl
    │
    ├─ Android Ethernet API
    ├─ ConnectivityManager
    └─ Network Callback
```

**사용하는 쪽은 구현체가 아니라 Interface를 바라보도록 설계**했습니다.

---

## 핵심 코드 ⑨ — Network 이벤트 처리

`NetworkManager`는 BroadcastReceiver, HandlerThread, Handler, InternetChecker, EthernetManager 등을 조합하여 Android TV / Set-top Box 환경의 네트워크 상태 변화를 관리합니다.

네트워크 이벤트가 짧은 시간에 반복될 수 있기 때문에 이전 메시지를 제거하고 Delay 후 마지막 상태를 반영하는 방식으로 이벤트를 제어합니다.

```text
Network Event
     ↓
BroadcastReceiver
     ↓
remove previous message
     ↓
delayed update
     ↓
NetworkHandler
     ↓
Listener callback
```

---

## 핵심 코드 ⑩ — HTTP 연결 계층 추상화

### `BaseConnection`

```kotlin
internal abstract class BaseConnection(
    val stringUrl: String,
    val allowUnverified: Boolean
) {
    abstract fun getInputStream(): InputStream?
    abstract fun requestHeader(key: String, value: String): Int
    abstract fun release()
}
```

구체 구현은 `OkHttpClientImpl`이 담당합니다. HTTP 연결 방식과 사용하는 곳을 분리하여 연결 계층을 추상화했습니다.

---

## 핵심 코드 ⑪ — Downloader 공통 흐름 + 구현 분리

```text
CONNECTING
    ↓
CONNECT_SUCCESS
    ↓
DOWNLOADING
    ├── SUCCESS
    ├── FAIL
    └── CANCELED
```

`BaseDownloader`가 공통 상태와 흐름을 관리하고, `Ok3Downloader`가 실제 OkHttp 기반 연결을 구현하는 방식으로 역할을 분리했습니다.

---

# 내가 기여한 부분

## ① 공통 코드의 추상화

- `GridAdapter`
- `GridAdapterContent`
- `GridItemView`
- `BaseDao<T>`
- `BaseConnection`
- `BaseDownloader`

등 여러 화면/기능에서 반복될 수 있는 기능을 공통 계층으로 분리했습니다.

## ② Interface를 통한 역할 계약 정의

- `ItemView`
- `IEthernetManager`
- `OnNetworkListener`
- `OnEthernetListener`
- `OnFocusSearch`
- `HomeRowItem`

등을 활용해 구현체와 사용하는 쪽의 결합도를 낮췄습니다.

## ③ 상속을 이용한 기능 확장

공통 동작은 부모에서 제공하고 실제 화면/도메인별 차이는 자식 클래스에서 구현했습니다.

## ④ Generic을 활용한 재사용

`BaseDao<T>`, `BaseAdapter<T>`처럼 타입별 동일 기능을 Generic으로 공통화했습니다.

## ⑤ Android TV 환경을 고려한 이벤트 처리

Focus, D-pad 입력, Key event, Long click, Network event, HandlerThread, BroadcastReceiver 등을 조합해 TV 환경에 필요한 UX와 시스템 동작을 구현했습니다.

---

# 기술적 성과

### Architecture / OOP

- Interface 기반 역할 설계
- Abstract Class 기반 공통 로직 재사용
- 상속과 Override를 통한 기능 확장
- Generic 기반 타입 공통화
- Composition을 통한 Manager 간 책임 분리

### Code Quality

- RecyclerView 처리 로직 중복 최소화
- DAO CRUD 공통화
- 공통 View 동작 중앙화
- HTTP / Download 공통 흐름 추상화
- 화면별 구현에서는 변경되는 부분에 집중

### Android TV

- Leanback 기반 TV UI
- D-pad Focus 중심 UX
- Focus Animation
- Launcher / Set-top Box 환경 대응
- Wi-Fi / Ethernet 관리
- APK Installer / Setup Wizard

---

# Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin, Java |
| Platform | Android TV / Set-top Box |
| UI | Android View, RecyclerView, Leanback |
| Architecture | Interface / Abstract Class / Inheritance / Composition |
| Database | Room |
| Network | OkHttp |
| Async | Coroutine / Handler / HandlerThread |
| System | ConnectivityManager / Ethernet / PackageManager |
| Image | Glide |
| Firebase | Firebase |

---

# Portfolio Summary

> **FormulerLauncher는 Android TV Launcher라는 복잡한 시스템 환경에서 공통 기능과 변경되는 기능을 분리하고, Interface / Abstract Class / Inheritance / Generic을 활용해 재사용 가능한 구조를 구현한 프로젝트입니다.**

이 프로젝트를 통해 **기능을 구현하는 것에 그치지 않고, 반복되는 문제를 공통화하고 변경되는 부분을 분리하여 새로운 기능이 추가되어도 기존 코드를 최대한 재사용할 수 있도록 설계하는 개발 역량**을 보여줄 수 있습니다.
