# Spec 402 — 结构化输出执法（effort #402）

> wayfinder map：`.wayfinder/maps/effort-402.md`（T695–T696）。D 会话第 3 轮。

## Problem Statement

「让模型输出 JSON」目前只能靠提示词恳求：模型终态输出无运行时契约执法，
坏输出（散文包裹、缺键、类型错、代码围栏）直接漏到宿主解析层炸
NPE/JSONException。工具入参有 ToolArgsValidator 执法，模型输出这一最大
泄漏面反而无门。

## Solution

`resilience.structured` 包（instructor 借鉴——验证失败把错误喂回模型自修复）：

- **`OutputSchema`**（最小子集契约）：`required`（必备键表）+
  `propertyTypes`（键→类型：string/number/integer/boolean/array/object）；
  `validate(text)` 解析（代码围栏 ```json 剥离、非 object 即错）返回错误
  清单（空 = 合法）；`summary()` 人读契约供反馈消息。
- **`StructuredOutputAdvisor`**（BaseAdvisor，order +480——缓存(+460)内、
  观测(+500)外：外层缓存看到的键→已修复响应，缓存语义正确）：
  - 首答走链 nextCall（内层观测/hook/韧性全跑）；**修复直达模型终端**
    （advisor 链是单遍弹出式 Deque，重入 nextCall 必炸——ResilienceAdvisor
    内层重试同口径约定：内层重试直达终端、外层把整环记一次逻辑调用；
    修复明细经 structured-output 事件流可见）；
  - 验证终态文本；合法即返（修复过则发 `repaired` 事件）；
  - 不合法 → 发 `repair-attempted` 事件 → 重建请求（原指令 + 上轮
    AssistantMessage + 结构化反馈 UserMessage：错误清单 + 契约 summary +
    上轮回复截断 500 字）重调；
  - `maxRepairAttempts`（默认 1；0 = 纯执法不修复）耗尽 → 发 `violation`
    事件 + 抛 `StructuredOutputViolationException`（外层 hook/观测按
    模型调用错误可见）；
  - 流式（adviseStream）直通——聚合后无法重调，诚实边界。
- 装配：`buzhou.resilience.structured-output.{enabled, max-repair-attempts,
  schema.{required, properties}}`——独立 RuntimeConfig bean（assembly
  customizer 注 advisor；不动 ResilienceModule 内路）；enabled=true 而
  schema 全空 fail-fast（恳求开启却没契约是配置错误）。

## User Stories

1. 作为宿主开发者，我想声明 JSON 契约后坏输出自动带错误反馈重试，
   所以 解析层的防御代码不再需要。
2. 作为宿主开发者，我想耗尽后拿到明确异常（含错误清单）， 所以坏
   输出不再静默漏到下游。
3. 作为运维，我想修复/违规都有事件， 所以 结构化输出的质量趋势可见。

## Implementation Decisions

- 契约最小子集（object + required + 类型表）：完整 JSON Schema 引擎
  非本轮；未知声明类型跳过（宽容）。
- 修复环只走 adviseCall；外层响应缓存见到的键→已修复响应（同请求
  得合法答案，缓存语义正确）。

## Testing Decisions

- 首答合法零修复；首答坏→修复成功（模型两次调用 + 事件对）；
- 耗尽抛异常 + violation 事件（maxRepairAttempts=0 纯执法）；
- 围栏剥离；yml 装配 + enabled-无-schema fail-fast。

## Out of Scope

- 完整 JSON Schema 引擎；流式执法；per-prompt 契约；宽严模式。

## Further Notes

- 新公共类型 `OutputSchema` / `StructuredOutputAdvisor` /
  `StructuredOutputViolationException` / `StructuredOutputProperties`
  随轮 regenerate 快照。
