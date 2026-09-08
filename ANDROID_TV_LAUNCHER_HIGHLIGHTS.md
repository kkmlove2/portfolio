# Android TV Launcher — 코드 아키텍처 하이라이트

> Android TV 런처에서 설계 의도가 드러나는 부분을 공개용으로 재작성한 문서입니다.
>
> 실제 제품 코드, 내부 식별자, 저장 구조 및 구현 세부사항은 공개하지 않고 핵심 설계와 책임 분리만 확인할 수 있도록 skeleton 형태로 정리했습니다.

## 목차

1. Drag & Reorder Abstraction
2. Heterogeneous List Item Modeling
3. 동시성 제어
4. RecyclerView Adapter 계층
5. HTTP 계층의 교체 가능한 전략 구조
6. UI 프레임워크화
7. 하드웨어 의존성 인터페이스 분리
8. Hilt DI + ViewModel 생명주기
9. 기능 단위 수직 분할
10. 콜백 기반 API 추상화
11. 원격 진단 설계
12. 기기 능력 기반 설정 UI
13. 이력서용 요약

---

## 1. Drag & Reorder Abstraction

홈 화면에서 아이템을 드래그하여 순서를 변경하는 기능을 재사용 가능한 abstraction으로 분리했습니다.

```kotlin
interface ReorderController<T> {
    fun startDrag(item: T)
    fun move(from: Int, to: Int)
    fun finishDrag()
}

class ReorderControllerImpl<T> : ReorderController<T> {
    override fun startDrag(item: T) { /* skeleton */ }
    override fun move(from: Int, to: Int) { /* skeleton */ }
    override fun finishDrag() { /* skeleton */ }
}
```

### 설계 포인트

- UI gesture와 상태 변경 책임 분리
- Generic abstraction으로 다양한 아이템 타입 지원
- 순서 변경 정책을 독립적으로 테스트 가능

---

## 2. Heterogeneous List Item Modeling

성격이 다른 Row와 App item을 하나의 리스트에서 안정적으로 표현하기 위해 타입 계층을 사용했습니다.

```kotlin
sealed interface LauncherItem {
    data class App(/* fields */) : LauncherItem
    data class Shortcut(/* fields */) : LauncherItem
    data class Folder(/* fields */) : LauncherItem
}

fun displayName(item: LauncherItem): String {
    // skeleton
    TODO("resolve display name")
}
```

UI에서는 구체적인 구현 저장 방식이 아니라 공통 domain contract만 사용합니다.

### 설계 포인트

- 타입별 UI 분기 명확화
- 새로운 item type 추가 시 영향 범위 최소화
- runtime 전용 상태와 영속 데이터의 경계 분리

---

## 3. 동시성 제어 — Shared Mutex

서로 다른 manager가 동일한 상태를 변경할 수 있는 구간에서는 하나의 동기화 경계를 공유하도록 구성했습니다.

```kotlin
class StateCoordinator(
    private val mutex: Mutex,
) {
    suspend fun <T> locked(block: suspend () -> T): T =
        mutex.withLock { block() }
}

class Manager(private val coordinator: StateCoordinator) {
    suspend fun update() {
        // skeleton
    }
}
```

### 설계 포인트

- manager별로 제각각 lock을 두는 대신 동일한 critical section을 공유
- coroutine 기반 비동기 처리에서도 상태 변경 순서를 명확하게 관리
- 실제 상태 저장/조회 구현은 공개하지 않고 동기화 구조만 노출

---

## 4. RecyclerView Adapter 계층 — Template Method

여러 화면에서 반복되는 focus, dim, item binding 등의 공통 동작을 base adapter로 추상화했습니다.

```kotlin
abstract class BaseGridAdapter<T, VH : RecyclerView.ViewHolder> :
    RecyclerView.Adapter<VH>() {

    final override fun onBindViewHolder(holder: VH, position: Int) {
        // common lifecycle / state handling
        bindItem(holder, getItem(position))
    }

    protected abstract fun getItem(position: Int): T
    protected abstract fun bindItem(holder: VH, item: T)
}
```

### 설계 포인트

- 공통 lifecycle 처리와 화면별 rendering 책임 분리
- Template Method 패턴으로 중복 코드 감소
- listener 계약을 interface로 제한해 호출부의 결합도 감소

---

## 5. HTTP 계층 — 구현체 교체 가능한 전략 구조

통신 구현 자체와 호출 정책을 분리해 네트워크 구현체를 교체할 수 있도록 설계했습니다.

```kotlin
interface HttpClient {
    suspend fun request(request: HttpRequest): HttpResponse
}

interface RequestPolicy {
    fun validate(response: HttpResponse): Boolean
}

class ApiGateway(
    private val client: HttpClient,
    private val policy: RequestPolicy,
) {
    suspend fun execute(request: HttpRequest): Result<HttpResponse> {
        // skeleton
        TODO("request pipeline")
    }
}
```

### 설계 포인트

- transport와 domain policy 분리
- 테스트에서 fake client 주입 가능
- 실제 서버 주소와 사내 구현은 공개하지 않음

---

## 6. UI 프레임워크화 — Abstract Fragment + Builder

반복되는 설정 화면 구조를 공통 Fragment와 builder 형태로 추상화했습니다.

```kotlin
abstract class BaseSettingsFragment : Fragment() {
    protected abstract fun buildItems(): List<SettingsItem>

    protected fun configure(builder: SettingsBuilder) {
        // skeleton
    }
}

class SettingsBuilder {
    fun add(item: SettingsItem): SettingsBuilder = this
    fun build(): List<SettingsItem> = TODO("skeleton")
}
```

### 설계 포인트

- 화면마다 반복되는 boilerplate 감소
- 설정 항목 생성과 rendering 책임 분리
- 기능별 확장이 base framework를 침범하지 않도록 구성

---

## 7. 하드웨어 의존성 인터페이스 분리

셋톱박스 하드웨어와 직접 연결되는 기능은 application/domain 계층에서 직접 참조하지 않고 capability interface로 감쌌습니다.

```kotlin
interface DeviceCapability {
    fun isSupported(feature: Feature): Boolean
}

interface TunerController {
    suspend fun scan(): ScanResult
    fun cancel()
}

class HardwareTunerController : TunerController {
    override suspend fun scan(): ScanResult = TODO("skeleton")
    override fun cancel() { /* skeleton */ }
}
```

### 설계 포인트

- hardware dependency를 boundary 내부로 격리
- 실제 기기 없이 fake 구현으로 테스트 가능
- 지원하지 않는 기능은 capability로 판단

---

## 8. Hilt DI + ViewModel 생명주기

화면이 직접 객체를 생성하지 않고 DI graph를 통해 필요한 dependency를 주입받도록 구성했습니다.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    // skeleton: bindings
}

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val useCase: LauncherUseCase,
) : ViewModel() {
    // skeleton
}
```

### 설계 포인트

- 객체 생성 책임과 사용 책임 분리
- ViewModel lifecycle에 맞는 dependency 관리
- 테스트에서 dependency 교체 가능

---

## 9. 기능 단위 수직 분할 — MVVM

화면 하나를 거대한 모듈로 만들기보다 feature 단위로 UI, state, use case를 함께 묶었습니다.

```text
feature/
 ├── ui/
 ├── state/
 ├── domain/
 └── data-boundary/
```

```kotlin
class FeatureViewModel(
    private val useCase: FeatureUseCase,
) : ViewModel() {
    val state: StateFlow<UiState> = TODO("skeleton")

    fun onAction(action: UiAction) {
        // skeleton
    }
}
```

---

## 10. 콜백 기반 API 추상화

legacy callback API를 상위 계층에서 coroutine 기반으로 사용할 수 있도록 경계를 만들었습니다.

```kotlin
interface LegacySource {
    fun request(callback: (Result<Data>) -> Unit)
}

suspend fun LegacySource.awaitData(): Data {
    // skeleton: callback lifecycle bridge
    TODO("skeleton")
}
```

### 설계 포인트

- legacy와 modern code의 경계 명확화
- callback lifecycle 관리 집중
- 호출부의 비동기 처리 복잡도 감소

---

## 11. 원격 진단 설계

원격 진단에 필요한 로그 API와 실제 출력 구현을 분리했습니다.

```kotlin
interface DiagnosticLogger {
    fun info(event: DiagnosticEvent)
    fun error(event: DiagnosticEvent, throwable: Throwable? = null)
}

class DiagnosticFacade(
    private val logger: DiagnosticLogger,
) {
    fun record(event: DiagnosticEvent) {
        // skeleton
    }
}
```

민감한 내부 필드와 실제 전송 경로는 공개하지 않고 진단 계층의 책임만 표현합니다.

---

## 12. 기기 능력 기반 설정 UI

제품별 hardware/software capability 차이를 화면 코드의 수많은 if 문으로 흩뿌리지 않고 데이터로 표현했습니다.

```kotlin
data class SettingsItem(
    val title: String,
    val visible: Boolean,
    val enabled: Boolean,
)

interface CapabilityProvider {
    fun supports(feature: Feature): Boolean
}

fun buildSettings(provider: CapabilityProvider): List<SettingsItem> {
    // skeleton
    TODO("compose settings model")
}
```

### 설계 포인트

- device variant 대응 범위 축소
- UI rendering과 capability 판단 분리
- 새로운 기능 추가 시 기존 화면 로직 영향 최소화

---

## 13. 이력서용 요약

- Android TV 런처의 복잡한 UI/상태 변경을 재사용 가능한 abstraction으로 분리
- Generic / sealed type / capability interface를 활용해 타입 수준에서 책임과 기능 범위를 명확화
- coroutine 환경의 동시성 문제를 shared synchronization boundary로 제어
- legacy callback API와 hardware dependency를 명확한 adapter boundary로 격리
- Hilt DI와 ViewModel을 활용해 lifecycle과 객체 생성 책임 분리
- 공통 UI 동작을 Template Method / Builder 구조로 프레임워크화

> 핵심은 특정 구현 기술보다 **책임 분리, 추상화 경계, 교체 가능성, 테스트 가능성**입니다.
