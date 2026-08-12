# 13 背压与多层限流

> 生产就绪缺口 07（M1 稳定基线）。设计 Spec 立项：core 扩展（spawn 闸 / 脊柱扇出闸）+ resilience 扩展（模型双桶 Advisor），不新增模块 / SPI。
> 蓝本：LangChain InMemoryRateLimiter（令牌桶挂模型层；超越其仅 RPM 边界到 RPM+TPM 双桶）；LangGraph Platform multitask_strategy（reject/enqueue 两档裁决枚举）；CrewAI max_rpm（数值化速率护栏）。
> 忠实度：三维挂点 / 过载两档契约 / TPM 事后记账由缺口决策定案；具体实现（令牌桶模型 / drain 唤醒 / steal 绕过）由本项目自主推演，标注 `> 【推演】`。

## 设计目标

让单个 Agent 实例在**量级失控**面前有闸门、体面地拒（明确错误、可重试、有事件留痕），而不是默默地垮。

- **三维挂点**：① spawn 并发会话上限 ② 每会话工具扇出上限 ③ 每模型 RPM+TPM 双桶。各维度独立可配。
- **过载语义两档统一**：`QUEUE`（有界排队 + 超时，默认）/ `FAIL_FAST`（快速失败）。拒绝 = 调用方可重试的明确异常 + 事件进 observability 既有通道。
- **框架自限流拒绝不经重试管线**：与 provider 429 的可重试语义严格区分，避免重试放大拥塞。
- **safe-by-default**：各维度阈值默认 null = 不限，显式配置才生效——不设可能误伤生产的魔法默认值。
- **不新增模块 / SPI / 外部依赖**：core 扩展 spawn/脊柱 + resilience 扩展双桶；17 模块星形依赖不变。
- **诚实边界**：TPM 是平均速率保护（事后记账+下次预检），不防单次尖峰越限；单进程内存语义，分布式精确限流不做。

## 术语

| 术语 | 含义 |
|---|---|
| spawn 闸 | 实例级并发活跃会话上限裁决（`SpawnGate`），复用 06 的 `liveSessions` 台账计数 |
| 扇出闸 | 每轮工具并发上限 + 许可获取超时（`HarnessToolCallingManager` 配置接线） |
| 模型双桶 | 每模型 RPM+TPM 令牌桶（`ModelRateLimiter` + `RateLimitAdvisor`），按 modelName 分桶 |
| 过载策略 | `QUEUE`（有界排队 + 超时）/ `FAIL_FAST`（快速失败），三维共用 `OverloadPolicy` 枚举 |
| 容量异常 | `SessionCapacityExceededException`：携带 sessionId + 当前活跃数 + 上限 + 已等待时长 |
| 限流异常 | `ModelRateLimitExceededException`：携带 modelName + 桶维度（RPM/TPM）+ 已等待时长 |

## API

### 维度① spawn 闸（core）

```java
public final class SpawnGate {
    public void acquireSlotOrThrow(String sessionId);  // 排队/拒绝（QUEUE 等空位+drain 唤醒；FAIL_FAST 立即拒）
    public void releaseSlot();                          // 会话 close 时释放空位 + 唤醒等待者
    public void signalDrainStarted();                   // drain 置位时唤醒全部等待者拒绝
}
```

- **裁决点**：`DefaultAgentRuntime.spawn()` 入口、租约获取**之前**。排队不持有租约，拿到空位后才走既有 `doSpawn` 全流程。
- **计数源**：复用 06 的 `liveSessions` 台账（`ConcurrentHashMap`，spawn 注册 / close 注销），不引入第二份会话计数。
- **排队实现**：`Semaphore` + `ReentrantLock` + `Condition`——空位由会话 close 释放时通知；drain 置位时唤醒拒绝。**禁止**轮询 sleep。
- **steal 绕过**：`spawn(steal=true)` 是接管路径（已活跃会话易主），不占新容量——在进入闸门之前判定 steal=true 直接绕过。
- **drain 交互**：drain 拒新判定**先于**容量裁决；排队中的 spawn 等待者在 drain 置位时被唤醒拒绝（不睡死在信号量上）。

### 维度② 扇出闸（core）

```java
public class HarnessToolCallingManager {
    public static final int DEFAULT_MAX_CONCURRENT_PER_TURN = 8;
    public static final Duration DEFAULT_TOOL_TIMEOUT = Duration.ofSeconds(60);
    // permitAcquireTimeout: null = 无限等待（现状）；ZERO = FAIL_FAST；正值 = 有界 tryAcquire
}
```

- **配置接线**：`HarnessAssembler.withToolFanout(maxConcurrent, toolTimeout, permitAcquireTimeout)`——消除硬编码 8 / 60s，现值抽命名常量。
- **许可超时**：`turnPermits.acquire()` → `tryAcquire(permitAcquireTimeout)`；超时后该工具调用返回错误结果（「工具过载未执行」），不阻断同轮其他工具、不吊死轮次。
- **串行组语义不变**：既有 serialGroups 互斥逻辑不动；`cancelInFlight` 取消传播不动。

### 维度③ 模型 RPM+TPM 双桶（buzhou-resilience）

```java
public final class ModelRateLimiter {
    public void acquireOrThrow(String modelName);              // RPM 预检+扣减 + TPM 预检
    public void recordUsage(String modelName, Long totalTokens); // TPM 事后记账
}
public class RateLimitAdvisor implements BaseAdvisor {
    // order = ToolCallingAdvisor.DEFAULT_ORDER + 650（外于 ResilienceAdvisor +700）
}
```

- **挂点**：`RateLimitAdvisor` 在模型调用前切面执行（order +650，外于 `ResilienceAdvisor` +700）。自限流拒绝在 `callChain.nextCall` 之前抛出 → `ResilienceAdvisor` 不可见 → 不进入重试分类。
- **桶模型**：令牌桶，按 modelName 分桶、单进程 `ConcurrentHashMap`。RPM `capacity = rpm, refillRate = rpm/60`；TPM 同理。
- **RPM**：调用前预检 + 扣减（每次扣 1 token）。
- **TPM**：调用后按实际 usage 记账（`chatResponse.getMetadata().getUsage()`）+ 下次调用前预检。流式在流末尾聚合 usage（`doOnComplete`）。
- **usage 缺失**：provider 不返回 usage 时 TPM 记 0 + 留痕（`backpressure.model-usage-missing` 事件），不伪造估值。
- **onModelError 兜底**：自限流拒绝抛 `ModelRateLimitExceededException`，经 `HookAdvisor` 到达 `onModelError` 切面——用户可 Replace/Block 兜底，与 provider 侧失败走同一切面但类型可区分。

> 【推演】「重试的每次尝试同样过桶」按 ResilienceAdvisor 内部 `modelTerminal` 直接调用的实现现实，解读为「每次逻辑模型调用只过一次桶」——RPM = 逻辑请求数（非物理调用数），语义正确且避免重试放大自限流。

### 过载策略枚举

```java
public enum OverloadPolicy {
    QUEUE,      // 有界排队 + 超时（默认）
    FAIL_FAST   // 快速失败（不排队，立即拒绝）
}
```

### 异常类型

| 异常 | 携带上下文 | 触发场景 |
|---|---|---|
| `SessionCapacityExceededException` | sessionId / currentCount / limit / waitedMillis | spawn 闸超限（FAIL_FAST 立即 / QUEUE 超时） |
| `ModelRateLimitExceededException` | modelName / dimension(RPM\|TPM) / waitedMillis | 模型双桶超限（FAIL_FAST 立即 / QUEUE 超时） |

## 配置项

### core 侧（`buzhou.backpressure` 前缀）

| 属性 | 默认 | 语义 |
|---|---|---|
| `buzhou.backpressure.enabled` | `true`（safe-by-default） | 机制总开关；关则不限并发、不限扇出 |
| `buzhou.backpressure.max-concurrent-sessions` | `null`（不限） | 实例级并发活跃会话上限 |
| `buzhou.backpressure.spawn-queue-timeout` | `30s` | spawn 排队等待超时（QUEUE 档生效） |
| `buzhou.backpressure.spawn-overload-policy` | `QUEUE` | spawn 过载策略 |
| `buzhou.backpressure.tool.max-concurrent-per-turn` | `8`（现值常量） | 每轮工具并发上限 |
| `buzhou.backpressure.tool.tool-timeout` | `60s`（现值常量） | 单工具执行超时 |
| `buzhou.backpressure.tool.permit-acquire-timeout` | `null`（无限等待） | 扇出许可获取超时；配置后改为有界 tryAcquire |
| `buzhou.backpressure.tool.overload-policy` | `QUEUE` | 工具过载策略（FAIL_FAST 等价 permitAcquireTimeout=0） |

### resilience 侧（`buzhou.resilience.rate-limit` 前缀）

| 属性 | 默认 | 语义 |
|---|---|---|
| `buzhou.resilience.rate-limit.requests-per-minute` | `null`（不限） | 每分钟请求数上限（RPM 令牌桶容量） |
| `buzhou.resilience.rate-limit.tokens-per-minute` | `null`（不限） | 每分钟 token 数上限（TPM 令牌桶容量） |
| `buzhou.resilience.rate-limit.queue-timeout` | `30s` | QUEUE 档排队超时 |
| `buzhou.resilience.rate-limit.overload-policy` | `QUEUE` | 模型限流过载策略 |

## 事件清单

| 事件类型 | payload | 时机 |
|---|---|---|
| `backpressure.spawn-queued` | sessionId / currentActive / limit | spawn 进入排队（QUEUE 档） |
| `backpressure.spawn-rejected` | sessionId / reason(timeout\|fail-fast\|drain) / currentActive / limit / waitedMs | spawn 被拒绝 |
| `backpressure.tool-permit-timeout` | toolName / waitedMs | 扇出许可获取超时 |
| `backpressure.model-throttled` | modelName / dimension(RPM\|TPM) | 模型调用进入排队（QUEUE 档） |
| `backpressure.model-rejected` | modelName / dimension / waitedMs | 模型调用被拒绝 |
| `backpressure.model-usage-missing` | modelName | provider 不返回 usage（TPM 记 0 留痕） |

> 【推演】spawn 闸事件发生在会话建立前，经运行时级事件通道（`AgentRuntime.addRuntimeEventListener`）——与既有 per-session 事件通道（`SessionEventListener`）区分，因为 `SpawnOptions.listeners` 在 `doSpawn` 返回后才挂载。

## 与动态预算的区分

- **动态预算**（spec 01 记忆压缩）：管单会话上下文窗口怎么分（历史 vs 系统 vs 工具 Schema vs 输出预留）。正交不重叠。
- **背压与限流**（本机制）：管跨会话速率与并发（spawn 闸 / 扇出闸 / 模型双桶）。

## 每实例配额折算

本机制单进程内存语义——多实例部署按「每实例配额 = 总配额 / 实例数」配置折算。例如 provider 给 1000 RPM、部署 4 实例 → 每实例配 `buzhou.resilience.rate-limit.requests-per-minute=250`。不引 Redis 强依赖，不做分布式精确限流。

## 推演标注

| # | 位置 | 推演点 | 依据 |
|---|---|---|---|
| 1 | RateLimitAdvisor order | +650（外于 ResilienceAdvisor +700）：自限流拒绝在 nextCall 之前抛出 → 不进入重试分类 | spec「限流是韧性前哨」+ ResilienceAdvisor modelTerminal 直接调用现实 |
| 2 | TPM 预检不扣减 | 检查 available() > 0 即放行；实际扣减在 recordTpm（按真实 usage） | spec「事后记账+下次预检=平均速率保护」 |
| 3 | drain 唤醒排队 spawn | drain 置位后 signalDrainStarted() 唤醒 Condition 等待者 | spec「排队中的 spawn 等待者须被唤醒并拒绝」 |
| 4 | steal 绕过容量闸 | spawn(steal=true) 在进入 SpawnGate 之前判定绕过 | spec「steal 不占新容量」 |
| 5 | 运行时级事件通道 | AgentRuntime.addRuntimeEventListener（additive default）收会话建立前事件 | drain 事件经 per-session 先例不适用（会话尚未建立） |
| 6 | 许可超时 null = 无限等待 | permitAcquireTimeout=null 保持 acquire()（现状）；配置后改为 tryAcquire | spec「不配置时行为与现状一致」 |

## 开放问题

- **绑定级 / 工具级 policy 覆盖**：同 03/05/06 M1 口径，待 policy 消费管线打通后接入（当前只到默认 + yml 两层）。
- **TPM 调用前预估预占**：当前走事后记账+下次预检；预估预占（按 maxTokens）为潜在增强，不在本期。
- **AIMD 自适应降速**：07 票留 Spec 期评估项，本期不做；provider 429 维持既有重试语义。
- **跨实例容量协调**：集群级会话上限归编排层职责；本机制只交付单实例原语。
- **与 08 死循环检测收敛**：M2 收敛「失控防护家族」统一形态（双窗口计数 + 软信号注入 + 硬顶阻断）。
