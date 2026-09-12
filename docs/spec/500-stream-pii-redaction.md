# Spec 500 — 流式回复 PII 脱敏（effort #500）

> wayfinder map：`.wayfinder/maps/effort-500.md`（T751–T752）。E 会话第 1 轮。

## Problem Statement

guard 的 PII 脱敏覆盖输入缝（spec 106 beforeTurn）与工具输出缝（spec 86
afterTool）——**模型回复出站缝**空白：模型自产或从上下文复读的 PII
（手机号/邮箱等）直达订阅者与非流式调用方，并进入 replyAccumulator 观测
累积面。流式场景 PII 可跨 chunk 断裂（`"138"`+`"0013"`），逐 chunk 独立
扫描必漏——需要跨界感知的流式改写。

## Solution

core 出站过滤 SPI + guard 窗口缓冲实现（Presidio 流式匿名化 + 流式 WAF
回看窗口思想）：

- **SPI（core.hook）**：`StreamTextFilter`（`String filter(String chunk)`
  逐 chunk 改写、`String flush()` 收口排空；每轮新建、单轮单用）；
  `BuzhouHook.replyStreamFilter()` 默认方法（null = 不参与）；
  `HookChain.newReplyFilters()` 按 hook 序收集。
- **接线（DefaultAgentSession 两缝）**：流式——`map` 逐 chunk 过滤重建
  ChatResponse（文本无变化零重建）+ `concatWith` 收口 flush chunk（进入
  doOnNext 累积面，观测与用户所见一致）；非流式 chat——整段
  `filter+flush` 拼接改写。无 filter 钩子时零包装零开销。
- **guard 实现**：`PiiStreamRedactionHook`（order 75）——滑动窗口缓冲：
  缓冲区达 window 前缀只发安全前缀（emitLen = len−window+1，任何未来
  match 覆盖不到已发字符），跨界实体留窗待完整后脱敏；flush 全量排空。
  窗口默认 128（RFC 5321 本地段 64 + 常见域名；固定型上限 19 全覆盖），
  可构造指定。命中计数复用 `buzhou.guard.pii.redactions`（type tag）+
  `PiiHitStats` Side.OUTPUT。
- **yml**：`buzhou.guard.pii.reply-redaction`（默认 false，106 输入侧
  开关同族）+ `reply-window`（可选，默认 128）。

## User Stories

1. 作为安全宿主，我想让模型流式回复里的 PII 在离场前占位符化，so 订阅者
   与观测面看不到手机号/邮箱/身份证等（86/106 之外的第三缝闭环）。
2. 作为集成方，我想流式输出不被逐 chunk 扫描切坏，so 跨 chunk 的实体也
   能完整识别且文本顺序不变（只是有界滞后）。

## Implementation Decisions

- 会话历史不回溯清洗（历史内模型自产 PII 随下一轮再注入——出站缝每轮
  拦截，用户面恒净；记忆治理域不越界）。
- 窗口放不下的超长跨界实体不保（诚实边界，窗口可调）。
- flush 文本过下游 filters（组合序：f_i.flush 经 f_{i+1..n}.filter）。
- ChatResponse 重建仅文本变化时发生（零命中零重建——引用等短路）。

## Testing Decisions

- 窗口过滤器单测：手机号跨三 chunk 断裂合并后占位符化；flush 排空窗尾；
  无 PII 文本逐 chunk 拼接恒等；窗口边界（恰 128）不漏。
- core 两缝 E2E（ScriptedChatModel）：流式订阅者所见全为占位符文本 +
  flush chunk 也到达；chat() 返回值脱敏；无 filter 时 chunk 原样透传
  （零行为变化回归）。
- yml：reply-redaction=true 装配钩子、缺省不装。

## Out of Scope

- 会话历史回溯清洗；NER 型实体检测（86 诚实边界同）；工具调用 chunk；
  窗口自适应。

## Further Notes

- 新公共类型 `StreamTextFilter`、`PiiStreamRedactionHook` 随轮 regenerate
  快照 + api-surface.md 加行。
