# Spec 47 — 日志链路关联与反馈捕获（effort #10）

> effort #10 第二篇。§A：MDC 会话轮次关联（T172）；§B：turn 反馈捕获 API（T173）。
> 外部事实源：Langfuse（~31K★）score 挂 trace 的反馈 API（显式/隐式、categorical/numeric/boolean +
> comment）；Spring 生态（~78K★）MDC/`%X` 惯例。语义借鉴零新依赖（slf4j-api 已在 core）。

## §A MDC 会话轮次关联（T172 / impl-141）

### Problem Statement

全仓 src/main 零 MDC——日志不携带 sessionId/turnSeq，OTel span 与日志两条观测线无法互查；
排障时「某会话第 3 轮的报错」只能靠异常消息里的巧合文本反查。

### Solution

轮次执行期间在 SLF4J MDC 写入 `buzhou.sessionId` 与 `buzhou.turnSeq`，轮次终结（含异常/取消）
finally 清除；宿主日志模式（如 `%X{buzhou.sessionId}`）即可把业务日志与会话/轮次对齐，
与 OTel span 属性互查。

### User Stories

1. As a 运维工程师, I want 日志行携带 sessionId/turnSeq, so that 一条 grep 同时命中日志与链路。
2. As a 宿主开发者, I want 轮次结束后 MDC 必被清, so that 线程池复用不串会话。
3. As a 框架开发者, I want MDC 键以 buzhou. 前缀命名空间, so that 与宿主自定义 MDC 键零冲突。

### Implementation Decisions

- chat/chatForEntity 路径：调用线程 try/finally 包裹——进入轮次 put、finally remove
  （key 命名空间 `buzhou.sessionId` / `buzhou.turnSeq`，轮次号在 nextTurn 后可得）。
- stream 路径：**实现期裁定不做**（spec 草案曾计划 doOnSubscribe/doFinally 方案——实现期诊断
  证实 Spring AI 流式管线把信号发射切到 boundedElastic 线程：put 落在订阅线程、remove 落在发射
  线程，ThreadLocal MDC 既无法跟随信号、也无法在订阅线程上可靠清除（清错线程 = 订阅线程泄漏）。
  结构性限制，非实现疏漏；chat 路径 try/finally 无此问题）。
- 诚实边界（入档）：ThreadLocal MDC 不跨线程——工具 fan-out（executor 虚拟线程）与异步观测管线
  线程上的日志**不**自动携带（引入 context-propagation 库属跨线程面扩展，本片不做）；覆盖面 =
  chat/chatForEntity 轮次调用线程（会话层日志的主产地）。
- guard 拦截路径（未开轮）不写 MDC。

### Testing Decisions

- core 端到端：观察者回调（onTurnStart/onTurnEnd，与 chat 同线程）内快照 MDC 断言两键存在且值正确；
  chat 返回后断言 MDC 已清；stream（同步 Flux.just 源，信号与订阅同线程）doOnNext 内可见、
  blockLast 后清除。
- 现有测试回归：core 全量（MDC 泄漏会污染后续断言）。

### Out of Scope

- 跨线程 MDC 传播（工具线程/异步管线；需 context-propagation，量级不抵）。
- 宿主日志 pattern 配置（运维侧；runbook 示例给出）。

## §B turn 反馈捕获 API（T173 / impl-142）

### Problem Statement

无任何 turn/session 级用户反馈面：宿主想记「这轮回答好/差」只能自建旁路；负反馈无法回流评估。

### Solution

`AgentSession.rateTurn(turnSeq, type, value, comment, source)`——校验后持久化到会话 state store
（键前缀 `buzhou.feedback.`，跨实例可读）并以 `turn.feedback` 会话事件外发（webhook 监听者即得）。
Langfuse score 语义收窄：type ∈ boolean|numeric|categorical，source ∈ user|implicit。

### User Stories

1. As a 平台用户, I want 给某轮点赞/点踩, so that 反馈进入统一观测与外发面。
2. As a 宿主开发者, I want 反馈落 state store, so that 重启/跨实例后反馈仍在（可导出评估）。
3. As a 运维工程师, I want 反馈经既有事件总线外发, so that webhook 订阅方零改造收到。
4. As a 框架开发者, I want 非法 type/value/source 被拒绝, so that 反馈数据面保持可解析。

### Implementation Decisions

- API：`AgentSession.rateTurn(int turnSeq, String type, String value, String comment, String source)`
  （default 方法抛 UnsupportedOperationException，DefaultAgentSession 实现；comment/source 可空）。
- 校验：type ∈ {boolean, numeric, categorical}；boolean → value ∈ {true,false}；
  numeric → value 可解析 long；source ∈ {user, implicit}（null 归 user）；
  turnSeq ∈ [1, 当前轮次]（未来轮次拒绝）；非法 IllegalArgumentException（文案含修复建议）。
- 持久化：SessionStateStore，key = `buzhou.feedback.<turnSeq>.<epochMillis>`
  （同轮可多次反馈、时序可排）；value = URLEncoded `k=v&…` 五字段（type/value/comment/source/at，
  core 零 JSON 依赖的 lossless 形态）；producer = `turn-feedback`；createdTurn = turnSeq；无 TTL。
- 外发：既有 `dispatchEvent(SessionEvent.of("turn.feedback", payload))`——监听者/webhook
  零改造收到（at-least-once 语义沿 outbox 既有契约）。
- 关闭会话后调用：拒绝（会话已关）。

### Testing Decisions

- core 端到端：合法 boolean/numeric/categorical 三型落库（scanByPrefix 断言键与解码字段）+
  事件 payload 断言；非法 type/值/source/未来轮次各拒；关闭后拒绝。
- 回归：全量 core。

### Out of Scope

- 反馈进 observability EventRecord/dashboard 回放（事件总线面已可观测；dashboard 展示后续按需）。
- session 级聚合反馈（Langfuse #2728 同未落地；turn 级先行）。
