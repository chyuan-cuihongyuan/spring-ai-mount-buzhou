# 10 模型韧性层（Model Resilience）

> 管辖 ADR：wayfinder「production-readiness」03 号票（决策=做、M1/M2 分期）、02 号票（底座 429 默认不重试 / 无统一超时 / 无熔断——不周山合法空间）。本档落地 **M1** 子集。源 spec：`.scratch/model-resilience/spec.md`。

## 设计目标

让模型调用这一跳稳定地跑在生产里：**重试**（瞬时错误 / 限流）、**统一超时**（deadline + 中断在途调用）、**归一化错误分类**（跨 provider 五类口径）、**onModelError 兜底切面**（模型失败时受控回复）。引入 `buzhou-resilience` 即得生产级韧性、无需手写重试循环；safe-by-default、可一键关。

- 执行点：`ResilienceAdvisor` 是 ChatClient advisor 链的**最内层模型包裹 Advisor**，作用于单次模型调用（落在 `ToolCallingAdvisor` 工具循环内部、每次模型调用粒度），而非整轮。
- 不引入 Resilience4j / Spring Retry（与 03 决策一致）：手写指数退避 + 抖动小回路；deadline 复用执行脊柱 `HarnessToolCallingManager` 既有的 `Future.get` + `cancel(true)` 手法。
- 事件进会话既有事件通道（与 Hook 事件同炉），**不新增存储 SPI**。

## 术语

领域术语以根目录 `CONTEXT.md` 为准：**模型韧性层 / 归一化错误分类（限流·鉴权·内容·网络·未知）/ 内容拒绝（静默通道）**。

## API

### 执行点：ResilienceAdvisor

`ResilienceAdvisor implements BaseAdvisor`，经 `SessionAssemblyCustomizer` + `SessionAssemblyContext.addAdvisor(...)` 注入（对齐 `buzhou-observability` 既有 Advisor 注入模式）。模块入口 `ResilienceModule.configure(ResilienceProperties)` 返回只含一个装配定制器的 `RuntimeConfig`。

**Order 生效位（实现期核验结果，固定如下）**：

| Advisor | order（`ToolCallingAdvisor.DEFAULT_ORDER + N`） |
|---|---|
| ToolCallingAdvisor | base（`DEFAULT_ORDER = -2147483348`） |
| BuzhouMemoryAdvisor | +400 |
| ObservabilityAdvisor | +500 |
| HookAdvisor | +600 |
| **ResilienceAdvisor** | **+700**（最内层、紧邻 HookAdvisor 之内） |
| ChatModelCallAdvisor（Spring AI 内置模型终端） | `Integer.MAX_VALUE` |

- 链按 order **升序**排，Spring AI 从最低 order 起消费（最低=最外层）。`ResilienceAdvisor`（+700）在 `HookAdvisor`（+600）之内：`beforeModel`/`afterModel` 观察到「经韧性层解决后」的一次逻辑模型调用（重试对 Hook 不可见）；终态失败时异常按原语义抛回 `HookAdvisor`，由其触发 `onModelError`。
- **重试与 advisor 链一次性消费的陷阱**：Spring AI 的 advisor 链 `Deque` 是一次性消费的，`callChain.nextCall` 每次弹一个 advisor；`ToolCallingAdvisor` 用 `chain.copy(this)` 回避（每轮工具循环重建链）。但 `copy(this)` 会**重放外层 advisor**（Memory/Hook），违反「重试对 Hook 不可见」。故 `ResilienceAdvisor` 的每次尝试**直接调用链最内层的模型终端**（`ChatModelCallAdvisor`，自包含 `chatModel.call`、不回调链），绕开被消费的链、不重放外层 advisor。

### 归一化错误分类 + ProviderErrorClassifier SPI

`ProviderErrorClassifier` 把「异常 + 响应元数据 → `Classification(category, retryAfter)`」做成可扩展点。框架内置 `DefaultErrorClassifier`（provider 无关，识别 Spring AI RestClient 系的 `RestClientResponseException` + 内容过滤元数据）：

| 类别 | 触发 | M1 默认是否重试 |
|---|---|---|
| RATE_LIMIT（限流） | HTTP 429，解析 Retry-After（秒 / HTTP-date） | 是 |
| NETWORK（网络） | 连接重置 / 读超时 / 瞬断（异常类名启发式）；**HTTP 5xx 归此**（服务端瞬时故障，五类别无独立「服务端」桶） | 是 |
| AUTH（鉴权） | HTTP 401 / 403 | 否（快速失败） |
| CONTENT（内容） | 响应 `finishReason` 含 `content_filter`（静默拒绝，不抛异常） | 否 |
| UNKNOWN（未知） | 其余（含非 401/403/429 的 4xx） | 否（保守，可经 `retryable-categories` 配为重试） |

- 内容拒绝治理策略归内容安全机制；本层只分类 + 保证 `afterModel`/`onModelError` 可观测。
- 非标 provider 经 SPI 覆盖（实现 `ProviderErrorClassifier`，经自定义 `ResilienceModule.configure` 注入）。

### onModelError 切面

`BuzhouHook` 新增 `default HookResult onModelError(ModelCallContext ctx)`（默认 `CONTINUE`，源码 / 二进制兼容）。`ModelCallContext.error()` 暴露终态失败原因。`HookAdvisor` 在 `adviseCall`/`adviseStream` 用 try/catch（流式用 `onErrorResume`）包住模型调用，终态失败后调用 `chain.onModelError(ctx)`：

- `Replace(ChatClientResponse)` → 吞错回填兜底响应（用户得受控回复，无裸异常）；
- `Block(reason)` → 回填文本兜底；
- `CONTINUE`（默认 / 全放行）→ 异常按底座原语义抛出。

触发源覆盖三类终态：重试耗尽、命中不可重试类别、超时。

### 统一超时（deadline）+ session.cancel() 漏网修复

- **模型级 deadline**：把单次模型调用终端包进虚拟线程任务、`Future.get(deadline)` 兜底；超时 `cancel(true)` 把中断传播进在途模型调用（对齐 `HarnessToolCallingManager`）。超时是**终态失败**（不重试）。
- **补 `session.cancel()` 的缺口**：当前 `cancel()` 只中断工具、不中断模型调用（02 号票与代码盘点确认）。本层注册 `SessionObserver`，其 `onCancel()` 经 `ModelCallInFlight` 取消在途模型 Future——deadline 与 cancel 共用同一条 `Future.cancel(true)` 中断路径。
- 流式 deadline：以 `Flux.timeout(deadline)` 作「首 token / 帧间空闲」超时（active 流式每帧重置计时）；M1 不对流式已发 token 做中途重试（边界不可行）。

## 配置项

`buzhou.resilience.*`（`@ConfigurationProperties` record，boxed 类型、null=未配置→取规范默认，对齐 `SpillProperties` 模板）：

| 属性 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 整个机制开关；关则不装配 advisor（回退底座原生行为） |
| `max-attempts` | `3` | 最大尝试次数（含首次） |
| `initial-backoff` | `500ms` | 首次重试退避基数 |
| `max-backoff` | `10s` | 退避上限（Retry-After 与指数增长都钳制到本值） |
| `multiplier` | `2.0` | 指数退避乘子（1.0 = 恒定） |
| `jitter` | `0.5` | 抖动因子 `[0,1]`（打散惊群；0=关闭） |
| `retryable-categories` | `[RATE_LIMIT, NETWORK]` | 可重试类别集合（覆盖默认表） |
| `deadline` | `60s` | 模型调用统一超时（0=关闭，不推荐生产关闭） |

> **绑定级 / per-tool 覆盖移出 M1**：绑定级层（`mechanismOverrides`）的存储已就位、但「被下游机制消费」的管线尚未接通（`LayeredPolicy` 脚手架未连装配）。M1 只到默认 + yml 两层。

## 事件清单

进会话既有事件通道（`SessionEventListener`，与 Hook 事件同炉）：

| 事件 type | payload | 时机 |
|---|---|---|
| `retry-attempted` | `category` / `attempt` / `backoffMs` / `retryAfter` | 每次重试前 |
| `retry-exhausted` | `category` / `attempts` | 重试用尽、放弃 |
| `error-classified` | `category` | 每次分类（SRE 统一口径） |
| `content-refusal-detected` | — | 内容拒绝静默识别 |
| `timeout-fired` | `deadlineMs` | deadline 超时 |

## 与底座叠加避让

- 语义分工：底座 Spring AI 只管网络瞬断；429 / 5xx / 超时 / （M2）熔断 / 降级 归不周山。
- 数值避让：本 Harness 经 `ChatClient.builder(chatModel)` 装配、未启用底座 `spring.ai.retry`（裸 ChatClient 无内置重试），故**不会双倍重试**（经「模型恰好被调用 maxAttempts 次」断言保证）。若业务侧另行显式启用 `spring.ai.retry`，应将其 `max-attempts` 调到 1，避免与不周山重试叠加。

## M2 预留扩展位

- `ResilienceAdvisor` 是 **M2（熔断 / 自动兜底降级 / 多模型路由 / 降级链 / failover）** 的同炉宿主——M2 不必重写执行点，在本 advisor 内叠加状态机 / 路由。
- `onModelError` 是 **08（重试耗尽 → 转人工出口）** 的天然挂载点。
- 与 **07（每模型 RPM/TPM 双桶 Advisor）** 协同：07 限流 Advisor 与本层共享同一 Advisor / Hook 平台。

## 推演标注

> 【推演】五类别无独立「服务端错误」桶：5xx 归 `NETWORK`（可重试的瞬时基础设施类）。CONTEXT.md 五类是固定契约，新增类别会破坏口径；SRE 如需区分「provider 宕机」与「网络瞬断」，可在事件 `payload` 附加 HTTP 状态细分，不新增类别。

> 【推演】流式 deadline 用 `Flux.timeout` 作空闲超时（每帧重置），而非「总时长硬切」——避免切断 active 流式、丢失已发 token。总时长预算属成本治理（11）家族，不在本层。

> 【推演】`ModelCallInFlight` + `ResilienceSessionObserver` 在 `buzhou-resilience` 内修复 `session.cancel()` 的模型调用漏网，不改 core 的 `DefaultAgentSession.cancel()`——机制按需装配、关闭时自然回退（无本模块则 cancel 仍只中断工具，与底座现状一致）。

## 开放问题

- 绑定级 / per-tool 韧性参数覆盖（policy 消费管线打通后纳入）。
- 流式场景的内容拒绝精细识别（M1 只覆盖同步路径）。
- 跨 advisor 的可观测性上下文（micrometer observation）随 deadline 线程切换的传播——M1 未做（buzhou Span 体系用 `SpanContextCarrier` 不受影响；micrometer 父子 span 细化留后续）。
