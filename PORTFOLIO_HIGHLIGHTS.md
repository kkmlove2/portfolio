# 포트폴리오 하이라이트 — 라이브 TV / 설정 모듈

상용 Android IPTV 플레이어 앱을 개발하며 **설계 판단이 들어간 부분**을 정리한 문서입니다.

> 실제 제품 코드는 비공개이므로, 이 문서의 코드는 **설계 구조만 드러나도록 재작성한 요약본**입니다.
> 시그니처와 핵심 흐름은 유지했고, 구현 세부·내부 식별자·사내 의존성은 제거했습니다.

**기술 스택** — Kotlin, Jetpack Compose, Coroutines/Flow, Hilt, Navigation Compose, ExoPlayer 계열 재생 엔진, Room/ContentProvider, 멀티모듈 Gradle

**규모** — 라이브 TV 모듈 약 50개 파일, 설정 모듈 약 57개 파일, 도메인 모듈 별도 분리

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

화면과 재생 로직이 이 차이를 알아야 한다면 분기문이 앱 전체로 번집니다.

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
이걸 파라미터로 흩어 들고 다니면 순서 실수와 누락이 계속 생깁니다.

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
- `this::class == other::class` 비교로 기본 Uid와 확장 Uid가 동등 비교에서 섞이는 사고를 차단

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
"즐겨찾기 화면이 실수로 튜너 스캔을 호출"하는 부류의 사고가 컴파일 단계에서 걸리지 않습니다.

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

24시간 × N채널 편성표를 다뤄야 했습니다. 요구사항이 이랬습니다.

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
애니메이션 값을 그대로 두면 스크롤이 0px 벽에 부딪혀 멈춰버립니다.

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
두 애니메이션을 따로 돌리면 화면이 튀어서, 진행률 0→1 하나로 두 값을 동시에 보간했습니다.

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
고정 그룹과 일반 그룹이 하나의 리스트에 섞여 있어서, 고정 그룹을 일반 영역으로 끌어다 놓거나
그 반대가 되면 안 됩니다. 스티키 헤더 위로 드롭하는 것도 막아야 합니다.

> 이 추상화 패턴 자체는 오픈소스 드래그 정렬 라이브러리와 구조적으로 유사합니다.
> 제 기여는 **그리드 지원 확장과 위 도메인 제약 조건 통합**입니다.

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
사용자에게 보여줄 메시지와 로그 분석 모두를 커버했습니다.

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
진행률은 `connector.progress`를 collect 하면 끝입니다.

---

## 10. 재생 품질 자동 보정 — 재생 상태 구독형 코루틴 상태 머신

### 문제

라이브 스포츠 중계에서 스트리밍 지연이 누적되면 실제 경기보다 늦게 봅니다.
버퍼가 충분히 쌓였을 때 재생 속도를 미세하게 올려 **라이브 엣지에 따라붙는** 기능이 필요했습니다.

단순히 속도를 올리면 버퍼가 고갈돼 재버퍼링이 발생하므로, 감시·발동·쿨다운을 분리해야 합니다.

### 설계

```kotlin
class SportModeMgr(private val pb: PlaybackControl, private val liveData: LiveData) {
    private const val INTERVAL_CHECK_BUFFER = 1_000L            // 평상시 감시 주기
    private const val INTERVAL_CHECK_BUFFER_IN_RUNNING = 100L   // 동작 중엔 촘촘하게
    private const val INTERVAL_COOLDOWN = 10_000L               // 재진입 방지
    private const val MIN_BUFFERED_TIMES_MS = 4_000L            // 발동 임계값

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

## 이력서 요약안

프로젝트 한 줄 소개와 함께 **3~5개만** 골라 쓰는 것을 권합니다.

> **상용 Android IPTV 플레이어 앱** — Kotlin, Jetpack Compose, Coroutines/Flow, Hilt, 멀티모듈
>
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
| 8 | "복원 중 앱이 강제 종료되면 어떻게 되나요?" | 솔직한 현재 한계 + 개선안 |

---

## 개선하고 싶은 점 (면접에서 먼저 꺼내면 좋은 것)

자기 코드의 한계를 아는 것도 평가 요소입니다.

- **일부 클래스의 비대화** — 라이브 ViewModel이 600줄을 넘습니다. 채널 선택 / 서버 관리 /
  즐겨찾기로 분리하고 화면별 ViewModel을 두는 편이 맞습니다.
- **`Any` 타입 사용** — 이종 리스트를 다루느라 `getItem(): Any` 같은 시그니처가 남았습니다.
  제네릭이나 sealed 계층으로 대체 가능합니다.
- **재생 상태의 `sealed` 미적용** — `PlaybackData` 계층은 `sealed`로 바꾸면 `when` 분기의
  누락을 컴파일러가 잡아줍니다.
- **테스트 부재** — 도메인 모델과 좌표 변환 로직은 순수 함수라 단위 테스트하기 좋은 구조인데
  실제 테스트는 부족합니다.
