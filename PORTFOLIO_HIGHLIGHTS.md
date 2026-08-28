# 포트폴리오 하이라이트 — 라이브 TV / 설정 모듈

상용 Android IPTV 플레이어 앱을 개발하며 **설계 판단이 들어간 부분**을 정리한 문서입니다.

> 실제 제품 코드는 비공개이므로, 이 문서의 코드는 **설계 구조만 드러나도록 재작성한 요약본**입니다.
> 시그니처와 핵심 흐름은 유지했고, 구현 세부·내부 식별자·사내 의존성은 제거했습니다.

**기술 스택** — Kotlin, Jetpack Compose, Coroutines/Flow, Hilt, Navigation Compose, ExoPlayer 계열 재생 엔진, Room/ContentProvider, 멀티모듈 Gradle

**구성** — 재생·스트리밍·도메인 제공자를 각각 독립 라이브러리 모듈로 분리한 멀티모듈 구조

---

## 아키텍처 개요 — 모듈 경계와 의존성 방향

기능을 추가하기 전에 **의존성이 흐르는 방향**을 먼저 고정했습니다.

```
                        ┌──────────┐
                        │  :app    │   화면 조립 · 네비게이션 · DI 진입점
                        └────┬─────┘
             ┌───────────────┼───────────────┐
             ▼               ▼               ▼
        ┌────────┐     ┌──────────┐    ┌───────────┐
        │ :live  │     │ :stream  │    │ :playback │   기능 계층 (서로 참조하지 않음)
        └───┬────┘     └────┬─────┘    └─────┬─────┘
            └───────────────┼────────────────┘
                    ┌───────┴────────┐
                    ▼                ▼
               ┌─────────┐     ┌────────────┐
               │  :core  │     │ :provider  │   기반 계층 (프로젝트 모듈 의존 없음)
               └─────────┘     └────────────┘
```

**규칙 세 가지**

1. 기반 계층(`:core`, `:provider`)은 **어떤 프로젝트 모듈도 참조하지 않습니다.** Gradle 의존성 블록이 비어 있습니다.
2. 기능 계층(`:live`, `:stream`, `:playback`)은 **서로를 참조하지 않습니다.** 세 모듈은 형제이고, 조립은 오직 `:app`에서 일어납니다.
3. 어떤 모듈도 `:app`을 참조하지 않습니다.

세 규칙이 지켜지므로 **순환 의존이 구조적으로 발생할 수 없습니다.**
기능 모듈끼리 우발적으로 결합되면 코드 리뷰가 아니라 **Gradle 빌드 단계에서 컴파일 에러로** 드러납니다.

실무적인 이득도 있었습니다. 스트리밍 프로토콜 로직을 고쳐도 재생 모듈은 재컴파일 대상이 아니라
빌드 캐시를 그대로 재사용합니다.

---

## 1. 이종(異種) 스트리밍 프로토콜을 하나의 도메인 모델로 추상화

### 문제

지원해야 하는 채널 소스가 5종이었습니다. 각각 인증 방식, 채널 목록 규격, 재생 URL 생성 규칙,
지원 기능(다시보기·성인채널·잠금)이 전부 다릅니다.

- Xtream Codes 계열 API
- Stalker Portal 계열 API
- M3U 플레이리스트
- 사업자 전용 포털
- 방송 튜너(하드웨어)

화면과 재생 로직이 이 차이를 직접 알아야 한다면 분기문이 앱 전체로 번집니다.

### 설계

공통 개념만 추상 클래스로 올리고, 소스별 차이는 하위 클래스에 가뒀습니다.

```kotlin
abstract class Channel {
    val uid: Uid
    val name: String
    val numberMajor: Int
    val numberMinor: Int

    // 소스마다 반드시 달라지는 것 -> 구현 강제
    abstract fun getServerType(): ServerType

    // 기본 동작이 있는 것 -> 필요한 소스만 재정의
    open fun isCatchup(): Boolean = false
    open fun isAdult(): Boolean = false
    open fun isTuner(): Boolean = false
    open fun isFavAvailable(): Boolean = true
    open fun isLockAvailable(): Boolean = true
}
```

`abstract`(구현 강제)와 `open`(기본값 제공)을 의도적으로 구분했습니다.
새 프로토콜을 추가할 때 **무엇을 반드시 채워야 하는지가 타입 시스템에 드러납니다.**

### 합성 식별자를 값 객체로

채널 하나를 특정하려면 `서버 + 그룹 + 채널 + 스트림종류` 네 값이 모두 필요합니다.
네 값을 개별 파라미터로 들고 다니면 인자 순서 실수와 누락이 반복됩니다.

```kotlin
open class Uid : Parcelable {
    val serverId: Int
    val groupId: Int
    val channelId: Long
    val streamType: StreamType

    // 정규 문자열 표현 <-> 객체 양방향 변환 (DB 컬럼, Intent, 로그에 그대로 사용)
    internal constructor(serialized: String) { /* 구분자로 분해 */ }
    override fun toString(): String = listOf(serverId, groupId, channelId, streamType.value)
        .joinToString(DELIMITER)

    // 상위 도메인 키를 파생
    fun getGroupUid(): Group.Uid = Group.Uid(serverId, groupId, streamType)

    // 튜너 채널은 식별자가 3개 더 필요 -> 하위 타입으로 확장하되 서로 섞이지 않도록 방어
    override fun equals(other: Any?): Boolean =
        other is Uid && this::class == other::class && sameValues(other)
}
```

**설계 포인트**

- **정규 문자열 표현**을 값 객체가 직접 소유 → DB·IPC·로그 전 구간에서 동일한 키 표현 사용
- 역직렬화는 팩토리가 **토큰 개수로 하위 타입을 판별**해 복원
- `this::class == other::class` 조건으로 기본 Uid와 확장 Uid가 서로 동등하다고 판정되는 사고를 차단

### 성과

새 프로토콜 대응 시 수정 범위가 **하위 클래스 추가 + 서버 타입 열거값 추가**로 한정됐습니다.
화면·재생 로직에는 프로토콜 분기가 존재하지 않습니다.

**이력서 문장**
> 규격이 상이한 5종 스트리밍 프로토콜을 단일 도메인 모델로 추상화하고, 합성 식별자를 정규
> 문자열 직렬화가 가능한 값 객체로 설계해 DB·IPC·로그 전 구간의 키 표현을 통일

---

## 2. Facade + 역할 인터페이스 분리 (Interface Segregation)

### 문제

채널 관리 로직이 한 클래스에 모이면 수천 줄이 되고, 호출부는 **필요 없는 권한까지 전부** 갖게 됩니다.
"즐겨찾기 화면이 실수로 튜너 스캔을 호출"하는 부류의 사고가 컴파일 단계에서 걸러지지 않습니다.

### 설계

관심사별로 구현을 나누고, 외부에는 **역할별 좁은 인터페이스만** 공개했습니다.

```kotlin
class ChannelMgr {
    // 구현체는 감춘다
    private val favMgr = ChannelMgrFav()
    private val ottMgr = ChannelMgrOtt()
    private val tunerMgr: ChannelMgrTuner = ChannelMgrTunerImpl()

    // 역할 인터페이스만 공개
    val fav: FavChController = favMgr
    val pinnedGroup: PinnedGroupController = ottMgr
    val tuner: TunerController = tunerMgr
}
```

호출부는 `channelMgr.fav.setFav(...)` 처럼 **의도가 드러나는 경로**로만 접근합니다.
구현 클래스의 내부 메서드는 인터페이스 밖으로 새어 나가지 않습니다.

### 같은 인스턴스를 두 역할로 노출

재생 객체에도 같은 원칙을 적용했습니다.

```kotlin
private val player = LivePlayer(policyManager)

val playbackInfo: PlaybackInfo = player          // 조회 전용
val playbackControl: PlaybackControl = player    // 제어 전용 (play/pause/seek/track/ratio...)
```

**하나의 인스턴스**를 두 개의 좁은 인터페이스로 나눠 공개했습니다.
재생 정보만 표시하는 컴포넌트는 제어 API에 **타입 수준에서 접근할 수 없습니다.**

**이력서 문장**
> 채널·재생 도메인에 Facade와 역할 기반 인터페이스 분리를 적용해 호출부가 필요한 권한만
> 갖도록 API 표면을 설계, 의도치 않은 상태 변경을 컴파일 타임에 차단

---

## 3. 재생 상태를 불리언 플래그가 아닌 타입으로 모델링

### 문제

"지금 무엇을 재생 중인가"를 `isLive`, `isCatchup`, `isPreview` 같은 플래그 조합으로 표현하면
**성립할 수 없는 조합**(`isLive && isCatchup`)이 타입상 허용됩니다. 다시보기 전용 데이터(EPG 정보)를
라이브 재생 중에 참조하는 코드도 컴파일됩니다.

### 설계

```kotlin
abstract class PlaybackData(val channel: Channel, val userAgent: String) {
    open val isChannel: Boolean = false
    open val isCatchup: Boolean = false
    var isMaintainPosition: Boolean = false
}

// 실시간 재생 — 추가 데이터 없음
class ChannelData(channel: Channel, userAgent: String) : PlaybackData(channel, userAgent) {
    override val isChannel = true
}

// 다시보기 — 편성 정보와 서버 타입이 반드시 필요
class CatchupData(
    channel: Channel,
    private val serverType: ServerType,
    val epg: Epg,
    userAgent: String
) : PlaybackData(channel, userAgent) {
    override val isCatchup = true
}
```

`epg`는 `CatchupData`에만 존재합니다. **다시보기가 아닌데 편성 정보를 참조하는 코드는
애초에 작성할 수 없습니다.**

상태 보관은 단방향 흐름을 강제했습니다.

```kotlin
class LiveData {
    private val _playback = MutableStateFlow<PlaybackData?>(null)
    val playback = _playback.asStateFlow()   // 외부는 읽기 전용

    private val _group = MutableStateFlow<Group?>(null)
    val group = _group.asStateFlow()

    fun setPlaybackData(data: PlaybackData) { _playback.value = data }
}
```

**이력서 문장**
> 재생 상태를 불리언 플래그 조합 대신 타입 계층으로 모델링해 유효하지 않은 상태 조합을
> 구조적으로 제거하고, StateFlow 기반 단방향 데이터 흐름으로 화면 간 상태를 동기화

---

## 4. 편성표(Grid EPG) — Compose 프리미티브로 직접 구현한 2D 스크롤·줌 뷰포트

이 프로젝트에서 기술적으로 가장 까다로웠던 부분입니다.

### 문제

24시간 × N채널 편성표를 다뤄야 했고, 요구사항은 다음과 같았습니다.

- 세로 스크롤(채널) + 가로 스크롤(시간) 동시 지원
- **핀치 줌으로 시간 축 배율 변경** (5분 단위 ~ 1시간 단위)
- 좌우 끝에 도달하면 **이전/다음 날짜를 이어서 로드**
- "현재 시각으로 이동" 버튼

표준 `LazyRow`/`LazyColumn` 조합으로는 불가능했습니다. X축 배율이 변하면 모든 아이템 크기가
재계산돼야 하고, 날짜가 앞쪽으로 추가되면 **좌표계 원점 자체가 이동**하기 때문입니다.

### 설계 — 논리 좌표계 하나로 환원

화면 좌표가 아니라 **"분(minute) 단위 논리 좌표"** 를 기준으로 잡고, 배율은 `minuteWidth`(분당 dp)
하나로 표현했습니다. 이 결정 덕분에 줌·스크롤·날짜 확장이 모두 같은 좌표계 위의 산술이 됩니다.

```kotlin
@Stable
class GridEpgState {
    /** dp per minute — 이 값 하나가 줌 배율 */
    var minuteWidth by mutableFloatStateOf(DEFAULT_MINUTE_WIDTH_DP)
    var scrollOffsetX by mutableFloatStateOf(0f)
    var scrollOffsetY by mutableFloatStateOf(0f)
    var totalMinutes by mutableIntStateOf(24 * 60)   // 날짜 로드에 따라 증가
}
```

### 4-1. 앵커 보존 핀치 줌

두 손가락 중심점이 **화면상 같은 위치에 고정**되어야 자연스럽습니다.
줌 전후로 앵커의 논리 좌표를 보존하고 스크롤 오프셋을 역산했습니다.

```kotlin
fun zoom(zoomFactor: Float, anchorScreenXPx: Float, density: Density) {
    // 1) 줌 전 — 앵커가 가리키는 논리 좌표(분)를 구한다
    val anchorMinute = toMinute(scrollOffsetX + anchorScreenXPx, density)

    // 2) 배율 갱신 (상·하한 클램프)
    minuteWidth = (minuteWidth * zoomFactor).coerceIn(MIN_MINUTE_WIDTH, MAX_MINUTE_WIDTH)

    // 3) 같은 분이 같은 화면 위치에 오도록 스크롤을 역산
    scrollOffsetX = toPx(anchorMinute, density) - anchorScreenXPx
}
```

### 4-2. 관성 스크롤 중 좌표계 확장

왼쪽으로 플링하는 도중 이전 날짜가 로드되면 **전체 좌표계가 오른쪽으로 밀립니다.**
애니메이션 값을 그대로 두면 스크롤이 왼쪽 한계(0px)에 부딪혀 멈춰버립니다.

애니메이션 값은 건드리지 않고 **누적 보정값(shift)** 으로 해결했습니다.

```kotlin
private var flingXShift = 0f

fun fling(velocityX: Float, ...) = scope.launch {
    flingXShift = 0f
    flingAnimX.snapTo(scrollOffsetX)
    flingAnimX.animateDecay(-velocityX, exponentialDecay()) {
        // 전체 길이와 뷰포트 폭은 플링 도중에도 변하므로 매 프레임 재계산
        val maxScrollX = (totalMinutes * minuteWidthPx - viewportWidthPx).coerceAtLeast(0f)
        scrollOffsetX = (value + flingXShift).coerceIn(0f, maxScrollX)
    }
}

/** 이전 날짜 로드 시 좌표계 이동량을 누적 */
fun shiftFlingX(offsetPx: Float) { flingXShift += offsetPx }
```

### 4-3. 줌과 스크롤 동시 보간

"현재 시각으로 이동"은 위치와 배율을 **둘 다** 바꿔야 합니다.
두 애니메이션을 따로 실행하면 화면이 튀기 때문에, 진행률 0→1 하나로 두 값을 동시에 보간했습니다.

```kotlin
anim.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) {
    minuteWidth = lerp(startWidth, DEFAULT_MINUTE_WIDTH, value)       // 배율 보간
    val anchor = lerp(startAnchor, targetMinute, value)               // 앵커 보간
    scrollOffsetX = toPx(anchor) - viewportWidthPx / 3f               // 매 프레임 역산
}
```

**이력서 문장**
> 표준 컴포넌트로 구현 불가능한 편성표 UI를 위해 Compose 애니메이션 프리미티브 위에 2D
> 스크롤·핀치줌 뷰포트를 직접 구현. 논리 좌표계 기반 설계로 앵커 보존 줌, 관성 스크롤 중
> 좌표계 확장, 줌·스크롤 동시 보간 처리

---

## 5. 하나의 화면으로 서로 다른 도메인 처리 — Strategy 패턴

### 문제

그룹 관리 화면은 **라이브 채널 그룹**과 **VOD/시리즈 카테고리**를 모두 다룹니다.
데이터 출처, ViewModel, 정렬 규칙이 전혀 다르지만 **UI는 완전히 동일**합니다.
화면 코드에 `if (isLive)` 분기를 넣으면 탭이 늘어날 때마다 분기가 증식합니다.

### 설계

화면이 의존하는 것은 인터페이스 하나뿐입니다.

```kotlin
@Stable
interface ManageGroup : TabModule {
    fun getViewModel(): ManageGroupViewModel

    @Composable
    fun ReqGroupGridData(onResponse: (List<GroupData>) -> Unit, onLoading: (Boolean) -> Unit)

    fun setShownGroup(item: Any, isShown: Boolean)
    fun setPinnedGroup(item: Any, isPinned: Boolean)
    fun changePinnedGroupPosition(from: Any, to: Any, fromPos: Int, toPos: Int)
}
```

```kotlin
// 탭 목록 = 전략 목록. 화면 코드에는 도메인 분기가 없다
val tabs = remember {
    listOf(
        LiveManageGroup(liveVm),
        VodManageGroup(vodVm, StreamType.Movie),
        VodManageGroup(vodVm, StreamType.Tv)
    )
}
```

### `@Stable`을 붙인 이유 — Compose 리컴포지션 이슈 해결

특정 탭에 처음 진입할 때 데이터가 일부만 표시되는 버그가 있었습니다.
원인은 **Compose 컴파일러가 인터페이스 타입의 안정성을 추론할 수 없다**는 데 있었습니다.

인터페이스는 구현체가 무엇일지 알 수 없으므로 컴파일러가 unstable로 간주하고,
이를 파라미터로 받는 **컴포저블 전체가 skippable에서 탈락**해 불필요한 리컴포지션이 발생합니다.

구현체가 안정성 계약을 지킨다는 것을 `@Stable`로 명시해 해결했습니다.
Compose 컴파일러 메트릭으로 해당 컴포저블이 skippable로 전환된 것을 확인했습니다.

**이력서 문장**
> Strategy 패턴으로 성격이 다른 두 도메인을 단일 화면 구현으로 통합하고, Compose 안정성
> 계약(`@Stable`)을 명시해 인터페이스 타입으로 인한 리컴포지션 성능 저하를 해결

---

## 6. LazyList와 LazyGrid를 동시에 지원하는 드래그 정렬 추상화

### 문제

드래그 앤 드롭 정렬이 **리스트(1열)와 그리드(N열)** 양쪽에 필요했습니다.
두 레이아웃은 아이템 정보 타입이 다릅니다(`LazyListItemInfo` / `LazyGridItemInfo`).
공통 상위 타입이 없어서 그대로는 로직을 공유할 수 없습니다.

### 설계 — 제네릭 + 추상 확장 프로퍼티

드래그 로직이 실제로 필요로 하는 것은 **아이템의 좌표와 인덱스뿐**입니다.
그 부분만 추상 확장 프로퍼티로 뽑아냈습니다.

```kotlin
abstract class ReorderableState<T>(
    private val scope: CoroutineScope,
    private val maxScrollPerFrame: Float,
    private val onMove: (from: ItemPosition, to: ItemPosition, cancelDrag: () -> Unit) -> Unit,
    private val canDragOver: ((draggedOver: ItemPosition, dragging: ItemPosition) -> Boolean)?,
    private val onDragEnd: ((startIndex: Int, endIndex: Int) -> Unit)?,
    val dragCancelledAnimation: DragCancelledAnimation,
    private val gridCells: Int = 1
) {
    // T가 무엇이든 이 좌표만 제공하면 드래그 로직이 동작한다
    protected abstract val T.left: Int
    protected abstract val T.top: Int
    protected abstract val T.right: Int
    protected abstract val T.bottom: Int
    protected abstract val T.itemIndex: Int
    protected abstract val T.itemKey: Any

    protected abstract val visibleItemsInfo: List<T>
    protected abstract val viewportStartOffset: Int
    protected abstract val viewportEndOffset: Int

    // 히트 판정 / 경계 자동 스크롤 / 취소 애니메이션은 여기서 단일 구현
}
```

하위 클래스는 **좌표 접근자만** 구현하면 됩니다.

```kotlin
class ReorderableLazyListState(...) : ReorderableState<LazyListItemInfo>(...) {
    override val LazyListItemInfo.left get() = 0          // 1열이므로 항상 0
    override val LazyListItemInfo.top get() = offset
    ...
}

class ReorderableLazyGridState(...) : ReorderableState<LazyGridItemInfo>(...) {
    override val LazyGridItemInfo.left get() = offset.x   // N열
    override val LazyGridItemInfo.top get() = offset.y
    ...
}
```

### 프로젝트 고유 요구사항

`canDragOver` 콜백으로 **이동 가능 영역을 제한**했습니다.
고정 그룹과 일반 그룹이 하나의 리스트에 섞여 있어서, 두 영역을 넘나드는 이동과 스티키 헤더 위로의
드롭을 모두 차단해야 합니다.

> 이 추상화 패턴 자체는 오픈소스 드래그 정렬 라이브러리와 구조적으로 유사합니다.
> 제 기여는 **그리드 지원 확장과 앞서 설명한 도메인 제약 조건의 통합**입니다.

**이력서 문장**
> 제네릭과 추상 확장 프로퍼티로 서로 다른 레이아웃의 아이템 정보를 추상화해, 드래그 정렬
> 로직(히트 판정·경계 자동 스크롤·취소 애니메이션)을 리스트/그리드에서 단일 구현으로 공유

---

## 7. 이종 리스트 아이템 모델링과 리컴포지션 범위 최소화

하나의 그리드에 **스티키 헤더 / 고정 그룹 / 일반 그룹** 세 종류가 섞여 들어갑니다.
공통 계약은 최소로 두고, 각 타입이 **자기에게만 필요한 상태**를 갖도록 나눴습니다.

```kotlin
interface GroupData {
    fun getName(): String
    fun getItem(): Any
}

// 헤더만 접기/펼치기 상태를 가진다
abstract class StickyHeaderData : GroupData {
    var expanded: MutableState<Boolean> = mutableStateOf(true)
}

// 일반 그룹만 표시/고정 토글 상태를 가진다 — 아이템 단위 StateFlow
class GroupListData(...) : GroupData {
    private val _isPinned = MutableStateFlow(isPinned)
    val isPinned = _isPinned.asStateFlow()

    private val _isShown = MutableStateFlow(isShown)
    val isShown = _isShown.asStateFlow()
}
```

**설계 포인트** — 상태를 리스트 전체가 아니라 **아이템 단위로 쪼갰습니다.**
토글 하나를 눌렀을 때 해당 아이템만 리컴포지션되고, 수백 개 항목이 있는 화면에서도
스크롤이 끊기지 않습니다.

---

## 8. 다단계 백업/복원 파이프라인과 단계별 취소 정책

### 문제

암호화된 백업 파일을 풀어 4종 데이터베이스를 복원하고, 서버를 순차 재등록하는 기능입니다.
**중간에 끊기면 데이터가 깨지는** 비가역 작업이 포함돼 있습니다.

### 설계 — 상태와 결과를 명시적 열거형으로

```kotlin
enum class RestoreState {
    NONE, DECRYPT, PREPARE_ETC, PREPARE_LIVE, PREPARE_VOD, PREPARE_APP,
    COMMIT, REGISTER, COMPLETE
}

enum class RestoreResult {
    SUCCESS, SUCCESS_STEP1, SUCCESS_STEP2,
    FAIL_DECRYPT, FAIL_PREPARE, FAIL_EMPTY_SERVER, FAIL_COMMIT, FAIL_REGISTER_SERVER
}
```

실패 지점을 하나의 `Boolean`이 아니라 **어디서 왜 실패했는지**가 남는 열거형으로 표현해,
사용자에게 보여줄 메시지 분기와 사후 로그 분석을 모두 처리할 수 있게 했습니다.

### 지연 실행으로 단계 조립

```kotlin
// 아직 실행하지 않고 조립만 한다
val step1: Deferred<RestoreResult> = scope.async(start = CoroutineStart.LAZY) {
    // 복호화 -> 4종 데이터 준비, 각 단계마다 진행률 보고
}
val step2: Deferred<RestoreResult> = scope.async(start = CoroutineStart.LAZY) {
    // DB 커밋 (여기서부터 비가역)
}

scope.launch {
    if (step1.await() != SUCCESS_STEP1) return@launch onFail(...)

    restoreState = RestoreState.COMMIT
    if (step2.await() != SUCCESS_STEP2) return@launch onFail(...)

    // 남은 진행률을 서버 개수로 균등 분배
    restoreState = RestoreState.REGISTER
    registerProgressStep = (100 - STEP3_START_PROGRESS) / serverList.size
    registerServersSequentially(serverList)
}
```

### 핵심 — 단계별 취소 정책

```kotlin
fun cancel() {
    when (restoreState) {
        // 커밋 이후는 취소 불가. 중단되면 DB 정합성이 깨진다
        RestoreState.COMMIT, RestoreState.REGISTER, RestoreState.COMPLETE -> Unit

        // 준비 단계까지는 언제든 안전하게 취소 가능
        else -> scope.cancel(CancellationException("cancelled by user"))
    }
}
```

취소 버튼을 비활성화하는 것으로 끝내지 않고 **취소 요청 자체를 상태 머신에서 판단**하도록 했습니다.
UI 상태와 무관하게 안전성이 보장됩니다.

**이력서 문장**
> 암호화 백업 복원을 다단계 코루틴 파이프라인으로 설계. 지연 실행(`CoroutineStart.LAZY`)으로
> 단계를 선언적으로 조립하고, 비가역 구간의 취소를 차단하는 단계별 취소 정책으로 중단 시
> 데이터 정합성 손상을 방지

---

## 9. 콜백 기반 레거시 API를 선언적 인터페이스로 평탄화

### 문제

서버 등록은 `추가 → 초기화 → (조건부) 로그인 → 완료` 순으로 진행되는데,
하위 SDK가 **단계마다 다른 리스너**를 요구했습니다. 그대로 쓰면 콜백이 3중으로 중첩되고,
해제 지점이 흩어져 리스너 누수가 발생합니다.

### 설계

결과 채널을 **생성자에서 한 번에 선언**하고, 단계 연결과 리스너 수명을 한 클래스가 소유합니다.

```kotlin
class Connector(
    val onAdded: (server: Server) -> Unit,
    val onReplaced: (server: Server) -> Unit,
    val onInitialized: (server: Server) -> Unit,
    val onInitFailed: (msg: String) -> Unit,
    val onLoginRequired: (connector: Connector, serverId: Int) -> Unit
) {
    // 진행률은 콜백이 아니라 Flow로 — UI가 원할 때 구독
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    // 다음 단계 연결을 내부에서 처리 — 호출부는 체이닝을 모른다
    private val addedListener = object : OnAddListener {
        override fun onAdded(server: Server) {
            this@Connector.onAdded(server)
            registerInitListener(initListener)
            sdk.initServer(server)
        }
    }

    // 해제 지점을 한 곳으로 모아 누수를 구조적으로 방지
    fun release() {
        unregisterAddListener(addedListener)
        unregisterReplaceListener(replacedListener)
        unregisterInitListener(initListener)
        sdk.cancelLastRequest()
    }
}
```

호출부는 `Connector(onAdded = {...}, onInitialized = {...}, ...)` 한 번으로 전체 흐름을 선언하고,
진행률은 `connector.progress`를 구독하기만 하면 됩니다.

---

## 10. 재생 품질 자동 보정 — 재생 상태 구독형 코루틴 상태 머신

### 문제

라이브 스포츠 중계에서 스트리밍 지연이 누적되면 실제 경기보다 늦은 장면을 보게 됩니다.
버퍼가 충분히 쌓였을 때 재생 속도를 미세하게 올려 **라이브 엣지에 따라붙는** 기능이 필요했습니다.

단순히 속도를 올리면 버퍼가 고갈돼 재버퍼링이 발생하므로, 감시·발동·쿨다운을 분리해야 합니다.

### 설계

```kotlin
class SportModeMgr(private val pb: PlaybackControl, private val liveData: LiveData) {
    companion object {
        private const val INTERVAL_CHECK_BUFFER = 1_000L            // 평상시 감시 주기
        private const val INTERVAL_CHECK_BUFFER_IN_RUNNING = 100L   // 동작 중엔 촘촘하게
        private const val INTERVAL_COOLDOWN = 10_000L               // 재진입 방지
        private const val MIN_BUFFERED_TIMES_MS = 4_000L            // 발동 임계값
    }

    // 역할별로 Job을 분리 — 서로 독립적으로 취소 가능
    private var checkBufferJob: Job? = null   // 버퍼 감시
    private var loopJob: Job? = null          // 속도 보정 루프
    private var cooldownJob: Job? = null      // 재진입 쿨다운

    init {
        pb.registerCallback(pbCallback)

        // 재생 대상이 바뀌면 스스로 정리하고 재시작 — UI가 관여하지 않는다
        scope.launch {
            liveData.playback.collect { data ->
                stop()
                if (data?.isChannel == true) startCheckBuffer()
            }
        }
    }

    fun release() {
        pb.unregisterCallback(pbCallback)
        scope.cancel()
    }
}
```

**설계 포인트** — 이 기능의 생명주기를 **화면이 관리하지 않습니다.**
재생 상태 스트림을 직접 구독해 스스로 시작·정지하므로, 화면 전환·PiP 진입·백그라운드 복귀 등
어떤 경로로 채널이 바뀌어도 동작이 일관됩니다. 화면 쪽에 정리 코드를 넣을 필요가 없습니다.

**이력서 문장**
> 라이브 스트리밍 지연 보정 기능을 재생 상태 스트림 구독형 코루틴 상태 머신으로 구현.
> 감시 주기·발동 임계값·쿨다운을 분리하고 UI 생명주기와 독립적으로 동작하도록 설계

---

## 11. 반응형 치수 처리와 재사용 컴포넌트

### 폼팩터별 치수를 선언적으로

폰/태블릿 × 세로/가로 조합마다 여백과 크기가 달라야 했습니다.
컴포저블마다 조건 분기를 넣는 대신, **값 자체가 문맥을 아는** 래퍼를 만들었습니다.

```kotlin
Modifier.padding(
    start = remember { AdaptiveValue(mobile = 16.dp, tablet = 24.dp) }.get(),
    end   = remember { AdaptiveValue(mobile = 0.dp,  tablet = 12.dp) }.get()
)
```

### 디자인 시스템 컴포넌트

Material 기본 컴포넌트로 표현이 안 되는 것은 `Canvas`로 직접 그렸습니다.

```kotlin
@Composable
fun CustomSwitch(width: Dp, height: Dp, enabled: Boolean, isOn: MutableState<Boolean>, onClick: () -> Unit) {
    val thumbX by animateFloatAsState(
        targetValue = with(LocalDensity.current) { (if (isOn.value) width - thumbRadius else thumbRadius).toPx() }
    )

    Canvas(modifier = Modifier.size(width, height).pointerInput(Unit) { detectTapGestures { onClick() } }) {
        drawRoundRect(...)                                    // 트랙 — 컨테이너 높이의 70%
        drawCircle(center = Offset(thumbX, size.height / 2))  // 썸 — 애니메이션 위치
    }
}
```

---

## 12. Repository + Policy Delegate — 프로토콜 구현을 저장소 뒤로 격리

### 문제

VOD/시리즈는 프로토콜마다 **페이지네이션 방식 자체가 다릅니다.** 어떤 프로토콜은 서버가 페이지를
끊어 주고, 어떤 프로토콜은 전체를 내려받아 로컬 DB에서 잘라야 합니다. 정렬·필터 옵션도 지원 범위가
다릅니다. 이걸 화면이 알게 되면 목록 화면마다 프로토콜 분기가 생깁니다.

### 설계 — 정책을 추상 타입 하나로 통일

```kotlin
class StreamRepository @Inject constructor(
    private val database: VodDatabase,
    private val manager: ServerProviderMgr,

    // 프로토콜별 정책 — 전부 PolicyDelegate 하위 타입
    private val stalker: StalkerPolicyDelegate,
    private val xtream: XtreamPolicyDelegate,
    private val playlist: PlaylistPolicyDelegate,

    // 저장소 기반 가상 카테고리(즐겨찾기 · 시청기록 · 최근)
    private val mixed: MixedPolicyDelegate,

    // 외부 메타데이터 소스
    private val tmdb: TmdbPolicyDelegate,
    private val external: ExternalPolicyDelegate,

    private val preference: StreamPreference
)
```

```kotlin
/*
 * Delegate 는 DB 가 올려주는 ENTITY 를 다룬다.
 * ENTITY -> UI 도메인 모델 변환은 Repository 의 책임.
 */
abstract class PolicyDelegate(val database: VodDatabase) {

    internal abstract fun getStreamByCategory(
        category: Category,
        optionSource: OptionSource
    ): Flow<PagingData<Stream>>

    internal abstract fun search(
        query: String, server: StreamServer, streamType: StreamType
    ): Flow<PagingData<Stream>>

    // 프로토콜이 지원하는 정렬/필터만 노출
    internal abstract fun getSortOptions(streamType: StreamType): List<Option.Sort>
    internal abstract fun getFilterGroupOptions(streamType: StreamType): List<Option.Filter.Group>
    internal abstract fun getDefaultOptionSource(identifier: Identifier): OptionSource

    // ENTITY -> 도메인 모델
    internal abstract fun buildStream(...): Stream
}
```

**설계 포인트**

- **반환 타입을 `Flow<PagingData<Stream>>` 하나로 통일했습니다.** 서버 페이징이든 로컬 DB 페이징이든
  화면이 받는 것은 동일한 Paging 스트림입니다. 목록 화면에 프로토콜 분기가 존재하지 않습니다.
- **`internal` 한정자로 모듈 경계를 명시했습니다.** delegate API 전체가 모듈 내부이고, `:app`에서
  보이는 것은 `StreamRepository` 하나뿐입니다. 프로토콜 구현이 앱 쪽으로 새어 나갈 수 없습니다.
- **계층 책임을 주석이 아니라 타입으로 못 박았습니다.** delegate는 DB Entity까지, Entity → 도메인
  모델 변환은 Repository가 담당합니다. 구현체가 늘어도 경계가 흐려지지 않습니다.
- **생성자 주입만 사용했습니다.** 대체 구현을 끼워 넣는 지점이 생성자 하나로 고정됩니다.

### DI 스코프를 의도적으로 좁힘

```kotlin
@InstallIn(ActivityRetainedComponent::class)   // ViewModel 수명에 묶음
@Module
internal object StreamModule { ... }
```

무조건 `SingletonComponent`에 넣지 않고, 이 의존성들이 실제로 필요한 수명인 **ViewModel 수명**에
맞췄습니다. 모듈 자체도 `internal`이라 다른 모듈에서 이 바인딩을 가져다 쓸 수 없습니다.

**이력서 문장**
> VOD 도메인을 Repository + 프로토콜별 Policy Delegate 구조로 설계하고, 페이징 방식이 상이한
> 프로토콜들을 단일 `Flow<PagingData<T>>` 계약으로 통일. `internal` 가시성으로 모듈 경계를 강제해
> 프로토콜 구현이 앱 계층에 노출되지 않도록 격리

---

## 13. 능력(capability) 인터페이스로 상세 모델의 API 표면 분해

### 문제

VOD 상세 화면 모델은 필드가 20개가 넘고, 즐겨찾기·시청기록·화질·재생정보 같은 **성격이 다른 동작**이
한 클래스에 모입니다. 게다가 프로토콜에 따라 **지원하지 않는 기능**이 있습니다.

### 설계 — 관심사별 인터페이스 + 널 가능 헬퍼

동작을 관심사 단위 인터페이스로 쪼개고, 상세 모델이 필요한 것만 구현하게 했습니다.

```kotlin
interface SupportFavorite {
    val favoriteHelper: FavoriteHelper?
    suspend fun recordFavorite(isRecord: Boolean)
    suspend fun isFavorite(): Boolean
    fun isFavoriteFlow(): Flow<Boolean>
}

interface SupportHistory {
    val historyHelper: HistoryHelper?
    suspend fun recordHistory(position: Long, duration: Long, recordTime: Long)
    suspend fun removeHistory()
    fun getHistoryFlow(): Flow<History>
}

interface SupportQuality  { suspend fun getQualityFlow(): Flow<List<Quality>>? }
interface SupportPlayback { suspend fun getPlaybackFlow(): Flow<Playback> }
```

**지원 여부는 헬퍼의 널 가능성으로 표현**하고, 미지원 시의 기본 동작을 모델이 직접 정의했습니다.

```kotlin
data class Detail private constructor(
    val identifier: Identifier,
    ...
    override val favoriteHelper: FavoriteHelper?,          // 미지원 프로토콜은 null
    override val historyHelper: HistoryHelper? = null
) : Parcelable, SupportFavorite, SupportHistory, SupportPlayback, SupportQuality {

    // 스레드 정책을 모델이 소유 — 호출부가 IO 디스패처를 잊을 수 없다
    override suspend fun recordFavorite(isRecord: Boolean) =
        withContext(Dispatchers.IO) { favoriteHelper?.recordFavoriteInternal(isRecord) }

    // 미지원이면 예외가 아니라 '비어 있는 스트림'으로 수렴
    override fun isFavoriteFlow(): Flow<Boolean> =
        favoriteHelper?.isFavoriteFlowInternal() ?: flowOf(false)
}
```

### 생성 경로를 프로토콜별 팩토리로 고정

```kotlin
class Detail private constructor(...) {
    companion object {
        internal fun createByXtream(
            stream: Stream, nativeStream: VodContentEntity?, nativeDetail: XtcDetail?,
            historyHelper: HistoryHelper?, favoriteHelper: FavoriteHelper
        ): Detail {
            require(stream.identifier.protocol == Protocol.Xtream)   // 불변식 방어
            ...
        }
        // createByStalker(...), createByPlaylist(...) — 프로토콜별 진입점
    }
}
```

생성자를 `private`으로 막고 **프로토콜별 명명 팩토리만** 열었습니다. 어떤 프로토콜의 상세인지가
호출 지점의 함수 이름에 드러나고, `require`로 식별자와 팩토리가 어긋나는 조합을 런타임 초입에서
잡습니다.

**설계 포인트**

- 화면은 `detail.isFavoriteFlow()`만 구독합니다. 지원 여부 분기가 화면에 없습니다.
- `Dispatchers.IO` 전환을 모델 안에 가뒀습니다. 호출부에서 스레드를 잘못 쓸 여지를 없앴습니다.
- 미지원을 예외가 아닌 **빈 값으로 수렴**시켜, 프로토콜이 늘어도 화면에 방어 코드가 늘지 않습니다.

> **한계로 인정하는 부분** — 현재 `Detail`은 네 인터페이스를 모두 구현하고, 실제 지원 여부는
> 헬퍼의 널 여부로만 판별됩니다. 능력을 타입으로 완전히 분리하면 `is SupportFavorite` 검사만으로
> 판별이 가능해집니다. 면접에서 먼저 꺼낼 개선 지점으로 준비해 두고 있습니다.

---

## 14. URI 기반 합성 식별자 — 프로토콜마다 다른 키 조합을 한 타입으로

### 문제

VOD 쪽 식별자는 라이브 채널(1번 항목의 `Uid`)보다 복잡합니다. 영화는 `스트림 ID`까지면 되지만
시리즈는 `시즌 · 에피소드`, 다시보기는 `catchup ID`, 다중 화질은 `quality ID`가 더 필요합니다.
프로토콜마다 **필요한 키의 개수와 종류가 다릅니다.**

필드를 전부 nullable로 선언하면 "이 조합에서 어떤 필드가 유효한지"를 아무도 모르게 됩니다.

### 설계 — 내부 표현을 `Uri` 하나로

```kotlin
class Identifier private constructor(private val source: Uri) : Parcelable {

    /**
     * {PROTOCOL}://{HOST}?server_id=..&stream_type=..&stream_id=..&season_id=..
     * SCHEME - stalker, xtream, playlist
     */

    val protocol: Protocol     get() = Protocol.from(source.scheme!!)
    val serverId: Int          get() = source.getQueryParameter(KEY_OF_SERVER_ID)?.toInt() ?: UNSET
    val streamType: StreamType get() = StreamType.from(source.getQueryParameter(KEY_OF_STREAM_TYPE) ?: EMPTY)
    val streamId: String       get() = source.getQueryParameter(KEY_OF_STREAM_ID) ?: EMPTY
    val seasonId: String?      get() = source.getQueryParameter(KEY_OF_SEASON_ID)   // 시리즈에만 존재

    // 생성 경로는 Builder 와 deserialize 둘뿐
    class Builder(
        private val protocol: Protocol = Protocol.Unknown,
        private val serverId: Int = UNSET,
        private val streamType: StreamType = StreamType.Unknown,
        private val streamId: String = EMPTY,
        ...
    ) {
        fun build(): Identifier = Identifier(Uri.Builder().apply { ... }.build())
    }

    companion object {
        fun deserialize(identifierString: String) = Identifier(Uri.parse(identifierString))
        val EMPTY_IDENTIFIER = Builder().build()
    }
}
```

**설계 포인트**

- **필드를 늘리지 않고 키 조합을 확장합니다.** 새 개념(화질·다시보기)이 생겨도 쿼리 파라미터 키가
  하나 늘 뿐, 기존 사용처는 영향을 받지 않습니다.
- **문자열 표현이 곧 저장 표현입니다.** DB 컬럼, 딥링크, `Parcelable` 전달에 같은 값을 씁니다.
  직렬화 포맷과 파서를 따로 관리하지 않습니다.
- 생성자를 막고 `Builder`/`deserialize`만 열어 **반쯤 채워진 식별자**가 만들어지지 않게 했습니다.
- `EMPTY_IDENTIFIER` 상수를 두어 초기 상태를 nullable 대신 값으로 표현했습니다.

> **트레이드오프** — 프로퍼티 접근마다 쿼리 파라미터 파싱이 일어납니다. 목록 렌더링처럼 반복
> 접근하는 경로에서는 지역 변수로 한 번만 읽도록 했습니다. 이 비용을 인지하고 선택한 설계입니다.

---

## 15. 재생 엔진 2종을 하나의 계약 뒤로 — internal 경계 + Factory + Facade

### 문제

지원해야 하는 코덱·컨테이너 범위가 넓어 **재생 엔진을 두 개** 사용합니다. 엔진마다 API가 전혀 다르고,
트랙 선택 모델은 특히 차이가 큽니다. 화면이 엔진을 알면 재생 관련 코드 전부가 엔진에 묶입니다.

### 설계

```kotlin
// 모듈 내부에만 존재하는 엔진 계약
internal interface PlaybackController {
    fun start(startTimeMs: Long, source: Source, retainSpeed: Boolean = false): Boolean
    fun seekTo(position: Long): Long
    fun resume(@ResumePauseChangeReason reason: Int)
    fun pause(@ResumePauseChangeReason reason: Int)

    fun getTracks(@TrackType trackType: Int): List<Track>
    fun setTrack(track: Track): Boolean
    fun isTrackOffSupported(@TrackType type: Int): Boolean

    val playerView: View
    var speed: Float
    var aspectRatioMode: Int
    val resolution: Pair<Int, Int>
}

internal class PlaybackControllerFactory {
    fun setType(@ControllerType type: Int) = apply { this.type = type }
    fun setContext(context: Context) = apply { this.context = context }

    fun create(): PlaybackController = when (type) {
        TYPE_EXO_PLAYER -> ExoControllerImpl(requireNotNull(context), playWhenReady)
        TYPE_VLC_PLAYER -> VlcControllerImpl(requireNotNull(context))
        else -> throw IllegalStateException("Unexpected value: $type")
    }
}
```

```kotlin
// 모듈 밖으로 공개되는 것은 이 인터페이스 하나
interface PlaybackSession {
    fun initialize(context: Context)
    fun release(context: Context)
    fun start(...)
    fun registerCallback(callback: PlaybackSessionCallback)
    val playerView: View
    var speed: Float
    ...
}
```

**설계 포인트**

- **`internal`이 핵심입니다.** `PlaybackController`도 `PlaybackControllerFactory`도 엔진 구현체도
  모듈 밖에서 **참조 자체가 불가능**합니다. 화면은 `PlaybackSession`만 알고, 어떤 엔진이 도는지
  모릅니다. "엔진을 직접 만지지 말자"는 규약이 아니라 컴파일러가 강제하는 경계입니다.
- 엔진 추가·교체 시 영향 범위가 **`create()`의 분기 한 줄과 새 구현 클래스**로 한정됩니다.
- 엔진별 차이가 가장 큰 트랙 선택은 `TrackSelectorDelegate`로 한 겹 더 분리하고, 엔진의 트랙 타입과
  도메인 트랙 타입을 양방향 변환 함수로 묶어 두었습니다.

```kotlin
interface TrackSelectorDelegateV2 {
    companion object {
        fun parseTrackTypeToRendererType(@TrackType trackType: Int): @C.TrackType Int = ...
        fun parseRendererTypeToTrackType(rendererType: @C.TrackType Int): Int = ...
        fun isTrackTypeSupported(@TrackType trackType: Int): Boolean = ...
    }
}
```

- 실행 중 켜고 끌 수 있어야 하는 부가 기능(오디오 포커스·시크 반복·속도 보정 등)은 공통 수명주기
  인터페이스로 묶어, 세션이 일괄로 초기화·해제합니다.

```kotlin
interface FlexibleProvider {
    fun initialize()
    fun release()
}
```

**이력서 문장**
> 코덱 지원 범위가 다른 재생 엔진 2종을 단일 `PlaybackController` 계약과 Factory 뒤로 캡슐화하고,
> `internal` 가시성으로 엔진 구현을 모듈 내부에 봉인. 상위 계층은 `PlaybackSession` Facade만 의존해
> 엔진 교체 영향 범위를 팩토리 분기로 한정

---

## 이력서 요약안

이력서에는 프로젝트 한 줄 소개와 함께 **3~5개만** 골라 쓰는 것을 권합니다.

> **상용 Android IPTV 플레이어 앱** — Kotlin, Jetpack Compose, Coroutines/Flow, Hilt, 멀티모듈
>
> - 기반·기능·조립 3계층 멀티모듈 구조를 설계하고 `internal` 가시성으로 모듈 경계를 강제해,
>   프로토콜·재생 엔진 구현이 앱 계층에 노출되지 않고 순환 의존이 발생할 수 없도록 구성
> - 규격이 상이한 5종 스트리밍 프로토콜을 단일 도메인 모델로 추상화하고, 합성 식별자를 정규
>   문자열 직렬화가 가능한 값 객체로 설계해 DB·IPC·로그 전 구간의 키 표현을 통일
> - Facade와 역할 기반 인터페이스 분리를 적용해 호출부가 필요한 권한만 갖도록 API 표면을 설계,
>   의도치 않은 상태 변경을 컴파일 타임에 차단
> - 표준 컴포넌트로 구현 불가능한 편성표 UI를 Compose 애니메이션 프리미티브 위에 2D
>   스크롤·핀치줌 뷰포트로 직접 구현 (앵커 보존 줌, 관성 스크롤 중 좌표계 확장, 줌·스크롤 동시 보간)
> - Strategy 패턴으로 성격이 다른 두 도메인을 단일 화면 구현으로 통합하고, Compose 안정성
>   계약(`@Stable`)을 명시해 리컴포지션 성능 저하 해결
> - 암호화 백업 복원을 다단계 코루틴 파이프라인으로 설계, 비가역 구간의 취소를 차단하는
>   단계별 취소 정책으로 중단 시 데이터 정합성 손상 방지
> - 코덱 지원 범위가 다른 재생 엔진 2종을 단일 계약과 Factory 뒤로 캡슐화해, 엔진 교체 영향
>   범위를 팩토리 분기로 한정

---

## 예상 면접 질문

| 항목 | 질문 | 준비 방향 |
|---|---|---|
| 1 | "새 프로토콜을 추가하면 어디를 고쳐야 하나요?" | 하위 클래스 + 서버 타입 추가로 한정된다는 점 |
| 2 | "왜 매니저를 통째로 노출하지 않았나요?" | 실제로 막힌 사고 사례를 들 것 |
| 3 | "sealed class를 쓰지 않은 이유는?" | 개선안으로 제시 가능한 지점 |
| 4 | "왜 LazyRow를 쓰지 않았나요?" | X축 배율 변경과 좌표계 확장이 불가능 |
| 5 | "Compose 안정성이 뭔가요? 왜 인터페이스가 unstable인가요?" | 컴파일러 메트릭 확인 방법까지 |
| 6 | "오픈소스 라이브러리를 쓰지 않은 이유는?" | 그리드 지원과 도메인 제약이 필요했음 |
| 7 | "수백 개 항목에서 토글 하나 눌렀을 때 어디가 다시 그려지나요?" | 아이템 단위 StateFlow로 범위를 좁힌 근거 |
| 8 | "복원 중 앱이 강제 종료되면 어떻게 되나요?" | 솔직한 현재 한계 + 개선안 |
| 개요 | "모듈을 왜 이렇게 나눴나요? 순환 의존은 어떻게 막나요?" | 3계층 규칙과 Gradle이 강제한다는 점 |
| 12 | "페이징 방식이 다른 프로토콜을 어떻게 한 타입으로 묶었나요?" | `Flow<PagingData<T>>` 통일과 `internal` 경계 |
| 13 | "지원하지 않는 기능은 어떻게 처리하나요?" | 널 가능 헬퍼 + 빈 값 수렴, 그리고 개선안 |
| 14 | "식별자를 왜 Uri로 표현했나요? 파싱 비용은?" | 확장성 vs 파싱 비용 트레이드오프 |
| 15 | "재생 엔진을 바꾸면 어디를 고치나요?" | 팩토리 분기 + 새 impl로 한정된다는 점 |

---

## 개선하고 싶은 점 (면접에서 먼저 꺼내면 좋은 것)

자기 코드의 한계를 아는 것도 평가 요소입니다.

- **일부 클래스의 비대화** — 라이브 ViewModel이 600줄을 넘습니다. 채널 선택 / 서버 관리 /
  즐겨찾기로 분리하고 화면별 ViewModel을 두는 편이 맞습니다.
- **`Any` 타입 사용** — 이종 리스트를 다루느라 `getItem(): Any` 같은 시그니처가 남았습니다.
  제네릭이나 sealed 계층으로 대체 가능합니다.
- **재생 상태의 `sealed` 미적용** — `PlaybackData` 계층은 `sealed`로 바꾸면 `when` 분기의
  누락을 컴파일러가 잡아줍니다.
- **능력 인터페이스의 절반만 타입으로 표현** — 상세 모델이 `Support*` 인터페이스를 전부 구현하고
  실제 지원 여부는 헬퍼의 널 여부로 판별합니다. 지원하는 인터페이스만 구현하도록 바꾸면
  `is SupportFavorite` 검사만으로 판별이 끝납니다.
- **테스트 부재** — 도메인 모델과 좌표 변환 로직은 순수 함수라 단위 테스트하기 좋은 구조인데
  실제 테스트는 부족합니다. 특히 Policy Delegate는 생성자 주입이라 대역을 끼우기 쉬운데도
  테스트가 없습니다.
