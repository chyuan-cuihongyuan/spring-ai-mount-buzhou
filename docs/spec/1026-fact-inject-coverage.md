# 1026 — 事实注入覆盖读面

> 来源：J 会话第 27 轮 = effort #1026（[T1503](../../.wayfinder/tickets/T1503-fact-inject-coverage-shape.md) / [T1504](../../.wayfinder/tickets/T1504-fact-inject-coverage-verify.md) / impl 779）。与 R21 同文件族延续（spec 07 事实管线的渲染端）。

## Problem Statement

FactAttachmentRenderer（spec 07 注入机制渲染端）把 activeFacts 渲染为 prompt 附件段，max-inject-chars 约束下逐条截断并以 key 指针附尾——但**渲染次数/注入条数/省略条数**零计数：「max-inject-chars 是否过紧」（省略常态化）只能翻文本指针，无量化水位。

## 目标

- `FactAttachmentRenderer` 增量（buzhou-guard fact 包，实例级）：`renders`（产出非空附件的渲染次数）/ `factsInjected`（实际注入的事实条数）/ `factsOmitted`（超限省略条数）三 AtomicLong。
- 嵌套 record `FactInjectStats(long renders, long factsInjected, long factsOmitted)` + `stats()` 快照。
- 两参 render 收敛为三参实现的 MAX_VALUE 委托（输出恒等——零行为变化收敛）。

## 兼容性

纯增量读面：渲染输出与省略指针文本逐位不变；无新配置项。

## Out of Scope

- 按 producer 分桶注入统计。
- 省略事件的独立事件面（指针文本已可排障）。
