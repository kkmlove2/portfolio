# Android TV Launcher — 코드 아키텍처 하이라이트

> 대상: Android TV(Leanback) 런처 — 홈 화면의 Row/App 배치, 최근 시청 이력, 앱 설치·관리,
> 셋업 위저드, 네트워크·펌웨어 업데이트까지 담당하는 셋톱박스 시스템 앱 (Kotlin / Java 230여 파일).
>
> 이 문서는 그중 **설계 의도가 드러나는 코드**만 추려, “왜 이렇게 짰는가”와 함께 정리한 것이다.

---

## 목차

1. [Room 기반 데이터 계층 — 제네릭 DAO 추상화](#1-room-기반-데이터-계층--제네릭-dao-추상화)
2. [Entity 상속으로 표현한 도메인 다형성](#2-entity-상속으로-표현한-도메인-다형성)
3. [DB 초기 시드 & 마이그레이션 — 코드로 관리되는 스키마 진화](#3-db-초기-시드--마이그레이션--코드로-관리되는-스키마-진화)
4. [Mutex 공유를 통한 다중 매니저 간 동시성 제어](#4-mutex-공유를-통한-다중-매니저-간-동시성-제어)
5. [position 정합성 자가 복구 로직](#5-position-정합성-자가-복구-로직)
6. [드래그 앤 드롭 재정렬 — O(변경분)만 DB에 반영](#6-드래그-앤-드롭-재정렬--o변경분만-db에-반영)
7. [RecyclerView Adapter 계층 — Template Method 패턴](#7-recyclerview-adapter-계층--template-method-패턴)
8. [HTTP 계층 — 구현체 교체 가능한 전략 구조](#8-http-계층--구현체-교체-가능한-전략-구조)
9. [도메인 enum + 컴파일타임 타입 안전성](#9-도메인-enum--컴파일타임-타입-안전성)
10. [UI 프레임워크화 — 추상 Fragment + Fluent Builder](#10-ui-프레임워크화--추상-fragment--fluent-builder)
11. [하드웨어 의존성 인터페이스 분리](#11-하드웨어-의존성-인터페이스-분리)
12. [Hilt DI + ViewModel 생명주기 관리](#12-hilt-di--viewmodel-생명주기-관리)
13. [기능 단위 수직 분할 — Hotkey 화면의 MVVM 구성](#13-기능-단위-수직-분할--hotkey-화면의-mvvm-구성)
14. [콜백 스레드를 호출자가 정하는 인터넷 감시자](#14-콜백-스레드를-호출자가-정하는-인터넷-감시자)
15. [원격 진단 설계 — 로그 파사드와 필드 탈출구](#15-원격-진단-설계--로그-파사드와-필드-탈출구)
16. [기기 능력에 따른 기능 축소 — 데이터로 조립하는 설정 메뉴](#16-기기-능력에-따른-기능-축소--데이터로-조립하는-설정-메뉴)
17. [이력서용 요약](#17-이력서용-요약)

---

## 1. Room 기반 데이터 계층 — 제네릭 DAO 추상화

`main/positioned/BaseDao.kt`

```kotlin
interface BaseDao<T> {
    @Delete fun delete(obj: T)
    @Insert fun insert(obj: T): Long
    @Insert fun insert(vararg obj: T)
    @Insert fun insert(obj: List<T>): List<Long>
    @RawQuery fun query(query: SupportSQLiteQuery): Cursor
    @Update fun update(obj: T)
    @Update fun update(obj: List<T>)
}
```

**설계 포인트**

- Room의 `@Insert` / `@Update` / `@Delete`는 **타입 파라미터를 그대로 받아들인다**는 점을 이용해,
  CRUD 보일러플레이트를 제네릭 인터페이스 하나로 흡수했다.
  `PositionedListDao`, `PositionedAppDao` 모두 `BaseDao<T>`를 상속받고 **테이블 고유 쿼리만** 선언한다.
- 보일러플레이트 절약보다 큰 이득은 **DAO 자체를 제네릭 타입으로 주고받을 수 있게 된다**는 점이다.
  아래 5번의 `resetPositionIfInvalidLocked(list, dao: BaseDao<T>)`가 정확히 그 사례로,
  "리스트 Row"와 "앱 아이템" 두 종류의 테이블을 **하나의 복구 알고리즘**으로 처리한다.

---

## 2. Entity 상속으로 표현한 도메인 다형성

`main/positioned/PositionedItem.kt`, `PositionedListDao.kt`

```kotlin
@Keep @Entity
abstract class PositionedItem(
    @ColumnInfo(name = "position") var position: Int
)
```

```kotlin
@Keep @Entity
open class PositionedListDaoItem(
    @PrimaryKey(autoGenerate = false) val id: Long,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "group_id") val groupId: Int?,
    position: Int
) : PositionedItem(position) {

    @Ignore open val isFav = false
    @Ignore open val isRecent = false
    @Ignore open val items: ArrayList<out HomeRowItem> = arrayListOf()

    open fun getDisplayName(): String? = name
}

/** 최근 시청 Row — 표시 이름이 DB가 아니라 런타임 provider에서 온다 */
class PositionedRecentRowItem(
    id: Long, name: String, type: String, groupId: Int?, position: Int,
    val recentType: RecentType,
    var recentName: String? = null
) : PositionedListDaoItem(id, name, type, groupId, position) {

    override val items: ArrayList<RecentCardItem> = arrayListOf()
    override val isRecent: Boolean get() = true

    override fun getDisplayName(): String? = recentName   // ← 이름 해석 규칙만 재정의

    fun setRecent(recentRowItem: RecentRowItem) {
        items.clear()
        items.addAll(recentRowItem.cardItems)
        recentName = recentRowItem.name
    }
}

/** 즐겨찾기 Row — 자식이 DB에 영속되는 앱 아이템 */
class PositionedFavRowItem(...) : PositionedListDaoItem(...) {
    override val items: ArrayList<PositionedAppDaoItem> = arrayListOf()
    override val isFav: Boolean get() = true
}
```

**설계 포인트**

- 홈 화면의 Row는 **성격이 전혀 다른 두 종류**다.
  - *즐겨찾기 Row* — 사용자가 편집하고 DB에 영속됨
  - *최근시청 Row* — 다른 앱(ContentProvider)이 소유, 런처는 **참조만** 하고 내용은 런타임에 채움
- 이 차이를 `type` 컬럼 분기(`if (type == "FAV") ... else ...`)로 코드 전반에 흩뿌리는 대신,
  **하나의 테이블 + 상속 계층**으로 모델링했다.
  `@Ignore`를 붙인 `items` / `isFav` / `isRecent`는 **DB 컬럼이 아닌 도메인 상태**로,
  Room 스키마를 오염시키지 않으면서 다형성을 얻는다.
- 결과적으로 UI 계층은 `getDisplayName()` 하나만 호출하면 되고,
  "최근 Row의 이름은 provider에서 온다"는 사실은 자식 클래스 안에 갇힌다.

`PositionedAppDaoItem`은 여기에 더해 **의사(pseudo) 아이템**을 팩토리로 표현한다.

```kotlin
companion object {
    const val LOCKER_PACKAGE_NAME = "positioned_app_dao_locker"
    private const val ADD_FAV_ITEM_PACKAGE_NAME = "add_fav_item_package_name"

    /** "+ 즐겨찾기 추가" 타일 — DB에 저장되지 않는 UI 전용 sentinel */
    fun getAddFavItem(rowId: Long, title: String) = PositionedAppDaoItem(
        id = -1, listId = rowId,
        packageName = ADD_FAV_ITEM_PACKAGE_NAME, position = -1
    ).apply { installedAppInfo = InstalledAppInfo().apply { this.title = title } }
}

fun isAddFavItem() = packageName == ADD_FAV_ITEM_PACKAGE_NAME
fun isLocker()     = packageName == LOCKER_PACKAGE_NAME
```

> 어댑터가 `viewType` 분기 없이 동일한 리스트를 그릴 수 있도록,
> "추가 버튼"조차 **같은 도메인 타입**으로 통일하고 판별은 캡슐화된 메서드로 노출.

---

## 3. DB 초기 시드 & 마이그레이션 — 코드로 관리되는 스키마 진화

`main/positioned/PositionedAppMgr.kt`

```kotlin
private val db = Room.databaseBuilder(context, PositionedListDb::class.java, PositionedListDb.DB_NAME)
    .addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            // 최초 설치 시 기본 즐겨찾기 Row 생성
            db.insert(PositionedListDaoItem.TB_NAME, SQLiteDatabase.CONFLICT_REPLACE,
                ContentValues().apply {
                    put("id", RESERVED_LIST_ID_FAV)
                    put("name", RESERVED_LIST_NAME_FAV)
                    put("type", DB_FAV_ROW_TYPE)
                    put("position", RESERVED_LIST_POSITION_FAV)
                })

            // 기본 탑재 앱을 순서대로 시드
            DEFAULT_FAV_ROW_PACKAGE_NAMES.forEachIndexed { index, packageName ->
                db.insert(PositionedAppDaoItem.TB_NAME, SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put("id", index.toLong())
                        put("list_id", RESERVED_LIST_ID_FAV)
                        put("package_name", packageName)
                        put("position", index)
                        put("lock", false)
                    })
            }
            PrefMgr.get().putLastAppVersion(context.getCurrentAppVersion())
        }
    })
    .addMigrations(object : Migration(PositionedListDb.VERSION - 1, PositionedListDb.VERSION) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE ${PositionedAppDaoItem.TB_NAME} ADD COLUMN lock INTEGER NOT NULL DEFAULT 0"
            )
        }
    })
    .build()
```

**설계 포인트**

- 마이그레이션 버전을 `1, 2` 하드코딩이 아닌 **`VERSION - 1 → VERSION`** 으로 선언해
  DB 버전 상수 한 곳만 올리면 되도록 했다.
- 테이블명을 `TB_NAME` 상수로 두고 **쿼리 문자열 전부를 문자열 템플릿으로 구성** →
  테이블명 오타로 인한 런타임 크래시를 원천 차단.
  ```kotlin
  @Query("SELECT * FROM ${PositionedAppDaoItem.TB_NAME}")
  abstract fun getAll(): List<PositionedAppDaoItem>
  ```
- **예약 ID 체계**(`RESERVED_LIST_ID_FAV = 0L`, `RESERVED_LIST_ID_APPS = -1L`, `LOCKED_APPS_LIST_ID = -2L`)로
  "시스템이 소유한 리스트"(0 이하)와 "사용자가 만든 리스트"(양수, `getNextId()`로 채번)를 부호로 분리 —
  별도 플래그 컬럼 없이 소유권을 표현.

원자성이 필요한 조작은 `@Transaction`으로 명시한다.

```kotlin
@Transaction
open fun lockApp(app: PositionedAppDaoItem) {
    delete(app.packageName)   // 기존 위치에서 제거
    insert(app)               // 잠금 리스트로 이동
}
```

그리고 **auto-generate PK를 삽입 직후 도메인 객체에 되돌려주는** 패턴:

```kotlin
fun insertApp(listId: Long, apps: List<InstalledAppInfo>): ArrayList<PositionedAppDaoItem> {
    val daoList = arrayListOf<PositionedAppDaoItem>().apply {
        apps.forEachIndexed { i, it ->
            add(PositionedAppDaoItem(id = i.toLong(), listId = listId,
                    packageName = it.packageName, position = i)
                .apply { installedAppInfo = it })
        }
    }
    // insert()가 반환한 rowId를 각 아이템에 반영해 메모리/DB 상태를 일치시킴
    return arrayListOf<PositionedAppDaoItem>().apply {
        insert(daoList).forEachIndexed { index, key -> add(daoList[index].copy(key)) }
    }
}
```

> Room의 `@Insert`가 반환하는 `List<Long>`(rowId)를 버리지 않고 즉시 반영해,
> **재조회(SELECT) 없이** 메모리 캐시와 DB의 PK를 동기화한다.

---

## 4. Mutex 공유를 통한 다중 매니저 간 동시성 제어

`main/positioned/PositionedAppMgr.kt` / `main/recent/RecentMgr.kt`

```kotlin
// PositionedAppMgr
private val ioScope = CoroutineScope(Dispatchers.IO)
private val mutex = Mutex()

// recent 와 positionedAppMgr 과 동기화를 해주기 위해 mutex 공유
// 같은 mutex 를 사용하면 한 번에 하나의 coroutine 만 실행되기 때문에 동기화 처리
private val recentMgr = RecentMgr(mutex)

private fun lockedLaunch(content: suspend CoroutineScope.() -> Unit) {
    ioScope.launch { mutex.withLock { content() } }
}
```

```kotlin
// RecentMgr — 동일한 Mutex 인스턴스를 주입받는다
class RecentMgr(private val mutex: Mutex) {
    private val recentScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }

    private fun lockedLaunch(content: suspend CoroutineScope.() -> Unit) {
        recentScope.launch { mutex.withLock { content() } }
    }

    fun reqRefreshRowItems() = lockedLaunch {
        recentRowItems.clear()
        buildRowItems().also { rowItems ->
            recentRowItems.addAll(rowItems)
            withContext(Dispatchers.Main) {          // 콜백은 항상 Main으로 복귀
                listeners.forEach { l -> l.onRefreshed(rowItems) }
            }
        }
    }
}
```

**설계 포인트**

- 두 매니저가 **같은 홈 화면 상태(Row 리스트)를 서로 다른 트리거로 갱신**한다.
  (앱 설치/삭제 → `PositionedAppMgr`, ContentProvider 변경 → `RecentMgr`)
  각자 별도의 Lock을 쓰면 두 갱신이 뒤섞여 Row 순서가 깨진다.
- 해결책: **Mutex 인스턴스 자체를 생성자로 주입**해 두 클래스가 하나의 임계 구역을 공유.
  `synchronized` 블록과 달리 코루틴을 **블로킹하지 않고 suspend**시키므로 IO 스레드가 낭비되지 않는다.
- `lockedLaunch { }`라는 **단일 진입점**을 만들어, "DB에 쓰는 모든 코드는 반드시 이 함수를 통과한다"는
  규칙을 코드 구조로 강제했다. `PositionedAppMgr` 내 ~20곳의 DB write가 전부 이 형태다.
- 리스너 통지는 예외 없이 `withContext(Dispatchers.Main)` — **스레드 계약이 매니저 안에서 완결**되어
  호출부(Fragment)는 스레드를 신경 쓸 필요가 없다.

리스너 컬렉션은 `CopyOnWriteArrayList`로 두어 순회 중 등록/해제 시
`ConcurrentModificationException`을 방지한다.

```kotlin
private val listeners = CopyOnWriteArrayList<OnRecentListener>()

fun registerOnRecentListener(listener: OnRecentListener) {
    if (!listeners.contains(listener)) listeners += listener   // 중복 등록 방어
}
fun unregisterOnRecentListener(listener: OnRecentListener) { listeners -= listener }
```

---

## 5. position 정합성 자가 복구 로직

`main/positioned/PositionedAppMgr.kt`

```kotlin
private fun resetPositionIfInvalidLocked(
    appsGridDBItems: ArrayList<PositionedAppDaoItem>,
    rows: ArrayList<PositionedListDaoItem>
) {
    // 1) 전체 앱 그리드
    resetPositionIfInvalidLocked(appsGridDBItems.map { it.position }, appsGridDBItems, appDao)

    // 2) 각 즐겨찾기 Row의 내부 앱들
    rows.forEach { row ->
        if (row is PositionedFavRowItem) {
            resetPositionIfInvalidLocked(row.items.map { it.position }, row.items, appDao)
        }
    }

    // 3) Row 자체의 순서
    resetPositionIfInvalidLocked(rows.map { it.position }, rows, listDao)
}

private fun <T : PositionedItem> resetPositionIfInvalidLocked(
    positionedList: List<Int>,
    list: ArrayList<T>,
    dao: BaseDao<T>                      // ← 1번의 제네릭 DAO가 여기서 값을 한다
) {
    // db 내 position 이 중복되어 있으면 memory index 로 전부 update 처리
    if (positionedList.lastIndex != positionedList.distinct().lastIndex) {
        dao.update(list.onEachIndexed { index, t -> t.position = index })
    }
}
```

**설계 포인트**

- 셋톱박스는 **전원이 그냥 뽑힌다.** 정렬 도중 프로세스가 죽으면 `position` 컬럼에 중복이 남고,
  다음 부팅 때 아이템 순서가 뒤엉키거나 아이템이 사라진 것처럼 보인다.
- 이를 예외 처리로 대응하는 대신 **부팅 시 무결성 검사 + 자가 복구**로 설계했다.
  중복 판정은 `distinct()` 후 크기 비교 한 줄 — 정상 케이스에서는 리스트 스캔 1회로 끝나
  **부팅 경로에 부담을 주지 않는다.**
- 세 종류의 리스트(앱 그리드 / Row 내부 앱 / Row 자체)를 **제네릭 함수 하나**로 처리.
  `<T : PositionedItem>` 상한 덕분에 `t.position = index`가 타입 안전하고,
  `BaseDao<T>` 파라미터 덕분에 대상 테이블이 달라도 같은 코드가 돈다.

---

## 6. 드래그 앤 드롭 재정렬 — O(변경분)만 DB에 반영

`main/positioned/PositionedAppMgr.kt`

```kotlin
fun moveApp(listId: Long, fromPosition: Int, toPosition: Int) {
    val apps: ArrayList<PositionedAppDaoItem>
    val changeList = arrayListOf<PositionedAppDaoItem>()

    if (listId == RESERVED_LIST_ID_APPS) {
        // 전체 앱 그리드: "끼워넣기" 시맨틱 → 사이에 낀 아이템들이 한 칸씩 밀린다
        apps = appGridItems
        changeList.addAll(
            if (fromPosition > toPosition) apps.increasePosition(toPosition, fromPosition)
            else                           apps.reducePosition(fromPosition + 1, toPosition + 1)
        )
    } else {
        // 즐겨찾기 Row: "맞교환" 시맨틱 → 두 아이템만 바뀐다
        apps = homeRowItems.getFavRow(listId).items
        changeList.add(apps[toPosition].also { it.position = fromPosition })
    }

    changeList.add(apps[fromPosition].apply { position = toPosition })
    apps.add(toPosition, apps.removeAt(fromPosition))   // 메모리 반영

    lockedLaunch { appDao.update(changeList) }          // 변경분만 일괄 UPDATE
}

private fun <T : PositionedItem> List<T>.increasePosition(from: Int = 0, to: Int = size): List<T> =
    subList(from, to).onEach { it.position += 1 }

private fun <T : PositionedItem> List<T>.reducePosition(from: Int = 0, to: Int = size): List<T> =
    subList(from, to).onEach { it.position -= 1 }
```

**설계 포인트**

- 리스트 성격에 따라 **재정렬 시맨틱이 다르다**(끼워넣기 vs 맞교환)는 도메인 규칙을
  한 함수 안에 명시적으로 분기해 담았다.
- 순진한 구현은 "리스트 전체를 재인덱싱 후 전부 UPDATE"지만,
  여기서는 `subList(from, to)`로 **실제로 영향받는 구간만** 잘라내
  `changeList`에 모아 **단 한 번의 배치 UPDATE**로 커밋한다.
  100개 앱 중 2칸 이동 시 DB write는 3행. 저사양 셋톱박스에서 체감 차이가 크다.
- `subList()`는 뷰(view)를 반환하므로 `onEach`로 준 변경이 **원본 리스트에 그대로 반영**된다 —
  복사 없이 메모리 상태와 DB 반영 대상을 동시에 확보.
- 확장 함수(`increasePosition` / `reducePosition`)로 빼서 **의도가 이름으로 드러나게** 했다.
- UI 즉시 갱신(메모리) → DB 반영(비동기)의 **낙관적 업데이트** 순서로,
  RCU 입력에 대한 반응성을 확보.

ID/position 채번도 확장 함수로 통일:

```kotlin
@JvmName("getNextIdListItem")
private fun List<PositionedListDaoItem>.getNextId(): Long {
    var ret = -1L; forEach { if (it.id >= ret) ret = it.id }; return ret + 1
}

fun List<PositionedItem>.getNextPosition(): Int {
    var ret = -1; forEach { if (it.position >= ret) ret = it.position }; return ret + 1
}
```

> `@JvmName`으로 JVM 시그니처 충돌(제네릭 소거)을 우회하면서
> 호출부에서는 `list.getNextId()`라는 동일한 표현을 유지.

---

## 7. RecyclerView Adapter 계층 — Template Method 패턴

`main/grid/GridAdapter.kt` → `GridAdapterContent.kt` → `home/GridAdapterContentHome.kt` → 구체 어댑터

```
        RecyclerView.Adapter
                 │
            GridAdapter                   ← 아이템/리스너/클릭 디바운스 등 공통 골격
                 │
         GridAdapterContent               ← 포커스 처리 레이어(TV 필수)
            ┌────┴──────────────────┐
   GridAdapterContentApps       GridAdapterContentHome        ← DiffUtil 레이어
   GridAdapterLockApps            ┌─────────┼──────────┐
                           ...HomeFav  ...HomeRecent  ...HomeCustom
```

### 7-1. 최상위 골격 — `GridAdapter`

```kotlin
abstract class GridAdapter : RecyclerView.Adapter<GridAdapter.ViewHolder>() {

    open val items = ArrayList<Any>()

    abstract fun getItemView(context: Context): View                    // 서브클래스가 뷰만 결정
    open fun onCreateItemView(parent: ViewGroup, viewType: Int): View = getItemView(parent.context)

    private var job: Job? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(onCreateItemView(parent, viewType)).also { holder ->
            holder.itemView.apply {
                setOnClickListener {
                    // RCU 연타로 인한 중복 실행 방지 — 마지막 입력만 100ms 뒤 반영
                    job?.cancel()
                    job = CoroutineScope(Dispatchers.Main).launch {
                        delay(100)
                        onClickListeners?.onItemClick(holder)
                    }
                }
                setOnLongClickListener { onClickListeners?.onItemLongClick(holder) ?: false }
                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) onTouchListeners?.onTouch(holder, v)
                    true
                }
            }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.item = getItem(position).also { (holder.itemView as ItemView).onItemSet(it) }
    }

    fun swap(fromPosition: Int, toPosition: Int) {
        items.add(toPosition, items.removeAt(fromPosition))
        notifyItemMoved(fromPosition, toPosition)
    }
}
```

```kotlin
/** 커스텀 뷰가 지켜야 할 유일한 계약 */
interface ItemView {
    fun onItemSet(item: Any)
}
```

**설계 포인트**

- `onBindViewHolder`에서 **findViewById도, 타입 캐스팅 분기도 하지 않는다.**
  바인딩 책임을 `ItemView.onItemSet(item)`으로 뷰 자신에게 넘겼다.
  → 어댑터는 아이템 개수만 알면 되고, **뷰를 새로 추가할 때 어댑터를 수정할 필요가 없다.**
- **클릭 디바운스**: TV 리모컨은 마우스와 달리 연타·중복 입력이 흔하다.
  `Job`을 취소/재발행하는 방식으로 마지막 입력만 살려, 다이얼로그 이중 실행 버그를 구조적으로 차단.
- `getItemView(context)`만 추상으로 남기고 나머지 전 과정을 상위에서 확정 —
  구체 어댑터가 **3줄**로 끝난다.

  ```kotlin
  class GridAdapterContentApps : GridAdapterContent() {
      override fun getItemView(context: Context): View = GridItemViewAppLarge(context)
  }
  ```

### 7-2. 포커스 레이어 — `GridAdapterContent`

```kotlin
abstract class GridAdapterContent : GridAdapter() {
    override fun onCreateItemView(parent: ViewGroup, viewType: Int): View =
        getItemView(parent.context).also { view ->
            view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                (v as GridItemView).updateFocusedView(hasFocus)
            }
        }
}
```

```kotlin
abstract class GridItemView(context: Context, ...) : FrameLayout(...), ItemView {
    companion object {
        const val ITEM_DEFAULT_SCALE = 1F
        const val ITEM_EXPAND_SCALE  = 1.14F
    }

    open fun updateFocusedView(focus: Boolean) {
        startAnimation(ScaleAnimation(
            if (focus) ITEM_DEFAULT_SCALE else ITEM_EXPAND_SCALE,
            if (focus) ITEM_EXPAND_SCALE  else ITEM_DEFAULT_SCALE,
            /* y축 동일 */ ...,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 100; fillAfter = true })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateDim(!(parent as ViewGroup).hasFocus())   // 재활용된 뷰의 dim 상태 복원
    }

    open fun updateDim(show: Boolean) {}
}
```

**설계 포인트**

- **터치 UI가 아닌 D-pad UI**에서 포커스 스케일/딤 처리는 전 아이템의 공통 요구사항이다.
  이걸 뷰마다 중복 구현하는 대신 계층 한 단계(`GridAdapterContent` + `GridItemView`)로 끌어올렸다.
- `onAttachedToWindow`에서 dim을 재계산하는 부분이 실전 디테일이다.
  RecyclerView가 뷰를 재활용하면 **이전 아이템의 dim 상태가 그대로 남는** 버그가 나는데,
  attach 시점에 부모 포커스 기준으로 다시 계산해 원천 차단했다.

### 7-3. Diff 레이어 — `GridAdapterContentHome`

```kotlin
abstract class GridAdapterContentHome(items: ArrayList<*>? = null) : GridAdapterContent() {
    init { items?.also { this.items.addAll(it) } }

    fun notifyDifferentItems(
        oldList: ArrayList<PositionedAppDaoItem>,
        newList: ArrayList<PositionedAppDaoItem>
    ) {
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = oldList[o].id == newList[n].id
            override fun areContentsTheSame(o: Int, n: Int) = oldList[o] == newList[n]
        }).dispatchUpdatesTo(this)
    }
}
```

`GridAdapterContentHomeRecent`는 아이템 타입에 따라 **뷰 클래스 자체를 갈아끼운다**:

```kotlin
class GridAdapterContentHomeRecent(
    private val recentType: RecentType,
    items: ArrayList<*>
) : GridAdapterContentHome(items) {

    // Live 채널이면 채널 카드, VOD면 포스터 카드 — viewType 분기 없이 생성자 주입으로 해결
    override fun getItemView(context: Context): View =
        if (recentType.isLive()) ChannelCardView(context) else VodCardView(context)

    interface RecentRowListener : GridAdapterHomeRoot.EditableRowListener {
        fun onItemClick(item: RecentCardItem)
    }
}
```

**설계 포인트**

- `getItemViewType()` 오버라이드 + `when` 분기 대신, **Row 단위로 어댑터 인스턴스가 분리**되어 있음을 활용해
  생성자 파라미터로 뷰 타입을 확정. 한 Row 안에서는 아이템 타입이 동일하다는 도메인 사실을 그대로 코드에 반영.
- **리스너 인터페이스를 어댑터 내부에 중첩 선언**하고 상속으로 확장(`EditableRowListener`)해,
  "이 어댑터를 쓰려면 무엇을 구현해야 하는가"가 한 파일에서 드러난다.

---

## 8. HTTP 계층 — 구현체 교체 가능한 전략 구조

`utils/http/BaseConnection.kt` / `OkHttpClientImpl.kt` / `HttpConnector.kt`

```kotlin
internal abstract class BaseConnection(val stringUrl: String, val allowUnverified: Boolean) {
    companion object { const val TIMEOUT_HTTPS = 1000 * 5 }

    abstract fun getInputStream(): InputStream?
    abstract fun requestHeader(key: String, value: String): Int
    abstract fun release()
}
```

```kotlin
/** release 호출 필수 */
class HttpConnector(stringUrl: String, allowUnverifiedConnection: Boolean) {
    companion object {
        private const val CONNECTION_TYPE_OK_HTTP_CLIENT = 3
        private const val CONNECTION_TYPE = CONNECTION_TYPE_OK_HTTP_CLIENT
        fun connectionTypeToString(type: Int) = when (type) {
            CONNECTION_TYPE_OK_HTTP_CLIENT -> "OkHttpClient"
            else -> "unknown"
        }
    }

    private val connector: BaseConnection = OkHttpClientImpl(stringUrl, allowUnverifiedConnection)

    fun requestHeader(key: String, value: String): Int {
        if (BuildConfig.DEBUG) {
            return connector.requestHeader(key, value)     // 디버그: 그대로 터뜨려 원인 파악
        } else {
            try {
                return connector.requestHeader(key, value)
            } catch (e: Exception) {                       // 릴리즈: 크래시 대신 원격 리포트
                FirebaseCrashlytics.getInstance().recordException(
                    Throwable("requestHeader - key: $key, value: ${value.masked()}//${e.message}", e))
            }
            return -1
        }
    }
}
```

```kotlin
internal class OkHttpClientImpl(stringUrl: String, allowUnverified: Boolean)
    : BaseConnection(stringUrl, allowUnverified) {

    override fun requestHeader(key: String, value: String): Int {
        try {
            okHttpClient = getCustomOkHttpClient()
            okHttpClientResponse = okHttpClient?.newCall(
                Request.Builder().url(stringUrl).addHeader(key, value).head().build()
            )?.execute()
            return okHttpClientResponse?.networkResponse?.code ?: -1
        } catch (e: Exception) {
            throw e
        } finally {
            release()                                     // 성공/실패 무관하게 자원 회수
        }
    }

    override fun release() {
        okHttpClientResponse?.body?.close()
        okHttpClientResponse?.close()
        okHttpClient?.connectionPool?.evictAll()           // 커넥션 풀까지 정리
    }
}
```

**설계 포인트**

- `HttpConnector`가 **Facade**, `BaseConnection`이 **Strategy 인터페이스**, `OkHttpClientImpl`이 구현체.
  호출부는 OkHttp를 전혀 모르며, 실제로 이 프로젝트는
  `HttpUrlConnection → HttpClient → OkHttp`로 두 번 구현체를 갈아탔지만
  **호출부 코드는 한 줄도 바뀌지 않았다.** (교체 이력이 상수로 남아 있다)
- 핵심은 **디버그/릴리즈 예외 정책의 분리**다:
  - 디버그 빌드 — 예외를 그대로 전파해 개발 중 즉시 발견
  - 릴리즈 빌드 — 크래시 대신 Crashlytics에 **요청 컨텍스트를 포함해** 기록하고 `-1` 반환

    셋톱박스는 사용자가 로그를 보내줄 수 없는 환경이라, 원격 리포트에 컨텍스트를 실어 보내는 게 유일한 단서다.
    다만 헤더 값에는 인증 토큰이 실릴 수 있으므로, **키는 그대로 남기고 값은 마스킹**해서
    디버깅 단서와 자격증명 보호를 동시에 만족시킨다.
- `release()`가 body/response/connectionPool **3단계를 모두** 정리한다.
  OkHttp에서 body만 닫고 커넥션 풀을 방치해 소켓이 누수되는 건 흔한 실수인데, 여기서는 명시적으로 처리.
- 클래스 주석 `/** release 호출 필수 */` — 소유권 계약을 코드 옆에 남김.

`network/BaseDownloader.kt`도 동일한 골격이다. 다운로드 **알고리즘(버퍼 루프, 취소 체크, 진행률 통지,
finally 정리)은 추상 클래스가 확정**하고, 연결 방식만 서브클래스에 위임한다.

```kotlin
abstract class BaseDownloader(val urlPath: String) {

    fun download(saveFilePath: String, l: OkDownloadListener) {
        var fos: FileOutputStream? = null
        try {
            state = STATE_DOWNLOADING
            listener?.onDownloadStart(urlPath, totalSize)
            ...
            while (true) {
                len = it.read(buffer)
                if (len <= 0) break
                fos!!.write(buffer, 0, len)
                downloadSize += len

                if (isCancel) {                      // 루프 내 협조적 취소
                    fos!!.flush()
                    listener?.onDownloadCancel(urlPath)
                    return@download
                }
                listener?.onDownloadProgress(urlPath, totalSize, downloadSize)
            }
            ...
        } catch (e: IOException)  { downloadErrCode = RESULT_DN_ERR_IOEXCEPTION }
          catch (e: Exception)    { downloadErrCode = RESULT_DN_ERR_EXCEPTION }
        finally {
            fos?.close(); inputStream?.close(); inputStream = null
            init()                                   // 다음 다운로드를 위한 상태 초기화
            if (isCancel) state = STATE_DOWNLOAD_CANCELED
        }
        state = STATE_DOWNLOAD_FAIL
        listener?.onDownloadFail(urlPath, saveFilePath, downloadErrCode)
    }

    fun stopDownload() { if (state == STATE_DOWNLOADING) isCancel = true }

    protected abstract fun connect()
    abstract fun getHeader(key: String): String?
    abstract fun disconnect()
}
```

> 스레드 강제 종료(`Thread.stop()`) 대신 **플래그 기반 협조적 취소**를 쓰고,
> 취소 시에도 `flush()`로 버퍼를 비운 뒤 콜백을 준다 — 부분 다운로드 파일이 깨지지 않는다.

---

## 9. 도메인 enum + 컴파일타임 타입 안전성

`main/recent/RecentType.kt`

```kotlin
enum class RecentType(
    @RecentID val id: Int,
    @RecentUID val uid: String,
    @StringRes val headerResId: Int,
) {
    TYPE_LIVE_HISTORY  (0, "live.history",   R.string.recent_live_tv_history),
    TYPE_LIVE_FAVORITE (5, "live.favorite",  R.string.recent_live_tv_favorite),
    TYPE_VOD_HISTORY   (1, "vod.history",    R.string.recent_vod_history),
    ...;

    fun isLive()        = this == TYPE_LIVE_HISTORY || this == TYPE_LIVE_FAVORITE
    fun isLiveFavorite()= this == TYPE_LIVE_FAVORITE
    fun isHistory()     = this == TYPE_LIVE_HISTORY || this == TYPE_VOD_HISTORY || this == TYPE_TV_SERIES_HISTORY

    companion object {
        fun from(@RecentID id: Int): RecentType = values().find { it.id == id }
            ?: throw IllegalArgumentException("RecentType - get - invalid id: $id")

        fun from(@RecentUID uid: String): RecentType = values().find { it.uid == uid }
            ?: throw IllegalArgumentException("RecentType - get - invalid uid: $uid")

        /*
         * 연동 대상 미디어 앱에서 Channel 구분을 위해 사용하는 provider UID.
         * 반드시 해당 앱의 정의와 일치해야 함 (TV Provider 의 채널 레코드를 통해 전달됨)
         * 형식) <연동앱 패키지>.<db id>.<recent uid>.<채널 식별자>
         */
        @Retention(AnnotationRetention.SOURCE)
        @StringDef(UID_LIVE_HISTORY, UID_LIVE_FAVORITE, ...)
        annotation class RecentUID

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(flag = true, value = [ID_LIVE_HISTORY, ID_LIVE_FAVORITE, ...])
        annotation class RecentID
    }
}
```

**설계 포인트**

- **외부 앱과의 프로토콜 상수를 단일 소스로 봉인.**
  이 id/uid는 연동 대상 미디어 앱의 ContentProvider와 반드시 일치해야 하는 값인데,
  관리하지 않으면 프로젝트 곳곳에 매직 넘버로 흩어지기 쉬운 값이다.
  enum 하나에 모으고 **어느 파일의 어느 상수와 짝인지 주석으로 명시**해 유지보수 지점을 고정했다.
- `@IntDef` / `@StringDef` 커스텀 어노테이션으로 **Lint 레벨 타입 검사**를 추가.
  `RecentType.from(3)` 같은 임의 정수 전달을 IDE가 경고한다.
  `@Retention(SOURCE)`이므로 런타임 오버헤드는 0.
- `from()`은 매칭 실패 시 **null이 아니라 예외**를 던진다.
  외부 앱이 모르는 타입을 보냈다면 그건 조용히 넘길 상황이 아니라 프로토콜 위반이므로,
  실패를 지연시키지 않고 즉시 드러내는 fail-fast 선택.
- `isLive()`, `isHistory()` 같은 **술어 메서드**를 enum에 붙여, 호출부에서
  `type == A || type == B` 형태의 조건식이 반복 확산되는 것을 막았다.
  → 7-3의 `if (recentType.isLive()) ChannelCardView else VodCardView`처럼 읽힌다.

`RecentMgr`에서는 확장 함수로 파싱 규칙까지 도메인 근처에 묶어둔다.

```kotlin
private fun Channels.getRecentType(): RecentType =
    RecentType.from(app_link_intent_uri?.last()?.toString()?.toInt() ?: -1)
```

---

## 10. UI 프레임워크화 — 추상 Fragment + Fluent Builder

`common/dialog/FullScreenDialogFragment.kt`

```kotlin
abstract class FullScreenDialogFragment : DialogFragment() {

    private val dimColor by lazy { resources.getColor(R.color.dim, null) }

    abstract fun getViewToDim(): View          // 어디를 어둡게 할지만 서브클래스가 결정

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        object : Dialog(requireActivity(), theme) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                // 최상단 자식 Fragment에 먼저 키 처리 기회를 준다 (RCU BACK 중복 처리 방지)
                val fragment = parentFragmentManager.fragments.last()
                if (fragment is IDispatchKeyEvent && fragment.customDispatchKeyEvent(event)) return true
                return super.dispatchKeyEvent(event)
            }
            override fun onBackPressed() {
                startExitAnim { super@FullScreenDialogFragment.dismissAllowingStateLoss() }
            }
        }

    // dismiss 경로를 전부 가로채 "애니메이션 완료 후 실제 dismiss"로 일원화
    override fun dismiss()                  { startExitAnim { super.dismiss() } }
    override fun dismissAllowingStateLoss() { startExitAnim { super.dismissAllowingStateLoss() } }

    protected open fun startEnterAnim() { startDimAnim(true) }
    protected open fun startExitAnim(onHideAnimEnded: () -> Unit) { startDimAnim(false, onHideAnimEnded) }
}
```

**설계 포인트**

- **dismiss 경로의 일원화**가 핵심이다. `dismiss()`와 `dismissAllowingStateLoss()`를 둘 다 오버라이드해
  어느 경로로 닫히든 **반드시 퇴장 애니메이션을 거치도록** 강제했다.
  호출부는 평범하게 `dismiss()`만 부르면 되고, 애니메이션을 잊어버릴 수가 없다.
- 키 이벤트를 **자식 Fragment에 먼저 위임**하는 체인 구조.
  TV 런처에서 "BACK 한 번에 패널이 두 번 닫히는" 류의 버그를 구조적으로 해결한 부분이다.
  (실제로 이 프로젝트의 커밋 이력에 남아 있는 이슈다)

`customguidedstep/CustomGuidedStepListFragment.kt` — 그 위에 얹은 **재사용 가능한 리스트 다이얼로그 프레임워크**

```kotlin
abstract class CustomGuidedStepListFragment : FullScreenDialogFragment() {

    // 서브클래스가 채워야 할 슬롯은 딱 둘
    abstract fun getTitle(): String
    abstract fun getGridAdapter(): GridAdapter

    open fun getEmptyData(): EmptyData = EmptyData()      // 빈 상태는 선택적 커스터마이즈

    private val content: CustomGuidedStepContent by lazy {
        CustomGuidedStepContent(getEmptyData())
            .setTitle(getTitle())
            .setGridAdapter(getGridAdapter())             // ← Fluent Builder
            .apply { focusSearchListener?.let { setListener(it) } }
    }

    override fun onCreateView(...): View =
        inflater.inflate(R.layout.layout_custom_guided_setp_list, container, false).apply {
            childFragmentManager.beginTransaction()
                .replace(R.id.custom_guided_step_list_content, content)
                .commit()
        }

    override fun startEnterAnim() { super.startEnterAnim(); startSlideAnim(true) }
    override fun startExitAnim(onHideAnimEnded: () -> Unit) {
        super.startExitAnim(onHideAnimEnded); startSlideAnim(false)
    }
}

data class EmptyData(@StringRes val titleRes: Int? = null, @DrawableRes val iconRes: Int? = null)
```

```kotlin
class CustomGuidedStepContent(private val emptyData: EmptyData) : CustomGuidedStepContentView() {
    fun setTitle(title: String): CustomGuidedStepContent { this.title = title; return this }
    fun setGridAdapter(adapter: GridAdapter): CustomGuidedStepContent { this.adapter = adapter; return this }

    fun updateEmptyView(show: Boolean) {
        gridView.isVisible = !show
        emptyView.isVisible = show
        emptyTitleView.text = emptyData.titleRes?.let { getString(it) }
        emptyData.iconRes?.let { emptyIconView.setImageResource(it) }
    }
}
```

**설계 포인트**

- 안드로이드 Leanback의 `GuidedStepFragment`가 디자인 요구사항에 맞지 않아 **직접 대체 구현**한 것.
  단순히 화면 하나를 만든 게 아니라, **다른 화면들이 재사용할 프레임워크로** 만들었다.
- 서브클래스는 `getTitle()`, `getGridAdapter()` **두 개만 구현**하면
  딤 처리 · 슬라이드 인/아웃 애니메이션 · 빈 상태 화면 · 포커스 탐색 · BACK 처리가 전부 따라온다.
- 애니메이션은 **super 호출 + 자기 몫 추가**로 조립된다.
  `FullScreenDialogFragment`가 딤을, `CustomGuidedStepListFragment`가 슬라이드를 담당해
  **각 계층이 자기가 아는 만큼만 애니메이션한다.**
- 빈 상태를 `EmptyData(titleRes, iconRes)` data class로 캡슐화하고 기본값을 제공 —
  대부분의 화면은 신경 쓸 필요가 없고, 필요한 화면만 `getEmptyData()`를 오버라이드.
- 콘텐츠 영역을 **child Fragment로 분리**해, 리스트 UI 자체를 다른 컨테이너에서도 재사용 가능하게 열어둠.

---

## 11. 하드웨어 의존성 인터페이스 분리

`setupwizard/IEthernetManager.kt`

```kotlin
interface IEthernetManager {
    fun init(context: Context)
    fun release(context: Context)

    fun getEthernetType(): EthernetType
    fun getInterfaceName(): String
    fun getDeviceNameList(): List<String>

    fun enableEthernet(enable: Boolean)
    fun setInterfaceName(ethDevice: String)

    fun getIpInfo(type: EthernetType?): IpInfo
    fun setEthernetListener(listener: OnEthernetListener)

    fun startDhcp()
    fun startManual(ipInfo: IpInfo)
}

interface OnEthernetListener {
    fun onConnected(type: EthernetType)
    fun onPhyLinkDown()          // 물리 케이블 분리 — TV 셋톱박스 특유의 상태
    fun onDisconnected()
}
```

**설계 포인트**

- 이더넷 제어는 **AOSP 내부 API / 벤더 SDK(리플렉션 포함)** 에 의존한다.
  이걸 그대로 Activity에서 부르면 기기·펌웨어가 바뀔 때마다 UI 코드가 함께 무너진다.
- 인터페이스로 경계를 긋고 `EthernetManagerImpl`(281줄)에 벤더 의존 코드를 격리 —
  **셋업 위저드 UI는 안정적인 인터페이스만 바라본다.**
- `onConnected` / `onDisconnected`와 별개로 `onPhyLinkDown()`을 둔 것이 도메인 이해를 보여준다.
  "설정은 살아있지만 케이블이 빠진" 상태는 논리적 연결 해제와 사용자 안내 문구가 달라야 한다.
- `IpInfo`는 전 필드가 `"0.0.0.0"` 기본값 —
  DHCP 모드에서 부분적으로만 채워지는 상황에 대해 null 체크 대신 **안전한 기본값** 전략.

---

## 12. Hilt DI + ViewModel 생명주기 관리

`main/MainViewModel.kt`

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {

    val positionedMgr = PositionedAppMgr(context)

    private var moveModeData: MoveModeData? = null

    override fun onCleared() {
        super.onCleared()
        positionedMgr.release()          // DB/코루틴 스코프 정리를 ViewModel 생명주기에 위임
    }

    fun startMoveMode(gridId: Long, type: MoveModeType) { moveModeData = MoveModeData(gridId, type) }
    fun stopMoveMode()  { moveModeData = null }

    fun isMoveMode()      = moveModeData != null
    fun isAppMoveMode()   = moveModeData?.type == MoveModeType.APP
    fun isRowMoveMode()   = moveModeData?.type == MoveModeType.ROW

    fun moveAppPosition(fromPosition: Int, toPosition: Int) {
        positionedMgr.moveApp(requireNotNull(moveModeData).gridId, fromPosition, toPosition)
    }

    private data class MoveModeData(val gridId: Long, val type: MoveModeType)
    enum class MoveModeType { ROW, APP }
}
```

**설계 포인트**

- `@ApplicationContext`를 명시적으로 주입 —
  `PositionedAppMgr`가 Room DB와 코루틴 스코프를 오래 들고 있으므로,
  **Activity Context가 새어 들어갈 여지를 DI 단계에서 차단**했다.
- **"모드"를 boolean 플래그 여러 개가 아닌 nullable data class로** 표현했다.
  `isMoveMode`, `isRowMode`, `isAppMode`를 각각 boolean으로 두면
  `isMoveMode=false && isRowMode=true` 같은 **불가능한 조합**이 표현 가능해진다.
  여기서는 `moveModeData` 하나가 null이면 비활성, 아니면 `type`이 곧 모드 —
  **잘못된 상태를 표현할 수 없게** 만들었다.
- `moveAppPosition`의 `requireNotNull(moveModeData)` — 이동 모드가 아닌데 호출됐다면
  그건 UI 흐름 자체의 버그이므로 조용히 무시하지 않고 즉시 드러낸다.
- `onCleared()`에서 자원 회수 — 화면 회전/재생성 시 DB 커넥션과 코루틴 스코프가 누수되지 않는다.

---

## 13. 기능 단위 수직 분할 — Hotkey 화면의 MVVM 구성

`main/quicksettings/hotkey/`

1~12번이 **레이어별 수평 구조**(데이터 계층 / 어댑터 계층 / 네트워크 계층)라면,
RCU 컬러키 설정 화면은 **기능 단위로 수직 분할**한 모듈이다.

```
hotkey/
├── HotkeyActivity.kt              ← NavHost 컨테이너
├── data/
│   ├── HotkeyItem.kt              ← Parcelable 도메인 모델 (화면 간 전달용)
│   ├── AppItem.kt
│   └── DataExtension.kt           ← @IntDef / @StringDef + 키 상수
├── viewmodel/
│   ├── SelectHotkeyViewModel.kt   ← LiveData 로 목록 노출
│   └── LinkHotkeyViewModel.kt     ← SavedStateHandle 로 화면 인자 수신
├── fragment/                      ← 관찰 + 어댑터 연결만
└── adapter/
    ├── BaseAdapter.kt             ← ListAdapter + DiffUtil 골격
    ├── SelectHotkeyAdapter.kt
    └── LinkHotkeyAdapter.kt
```

**설계 포인트**

- 화면 전환은 Navigation Component가 맡고, 인자는 `Parcelable`로 실어 보낸다.
  받는 쪽은 Fragment가 아니라 **ViewModel이 `SavedStateHandle`에서 직접 꺼낸다.**

  ```kotlin
  class LinkHotkeyViewModel(
      application: Application,
      savedStateHandle: SavedStateHandle,
  ) : AndroidViewModel(application) {

      val hotkeyItem: HotkeyItem = savedStateHandle.get<HotkeyItem>(EXTRA_HOTKEY) as HotkeyItem
      val currentLinkedPackageName: String = PrefMgr.get().getString(hotkeyItem.hotkeyId, "")
  }
  ```

  → Fragment에 `arguments` 파싱 코드가 존재하지 않고, 프로세스가 재생성돼도 인자가 살아남는다.
- 그 결과 Fragment에는 **관찰과 연결만** 남는다.

  ```kotlin
  class SelectHotkeyFragment : Fragment() {
      private val viewModel by lazy { ViewModelProvider(this).get(SelectHotkeyViewModel::class.java) }

      override fun onCreateView(...): View =
          FragmentSelectHotkeyBinding.inflate(inflater, container, false).apply {
              val adapter = SelectHotkeyAdapter(requireContext().packageManager)
              rvSelectHotkey.adapter = adapter
              rvSelectHotkey.setHasFixedSize(true)
              viewModel.hotkeys.observe(viewLifecycleOwner) { adapter.submitList(it) }
          }.also { viewModel.updateHotkeyItems(requireContext()) }.root
  }
  ```

  `viewLifecycleOwner`로 관찰해 뷰가 파괴되면 구독이 함께 끊기고,
  `findViewById` 대신 ViewBinding을 써서 뷰 참조를 컴파일타임에 검증한다.
- 헤더/본문/푸터 구분도 매직 넘버가 아니라 `@IntDef` 상수 + 술어 메서드로 봉인 — 9번과 같은 전략이다.

  ```kotlin
  @Retention(AnnotationRetention.SOURCE)
  @IntDef(TYPE_OF_HEADER, TYPE_OF_FOOTER, TYPE_OF_CONTENTS)
  annotation class ItemType

  data class HotkeyItem(
      @ItemType val itemType: Int = TYPE_OF_CONTENTS,
      @HotkeyId val hotkeyId: String? = KEY_OF_HOTKEY_UNKNOWN,
      @DrawableRes val iconResId: Int = -1,
      @StringRes val nameResId: Int = -1,
      @StringRes val descriptionResId: Int = -1,
  ) : Parcelable {
      fun isHeader()   = itemType == TYPE_OF_HEADER
      fun isContents() = itemType == TYPE_OF_CONTENTS
  }
  ```

  아이콘·문자열을 `Drawable`/`String`이 아니라 **리소스 ID로 들고 다니는** 것도 의도적이다.
  Parcelable로 직렬화되는 모델에 무거운 객체가 실리지 않고, 언어 변경 시 해석 시점이 화면 쪽으로 미뤄진다.

### 13-1. `BaseAdapter<T>` — 어댑터를 "질문 목록"으로 뒤집기

```kotlin
abstract class BaseAdapter<T>(diffCallback: DiffUtil.ItemCallback<T>)
    : ListAdapter<T, BaseAdapter<T>.ViewHolder>(diffCallback) {

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSelectHotkeyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    final override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.binding(getItem(position))

    // 서브클래스는 "이 아이템은 어떤가?"에만 답한다
    abstract fun isFocusEnabled(item: T): Boolean
    abstract fun isDividerEnabled(item: T): Boolean
    @FloatRange(from = 0.0, to = 1.0)
    abstract fun getVerticalGuidelinePercent(item: T): Float
    abstract fun getHorizontalGuidelinePercent(item: T): Float
    abstract fun getIconDrawable(resources: Resources, item: T): Drawable?
    abstract fun getTitle(resources: Resources, item: T): String?
    abstract fun getDescription(resources: Resources, item: T): String?
    abstract fun onItemClicked(v: View, item: T)

    inner class ViewHolder(private val binding: ItemSelectHotkeyBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun binding(item: T) = binding.apply {
            containerItemSelectHotkey.apply {
                isFocusable = isFocusEnabled(item)
                setOnClickListener { onItemClicked(this, item) }
                setOnFocusChangeListener { _, hasFocus ->
                    tvSelectHotkeyDescription.isSelected = hasFocus   // 포커스 시에만 marquee
                }
            }
            divider.visibility = if (isDividerEnabled(item)) View.VISIBLE else View.GONE
            guidelineVertical.setGuidelinePercent(getVerticalGuidelinePercent(item))     // 좌우 정렬
            guidelineHorizontal.setGuidelinePercent(getHorizontalGuidelinePercent(item)) // 상하 정렬
            ivSelectHotkeyIcon.foreground = getIconDrawable(resources, item)
            tvSelectHotkeyTitle.text = getTitle(resources, item)
            tvSelectHotkeyDescription.text = getDescription(resources, item)
        }
    }
}
```

**설계 포인트**

- 7번의 `GridAdapter`가 "뷰를 무엇으로 만들지"를 서브클래스에 위임했다면,
  여기서는 한 걸음 더 나가 **"각 아이템의 값이 무엇인지"만 묻는 선언형 슬롯**으로 뒤집었다.
  서브클래스에는 `if/else`가 아니라 **답변 8개**만 남는다.
- `onCreateViewHolder` / `onBindViewHolder`에 `final`을 붙여 **템플릿 자체를 잠갔다.**
  서브클래스가 바인딩 순서를 바꾸거나 슬롯을 건너뛸 방법이 없다 — Template Method의 계약을
  주석이 아니라 **컴파일러가 강제**한다.
- `ListAdapter` 상속이라 갱신은 `submitList()` 한 번. DiffUtil 계산과 애니메이션은 라이브러리가 처리하고,
  각 어댑터는 `DiffUtil.ItemCallback`만 자기 파일 안에 중첩 선언한다.
- 포커스 가능 여부(`isFocusEnabled`)까지 슬롯으로 뺀 것이 D-pad UI 특유의 지점이다.
  헤더는 포커스를 받으면 안 되는데, 이걸 뷰 XML이 아니라 **아이템 데이터로 결정**해야
  같은 레이아웃을 헤더와 본문이 함께 쓸 수 있다.

### 13-2. 레이아웃 하나로 두 화면

두 화면(핫키 선택 / 앱 연결)과 헤더·본문 아이템이 **`item_select_hotkey.xml` 단 하나**를 공유한다.
차이는 ConstraintLayout **Guideline 퍼센트**로만 표현된다.

```kotlin
// SelectHotkeyAdapter — 아이템 성격에 따라 정렬 기준선을 옮긴다
override fun getVerticalGuidelinePercent(item: Pair<HotkeyItem, String>) = when {
    item.first.isHeader()        -> PERCENT_OF_GUIDELINE_HEADER_VERTICAL  // 0.05
    -1 != item.first.iconResId   -> PERCENT_OF_GUIDELINE_VERTICAL         // 0.2
    else                         -> 0F                                    // 아이콘 없음 → 좌측 정렬
}

override fun getHorizontalGuidelinePercent(item: Pair<HotkeyItem, String>) =
    if (-1 != item.first.descriptionResId) PERCENT_OF_GUIDELINE_HORIZONTAL else 1F  // 2줄 vs 1줄
```

**설계 포인트**

- `getItemViewType()` 분기도, 레이아웃 파일 추가도 없이 **레이아웃 변형을 데이터로 처리**했다.
  뷰 재활용 풀이 하나로 유지되므로 스크롤 시 재활용 실패(다른 viewType 간 캐시 미스)가 발생하지 않는다.
- 리소스 ID `-1`을 "없음"의 의미로 쓰고 판정을 어댑터 한곳에 모아, 화면마다 흩어질 수 있는
  "아이콘이 없으면 왼쪽으로 붙인다" 같은 **레이아웃 규칙을 코드로 명문화**했다.

---

## 14. 콜백 스레드를 호출자가 정하는 인터넷 감시자

`manager/network/InternetChecker.java`

```java
/**
 * internet 연결 확인 로직 반복 시작
 *
 * @param listener              internet 연결 상태를 전달받을 listener
 * @param listenerHandlerLooper OnInternetListener callback 을 호출해 줄 thread
 * @param startDelay            최초 연결 확인 delay
 */
public void startWatching(OnInternetListener listener, Looper listenerHandlerLooper, int startDelay) {
    mListener = listener;
    mListenerHandler = new ListenerHandler(listenerHandlerLooper);   // ← 콜백 스레드는 호출자가 결정

    if (mCheckHandlerThread == null) {
        // stopWatching 후 재시작 시 이전 세션의 thread 와 구별하기 위해 매번 새 instance 생성
        mCheckHandlerThread = new HandlerThread(CHECK_THREAD_NAME);
        mCheckHandlerThread.start();
        isWatching = true;
        mCheckHandler = new CheckHandler(mCheckHandlerThread.getLooper());
        mCheckHandler.sendEmptyMessageDelayed(MSG_START_CHECK_INTERNET, startDelay);
    }
}
```

```java
private class CheckHandler extends Handler {          // 워커 스레드 — 실제 검사
    @Override public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START_CHECK_INTERNET:
                removeMessages(MSG_CHECK_INTERNET_RESULT);
                removeMessages(MSG_CHECK_INTERNET);    // 중복 예약 제거 후 재시작
                sendEmptyMessage(MSG_CHECK_INTERNET);
                break;

            case MSG_CHECK_INTERNET:
                boolean connected = handleUrlHeadCheck(fConnectionCheckUrl);
                if (isCurrentSession()) sendMessage(obtainMessage(MSG_CHECK_INTERNET_RESULT, connected));
                break;

            case MSG_CHECK_INTERNET_RESULT:
                // 이전 세션의 thread 가 뒤늦게 보낸 결과는 버린다
                if (isCurrentSession()) {
                    mListenerHandler.sendMessage(
                            mListenerHandler.obtainMessage(MSG_NOTIFY_CONNECTION, msg.obj));
                    if (!hasMessages(MSG_CHECK_INTERNET)) {
                        sendEmptyMessageDelayed(MSG_CHECK_INTERNET, INTERVAL_CHECK_INTERNET);  // 자기 자신을 재예약
                    }
                }
                break;
        }
    }

    /** 이 메시지를 처리 중인 thread 가 아직 살아있는 현재 세션의 thread 인가 */
    private boolean isCurrentSession() {
        return mCheckHandlerThread != null && mCheckHandlerThread.isAlive()
                && mCheckHandlerThread == getLooper().getThread();
    }
}
```

```java
private boolean handleUrlHeadCheck(String url) {
    Response response = null;
    try {
        OkHttpClient client = Ok3Downloader.Companion
                .createClient(url, TIMEOUT_CONNECT_INTERNET, TIMEOUT_CONNECT_INTERNET);
        response = client.newCall(new Request.Builder().url(url)
                .addHeader("User-Agent", USER_AGENT_VALUE).head().build()).execute();   // HEAD — 본문 없음
        int code = response.networkResponse().code();
        return code == HttpCode.HTTP_OK || code == HttpCode.HTTP_NO_CONTENT;
    } catch (Exception e) {
        return false;                                  // 실패 = 연결 없음. 예외를 상태로 환원
    } finally {
        if (response != null) response.close();
    }
}
```

**설계 포인트**

- 4번의 매니저들이 "콜백은 언제나 Main"이라는 계약을 안에서 완결시켰다면,
  여기서는 반대로 **콜백 Looper를 생성자가 아닌 `startWatching()` 인자로 주입받는다.**
  셋업 위저드(UI 스레드에서 즉시 화면 갱신)와 백그라운드 감시(워커 스레드에서 후속 처리)가
  같은 클래스를 쓰기 때문에, **스레드 정책을 라이브러리가 아니라 사용자가 정하게** 열어둔 것이다.
- 스레드 경계가 코드에 그대로 드러난다. `CheckHandler`(전용 `HandlerThread`)가 네트워크를 때리고,
  결과만 `ListenerHandler`(호출자 Looper)로 넘긴다. 어느 콜백이 어느 스레드에서 오는지
  **문서가 아니라 타입과 필드명으로** 알 수 있다.
- **stale 세션 가드**가 이 클래스의 핵심 디테일이다. 타임아웃이 5초, 재검사 간격이 60초이므로
  `stopWatching()` → `startWatching()`을 빠르게 반복하면 **이전 스레드의 검사 결과가 뒤늦게 도착하는 창**이
  실제로 열린다. 매 세션 `HandlerThread`를 새로 만들고 `mCheckHandlerThread == getLooper().getThread()`로
  대조해, 죽은 세션의 결과가 새 리스너를 오염시키는 것을 차단했다.
- 주기 폴링을 `while + sleep`이 아니라 **메시지 재예약**으로 구현했다.
  `quit()` 한 번으로 즉시 종료되고, 대기 중에 스레드를 붙잡지 않는다.
  `MSG_START_CHECK_INTERNET`에서 기존 메시지를 먼저 `removeMessages()` 하므로 예약이 중복 누적되지도 않는다.
- 연결 확인은 **HEAD 요청**만 보낸다. 본문을 받지 않아 트래픽이 최소이고,
  `finally`에서 `response.close()`로 소켓을 반드시 회수한다(8번의 `release()`와 같은 원칙).
- `stopWatching()` / `release()`가 스레드 종료 · 큐 비우기 · **리스너 참조 해제**를 함께 수행한다.
  리스너는 대개 Activity이므로, 참조를 남기면 화면이 사라져도 객체가 살아남는다.

---

## 15. 원격 진단 설계 — 로그 파사드와 필드 탈출구

`utils/Clog.java`, `LauncherApp.java`

셋톱박스는 **사용자가 로그를 뽑아 보내줄 수 없는 기기**다. 이 제약이 두 가지 설계를 만들었다.

```java
public class Clog extends LogUtils {

    private static final boolean IS_CRASHLYTICS_ENABLED = !BuildConfig.DEBUG;

    /** release log — 릴리즈 빌드에서는 크래시 리포트의 breadcrumb 로도 남는다 */
    public static void r(String tag, String message) {
        if (Logger.MODE_CONSOLE == LOG_MODE && IS_CRASHLYTICS_ENABLED) {
            FirebaseCrashlytics.getInstance().log("I/" + tag + ": " + message);
        }
        printReleaseLog(tag, message);
    }

    public static void w(String tag, String message) { /* 동일 + printErrorLog */ }
    public static void e(String tag, String message) { /* 동일 + printErrorLog */ }

    /** debug log — 로컬에만 남는다 */
    public static void d(String tag, String message) {
        printDebugLog(tag, message);
    }
}
```

**설계 포인트**

- **로그 레벨이 곧 원격 전송 정책이다.** `d()`는 로컬에만, `r()`/`w()`/`e()`만 Crashlytics 브레드크럼으로 올라간다.
  8번의 `recordException`이 "무엇이 터졌는가"를 남긴다면, 이쪽은 "**어쩌다 거기까지 갔는가**"를 남긴다.
  크래시 리포트를 열면 직전 로그가 함께 붙어 있어 재현 없이 경로를 추적할 수 있다.
- 모든 로그를 올리면 비용·성능·개인정보가 모두 문제가 된다.
  전송 여부를 별도 플래그가 아니라 **호출하는 함수 이름으로 결정**하게 해서,
  개발자가 "이 로그를 원격에 보낼까"를 로그를 찍는 순간에 자연스럽게 판단하도록 만들었다.
- `IS_CRASHLYTICS_ENABLED`는 `BuildConfig.DEBUG` 기반 **컴파일타임 상수**라
  디버그 빌드에서는 분기와 문자열 결합 자체가 제거된다.
- 벤더 `LogUtils`를 상속한 파사드이므로, 로깅 백엔드가 교체돼도 호출부의 `Clog.d(...)`는 그대로다.

```java
private void setLogMode() {
    boolean isDebug = SysPropertyUtils.getDebuggable(0) == 1;   // 시스템 프로퍼티로 로그 레벨 결정
    Clog.initWithCrashlytics(Logger.MODE_CONSOLE, "[" + TAG + "]", isDebug);
}

/**
 * launcher 무한 재시작 문제 발생 시 해결을 위해 hidden 기능 추가.
 * usb root 에 "_clearlauncherdata" 파일이 존재하는 경우 launcher 시작 시 launcher data clear.
 */
private void hiddenFunctionClearLauncherDataToUsb() {
    File file = new File(LauncherFileUtils.getUsbPath() + "/" + HIDDEN_FILE_NAME_CLEAR_DATA);
    if (file.exists()) {
        File completeFile = new File(file.getParent(), HIDDEN_FILE_NAME_CLEAR_DATA_COMPLETED);
        file.renameTo(completeFile);          // 트리거를 즉시 소진 — 매 부팅 반복 방지 + 실행 흔적
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
    }
}
```

**설계 포인트**

- 런처가 부팅 직후 죽으면 **화면에 아무것도 남지 않는다.** 설정 앱으로 들어갈 수도, 개발자 옵션을 켤 수도 없다.
  그래서 USB 루트에 빈 파일 하나를 넣고 재부팅하면 앱 데이터가 초기화되는 탈출구를 만들었다 —
  개발자가 아니라 **현장 CS가 전화로 안내할 수 있는 수준의 복구 절차**다.
- 파일을 검사만 하지 않고 **`_completed`로 rename 해 소비**하는 것이 핵심이다.
  파일을 그대로 두면 그 USB를 꽂은 채 재부팅할 때마다 초기화가 반복된다.
  이름을 바꿔 1회성으로 만들면서, 동시에 "이 USB로 초기화가 실행됐다"는 증거를 남긴다.
- 릴리즈 빌드에 남아 있는 디버그 훅이므로 트리거를 **USB 물리 접근**으로 제한했다. 원격으로는 건드릴 수 없다.
- 로그 레벨도 빌드 재배포 없이 **시스템 프로퍼티만 바꿔** 올릴 수 있게 열어두었다.

---

## 16. 기기 능력에 따른 기능 축소 — 데이터로 조립하는 설정 메뉴

`LauncherApp.java`, `main/controlcenter/launchersettings/LauncherSettingsDialog.kt`

같은 APK가 RAM 1GB 보급형부터 상위 모델까지, 자사 브랜드부터 OEM 물량까지 커버해야 한다.
빌드를 쪼개는 대신 **런타임 능력 질의 + 데이터 조립**으로 해결했다.

```java
private static final long MAX_MEM_SIZE_LITE_MODEL = 1024;

/** RAM 용량으로 결정되는 저사양 모델 플래그 — 클래스 로딩 시 1회만 계산 */
public static final boolean IS_LITE_MODEL =
        SysPropertyUtils.getRoProductPMemSize(2048) <= MAX_MEM_SIZE_LITE_MODEL;
```

```kotlin
// 메뉴를 뷰가 아니라 "데이터"로 조립한다
arrayListOf<LauncherSettingsItem>().apply {
    add(LauncherSettingsItem(..., type = LauncherSettingsType.STARTUP_APP))
    add(LauncherSettingsItem(..., type = LauncherSettingsType.SCREENSHOT_PATH))

    if (!LauncherApp.IS_LITE_MODEL) {                      // 저사양 모델에서는 배경화면 기능 제외
        add(LauncherSettingsItem(..., type = LauncherSettingsType.WALLPAPER))
    }

    if (SysPropertyUtils.isModelA() || SysPropertyUtils.isModelB()) {   // 지원 모델에서만 노출
        add(LauncherSettingsItem(..., type = LauncherSettingsType.WEATHER))
    }
}
```

```kotlin
// HotkeyActivity — 브랜드 분기는 리소스 ID 하나로 좁힌다 (화이트라벨 대응)
@DrawableRes
private fun getBackgroundResIdByBrand(): Int = when {
    SysPropertyUtils.isOwnBrand() -> R.drawable.hotkey_brand_background
    else                          -> R.drawable.hotkey_oem_background
}
```

> 모델·브랜드 판별 함수명은 문서용으로 일반화해 표기했다.

**설계 포인트**

- 메뉴를 XML 레이아웃이 아니라 `List<LauncherSettingsItem>`으로 조립했기 때문에,
  **기능 하나를 빼는 일이 `add()`를 감싸는 `if` 한 줄**이다.
  뷰 계층·포커스 순서·구분선 위치는 리스트 길이에 맞춰 어댑터가 다시 계산한다.
  레이아웃에 뷰를 직접 배치했다면 모델별로 `visibility` 처리와 포커스 순서 재지정이 흩어졌을 것이다.
- 각 항목이 `type` enum을 들고 있어, **"무엇을 보여줄지"(목록 구성)와 "누르면 무엇을 할지"(타입 분기)가
  분리**된다. 항목 추가는 리스트에 한 줄, 동작 추가는 `when`에 한 줄이다.
- `IS_LITE_MODEL`은 `static final` — 시스템 프로퍼티 조회는 프로세스당 1회로 끝나고
  이후 분기는 상수 비교다. 부팅 경로에서 프로퍼티를 반복 조회하지 않는다.
- 기본값 인자 `getRoProductPMemSize(2048)`의 선택이 의도적이다.
  프로퍼티를 읽지 못한 기기는 **상위 모델로 간주**한다 —
  알 수 없을 때 기능을 꺼서 정상 기기의 메뉴가 사라지는 쪽보다,
  기능을 켜서 저사양 기기가 느려지는 쪽이 회수 가능한 실패이기 때문이다.
- 브랜드 차이를 로직이 아니라 **리소스 ID 반환 함수 하나**로 격리했다.
  OEM이 추가돼도 바뀌는 코드는 이 `when` 절뿐이다.

---

## 17. 이력서용 요약

> **Android TV 런처 (시스템 앱, Kotlin / Java 혼재 · 230+ 파일)**
>
> - **Room 기반 데이터 계층 설계** — 제네릭 `BaseDao<T>` 추상화로 CRUD 보일러플레이트를 제거하고,
>   Entity 상속 계층(`PositionedItem` → `PositionedListDaoItem` → Fav/Recent)으로
>   성격이 다른 두 Row 타입을 단일 테이블에서 다형적으로 처리. 호출부의 `type` 분기 제거.
> - **동시성 제어** — 홈 화면 상태를 공유하는 두 매니저(`PositionedAppMgr` / `RecentMgr`)에
>   `Mutex` 인스턴스를 주입해 임계 구역을 공유하고, `lockedLaunch {}` 단일 진입점으로
>   모든 DB write를 직렬화. 콜백은 매니저 내부에서 Main 디스패처로 복귀시켜 스레드 계약을 완결.
> - **비정상 종료 대응 자가 복구 로직** — 셋톱박스 강제 전원 차단으로 발생하는 `position` 중복을
>   부팅 시 O(n) 검사로 감지해 자동 복구. 제네릭 + `BaseDao<T>`로 3종 리스트를 한 함수로 처리.
> - **드래그 앤 드롭 재정렬 최적화** — 전체 재인덱싱 대신 `subList()`로 영향 구간만 추출해
>   단일 배치 UPDATE로 커밋. 낙관적 업데이트(메모리 즉시 → DB 비동기)로 RCU 반응성 확보.
> - **RecyclerView Adapter 4단 계층 설계** — Template Method 패턴으로 클릭 디바운스 ·
>   D-pad 포커스 애니메이션 · DiffUtil을 상위 계층에 확정하고, 구체 어댑터는 뷰 팩토리 1개만 구현.
>   바인딩 책임을 `ItemView` 인터페이스로 뷰에 위임해 어댑터를 아이템 타입에서 완전히 분리.
> - **HTTP/다운로드 계층 추상화** — `BaseConnection` 전략 인터페이스로
>   `HttpUrlConnection → HttpClient → OkHttp` 2회 구현체 교체를 **호출부 변경 없이** 수행.
>   디버그/릴리즈 예외 정책을 분리해 릴리즈에서는 요청 컨텍스트와 함께 Crashlytics 리포트.
> - **UI 프레임워크화** — Leanback `GuidedStepFragment`를 대체하는 다이얼로그 프레임워크를 구축.
>   서브클래스가 title/adapter 2개만 구현하면 딤·슬라이드 애니메이션·빈 상태·BACK 처리를 상속.
>   dismiss 경로를 전부 오버라이드해 퇴장 애니메이션 누락을 구조적으로 차단.
> - **외부 앱 연동 프로토콜 봉인** — 타 앱 ContentProvider와 공유하는 상수를
>   `RecentType` enum + `@IntDef`/`@StringDef`로 단일화해 컴파일타임 검증 확보.
> - **Hilt DI 적용** — `@ApplicationContext` 명시 주입으로 Activity Context 누수를 차단하고,
>   `onCleared()`에서 DB/코루틴 스코프를 회수. 화면 모드는 nullable data class로 모델링해
>   불가능한 상태 조합을 타입 수준에서 제거.
> - **기능 단위 모듈 구성 (MVVM + Navigation)** — 핫키 설정 화면을 data/viewmodel/fragment/adapter로
>   수직 분할하고, `SavedStateHandle`로 화면 인자를 받아 Fragment에서 인자 파싱 코드를 제거.
>   `ListAdapter` + `DiffUtil` 기반 `BaseAdapter<T>`로 바인딩을 선언형 슬롯 8개로 축약하고,
>   레이아웃 파일 하나를 두 화면이 Guideline 퍼센트만 바꿔 공유.
> - **스레드 경계 설계** — 인터넷 감시자의 콜백 Looper를 호출자가 주입하도록 열어 UI/백그라운드 양쪽에서
>   재사용. 세션 식별로 이전 워커 스레드의 지연 결과를 폐기하고, 주기 폴링을 `sleep`이 아닌
>   메시지 재예약으로 구현해 즉시 종료 가능하게 처리.
> - **원격 진단 · 현장 복구 설계** — 로그 레벨을 곧 원격 전송 정책으로 삼아 릴리즈 로그만 Crashlytics
>   브레드크럼으로 수집하고, USB 파일 트리거 기반 데이터 초기화 탈출구로 화면이 뜨지 않는 기기의
>   현장 복구 경로를 확보.
> - **단일 APK 다기종 대응** — RAM 용량·모델·브랜드를 런타임에 질의해 설정 메뉴를 데이터로 조립.
>   기능 축소가 `if` 한 줄이고, 브랜드 차이는 리소스 ID 반환 함수 하나로 격리.

---

### 부록 — 관련 파일 위치

| 주제 | 파일 |
|---|---|
| 제네릭 DAO | `main/positioned/BaseDao.kt` |
| Entity 계층 | `main/positioned/PositionedItem.kt`, `PositionedListDao.kt`, `PositionedAppDao.kt` |
| DB 정의 | `main/positioned/PositionedListDb.kt` |
| 데이터 매니저 (동시성/복구/재정렬) | `main/positioned/PositionedAppMgr.kt` |
| 최근시청 매니저 | `main/recent/RecentMgr.kt` |
| 도메인 enum | `main/recent/RecentType.kt` |
| Adapter 계층 | `main/grid/GridAdapter.kt`, `GridAdapterContent.kt`, `ItemView.kt`, `GridItemView.kt` |
| Home Adapter | `main/grid/home/GridAdapterContentHome*.kt` |
| HTTP 추상화 | `utils/http/BaseConnection.kt`, `OkHttpClientImpl.kt`, `HttpConnector.kt` |
| 다운로더 | `network/BaseDownloader.kt`, `Ok3Downloader.kt` |
| 다이얼로그 프레임워크 | `common/dialog/FullScreenDialogFragment.kt`, `customguidedstep/*.kt` |
| 하드웨어 추상화 | `setupwizard/IEthernetManager.kt`, `EthernetManagerImpl.kt` |
| DI / ViewModel | `main/MainViewModel.kt`, `LauncherApp.java` |
| Hotkey 기능 모듈 | `main/quicksettings/hotkey/**` (data / viewmodel / fragment / adapter) |
| 인터넷 감시 | `manager/network/InternetChecker.java` |
| 로그 파사드 | `utils/Clog.java` |
| 앱 초기화 · 필드 탈출구 | `LauncherApp.java` |
| 설정 메뉴 조립 | `main/controlcenter/launchersettings/LauncherSettingsDialog.kt` |
