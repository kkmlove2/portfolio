# Android Portfolio — Highlights

## 공개 목적

Android 개발에서의 **설계, 추상화, 책임 분리, 문제 해결 능력**을 보여주기 위한 공개용 포트폴리오입니다.

실제 서비스의 구현 세부사항, 내부 식별자, 저장 구조 및 사내 의존성은 공개하지 않고 핵심 설계와 코드 구조만 skeleton 형태로 표현했습니다.

**기술 스택** — Kotlin, Jetpack Compose, Coroutines/Flow, Hilt, Navigation Compose, ExoPlayer 계열 재생 엔진, 멀티모듈 Gradle

---

## 아키텍처 개요 — 모듈 경계와 의존성 방향

기능을 추가하기 전에 의존성이 흐르는 방향을 먼저 고정했습니다.

```text
                        ┌──────────┐
                        │  :app    │   화면 조립 · 네비게이션 · DI 진입점
                        └────┬─────┘
             ┌───────────────┼───────────────┐
             ▼               ▼               ▼
        ┌────────┐     ┌──────────┐    ┌───────────┐
        │ :live  │     │ :stream  │    │ :playback │
        └───┬────┘     └────┬─────┘    └─────┬─────┘
            └───────────────┼────────────────┘
                    ┌───────┴────────┐
                    ▼                ▼
               ┌─────────┐     ┌────────────┐
               │  :core  │     │ :provider  │
               └─────────┘     └────────────┘
```

### 규칙

1. 기반 계층은 feature module을 참조하지 않습니다.
2. feature module끼리는 서로 직접 참조하지 않습니다.
3. 화면 조립과 DI 진입점은 `:app`에 한정합니다.

이 구조를 통해 순환 의존을 구조적으로 방지하고, 기능별 변경 범위를 예측 가능하게 만들었습니다.

---

## 1. 이종 스트리밍 프로토콜을 하나의 도메인 모델로 추상화

서로 다른 channel source가 인증 방식, 목록 규격, 재생 URL 생성 규칙 및 지원 기능에서 차이를 갖는 상황을 하나의 domain model로 통합했습니다.

```kotlin
abstract class Channel {
    val uid: ChannelUid
    val name: String

    abstract fun serverType(): ServerType

    open fun isCatchup(): Boolean = false
    open fun isAdult(): Boolean = false
    open fun isTuner(): Boolean = false
    open fun isFavAvailable(): Boolean = true
}
```

`abstract`와 `open`을 의도적으로 구분해 새 source를 추가할 때 반드시 구현해야 하는 계약을 타입 시스템에 드러냈습니다.

### 합성 식별자를 값 객체로

여러 값이 함께 channel을 식별하는 경우 개별 parameter를 전달하지 않고 하나의 value object로 묶었습니다.

```kotlin
@JvmInline
value class ChannelUid(val value: String)

interface ChannelIdParser {
    fun parse(value: String): ChannelUid
}
```

### 설계 포인트

- 식별자 생성 규칙 캡슐화
- 호출부의 인자 순서 실수 방지
- 외부 경계에서 안정적인 identifier 사용

---

## 2. Facade + 역할 인터페이스 분리

관리 기능을 하나의 거대한 API로 노출하지 않고 역할별 좁은 interface로 분리했습니다.

```kotlin
class ChannelFacade {
    private val manager = ChannelManager()

    val favorites: FavoriteController = manager
    val pinned: PinnedController = manager
    val tuner: TunerController = manager
}
```

같은 instance를 여러 capability interface로 노출해 호출부가 필요한 권한만 갖도록 했습니다.

```kotlin
private val player = LivePlayer()

val playbackInfo: PlaybackInfo = player
val playbackControl: PlaybackControl = player
```

### 설계 포인트

- Interface Segregation 적용
- 조회 컴포넌트가 제어 API에 접근하지 못하도록 타입 수준에서 제한
- 의도치 않은 상태 변경 가능성 감소

---

## 3. 재생 상태를 불리언 조합이 아닌 타입으로 모델링

`isLive`, `isCatchup` 같은 flag 조합 대신 타입 계층으로 상태를 표현했습니다.

```kotlin
sealed interface PlaybackData {
    val channel: Channel

    data class Live(override val channel: Channel) : PlaybackData

    data class Catchup(
        override val channel: Channel,
        val schedule: Schedule,
    ) : PlaybackData
}
```

다시보기 전용 데이터는 `Catchup`에만 존재하므로 유효하지 않은 상태 조합을 구조적으로 줄일 수 있습니다.

상태 보관은 읽기 전용 stream을 외부에 노출하는 방식으로 구성했습니다.

```kotlin
class PlaybackStateHolder {
    private val _state = MutableStateFlow<PlaybackData?>(null)
    val state: StateFlow<PlaybackData?> = _state.asStateFlow()

    fun update(value: PlaybackData) {
        // skeleton
    }
}
```

---

## 4. 편성표(Grid EPG) — 2D 스크롤·줌 뷰포트

24시간 × 여러 채널의 편성표에서 세로/가로 스크롤, 시간축 확대/축소, 날짜 경계 이동을 처리했습니다.

핵심은 화면 좌표가 아니라 **분(minute) 단위의 논리 좌표계**를 기준으로 잡는 것이었습니다.

```kotlin
@Stable
class TimelineViewport {
    var minuteWidth: Float by mutableFloatStateOf(0f)
    var scrollOffsetX: Float by mutableFloatStateOf(0f)
    var scrollOffsetY: Float by mutableFloatStateOf(0f)

    fun minuteToPx(minute: Int): Float {
        // skeleton
        TODO("coordinate conversion")
    }

    fun pxToMinute(px: Float): Int {
        // skeleton
        TODO("coordinate conversion")
    }
}
```

### 설계 포인트

- zoom과 scroll을 하나의 논리 좌표계로 통합
- 날짜가 추가되어도 화면 좌표와 domain 좌표를 분리
- Compose primitive를 이용해 viewport 동작을 직접 제어

---

## 5. Compose 안정성 및 리컴포지션 제어

Compose에서 interface 타입을 state로 전달할 때 안정성 판단으로 인해 불필요한 recomposition이 발생할 수 있는 구간을 분석하고 계약을 명시했습니다.

```kotlin
@Stable
interface UiItem {
    val id: String
    val title: String
}

@Composable
fun ItemView(item: UiItem) {
    // skeleton
}
```

### 설계 포인트

- Compose compiler의 stability 개념을 고려한 API 설계
- immutable / stable contract를 명확하게 정의
- 성능 문제를 추측하지 않고 recomposition과 compiler metric을 기준으로 확인

---

## 6. 재생 진행 상태와 UI의 단방향 데이터 흐름

재생 진행률, 선택 상태, 표시 상태를 mutable state 하나로 공유하지 않고 각 책임에 맞는 state stream으로 분리했습니다.

```kotlin
class PlayerViewModel : ViewModel() {
    val progress: StateFlow<Progress> = TODO("skeleton")
    val uiState: StateFlow<PlayerUiState> = TODO("skeleton")

    fun onAction(action: PlayerAction) {
        // skeleton
    }
}
```

UI는 action을 전달하고 ViewModel이 state를 변경하는 단방향 흐름을 유지합니다.

---

## 7. Repository + Policy Delegate

데이터 접근과 비즈니스 정책을 하나의 구현에 결합하지 않고 역할별 계약으로 분리했습니다.

```kotlin
interface Repository<T> {
    suspend fun load(): T
    suspend fun save(value: T)
}

interface Policy<T> {
    fun validate(value: T): Boolean
}

class RepositoryImpl<T>(
    private val policy: Policy<T>,
) : Repository<T> {
    override suspend fun load(): T = TODO("skeleton")

    override suspend fun save(value: T) {
        // skeleton
    }
}
```

### 설계 포인트

- 저장 방식과 정책의 결합도 감소
- policy 독립 테스트 가능
- 구현 기술에 종속되지 않는 domain contract

---

## 8. Legacy Callback → Coroutine Bridge

기존 callback 기반 API를 coroutine 환경에서 일관되게 사용할 수 있도록 adapter boundary를 만들었습니다.

```kotlin
interface LegacyApi {
    fun request(callback: (Result<Data>) -> Unit)
}

suspend fun LegacyApi.await(): Data {
    // skeleton: callback lifecycle bridge
    TODO("skeleton")
}
```

### 설계 포인트

- legacy와 modern code의 경계 명확화
- callback lifecycle 관리 집중
- 호출부의 비동기 처리 복잡도 감소

---

## 9. Adaptive UI Components

화면 크기와 상태에 따라 표현이 달라지는 공통 component를 구성해 화면별 중복을 줄였습니다.

```kotlin
@Composable
fun AdaptiveContent(
    state: UiState,
    modifier: Modifier = Modifier,
) {
    // skeleton
}
```

---

## 10. Capability Interfaces

하나의 거대한 interface 대신 기능을 capability 단위로 분리했습니다.

```kotlin
interface Playable {
    fun play()
}

interface Seekable {
    fun seek(position: Long)
}

interface Configurable {
    fun configure()
}
```

### 설계 포인트

- 필요한 기능만 의존
- mock / fake 구현 단순화
- 기능 확장 시 기존 계약 영향 최소화

---

## 11. URI-based Identifier

외부 시스템과의 경계에서는 객체 자체보다 안정적인 identifier를 전달하도록 설계했습니다.

```kotlin
@JvmInline
value class ResourceUri(val value: String)

interface ResourceResolver {
    suspend fun resolve(uri: ResourceUri): Resource
}
```

이를 통해 객체 생명주기와 식별 책임을 분리하고 외부 연동 boundary를 명확하게 유지했습니다.

---

## 12. Dual Playback Engine Abstraction

서로 다른 playback 구현을 상위 계층에서 동일한 계약으로 사용할 수 있도록 abstraction boundary를 구성했습니다.

```kotlin
interface PlaybackEngine {
    fun prepare(source: Source)
    fun play()
    fun pause()
    fun release()
}

class EngineA : PlaybackEngine {
    override fun prepare(source: Source) { /* skeleton */ }
    override fun play() { /* skeleton */ }
    override fun pause() { /* skeleton */ }
    override fun release() { /* skeleton */ }
}

class EngineB : PlaybackEngine {
    override fun prepare(source: Source) { /* skeleton */ }
    override fun play() { /* skeleton */ }
    override fun pause() { /* skeleton */ }
    override fun release() { /* skeleton */ }
}
```

### 설계 포인트

- 상위 계층의 engine 의존성 제거
- 구현체 교체 가능
- engine별 차이를 boundary 내부로 격리

---

## 13. Hilt DI + ViewModel

객체 생성 책임을 화면에서 분리하고 lifecycle에 맞는 dependency graph를 구성했습니다.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    // skeleton: bindings
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val useCase: MainUseCase,
) : ViewModel() {
    // skeleton
}
```

---

## 14. 문제 해결 방식

복잡한 기능을 작은 책임으로 분해하고 각 경계에서 계약을 정의했습니다.

```text
Input
 ↓
State / Model
 ↓
Policy / Use Case
 ↓
Repository / Adapter
 ↓
External System
```

### 중요하게 보는 기준

- 변경 이유가 하나의 책임에 모여 있는가?
- 구현체를 교체할 수 있는가?
- 테스트에서 외부 의존성을 제거할 수 있는가?
- UI가 domain/infrastructure 세부사항을 직접 알지 않아도 되는가?
- 공개 코드에서 핵심 설계는 전달하면서 내부 구현은 보호할 수 있는가?

---

## Interview Summary

이 포트폴리오에서 강조하는 부분은 단순히 Kotlin/Compose를 사용할 수 있다는 점보다, **복잡한 기능을 책임 단위로 분해하고 적절한 abstraction boundary를 설계하는 능력**입니다.

- 재사용 가능한 UI / domain abstraction 설계
- legacy API를 modern coroutine 구조로 연결
- policy와 실행 책임 분리
- 서로 다른 구현체를 하나의 계약으로 추상화
- 변경 가능성과 테스트 가능성을 고려한 architecture 설계

> 실제 서비스 구현은 공개하지 않고, 설계 역량을 확인할 수 있는 수준의 skeleton만 제공합니다.
