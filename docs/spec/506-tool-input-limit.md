# Spec 506 — 工具入参限幅（effort #506）

> wayfinder map：`.wayfinder/maps/effort-506.md`（T763–T764）。E 会话第 7 轮。

## Problem Statement

31 结果限幅护出站面；入站面空白——模型可拼超大入参（万行 JSON/整文件
内联）直发下游工具，炸内存/超时/拖垮外部系统。nginx client_max_body_size
同思想：执行前体积门。

## Solution

`core.exec.ToolInputLimiter` + `ToolInputLimiterHolder`（31 对称面）：

- violation(toolName, args)：超限返回结构化反馈（工具名+超限字符数+
  精简指引），**不回显入参**（回显即重新入上下文）；未超限 null。
- glob per-tool 覆盖（31 同法，`*` 通配；-1 该工具不限）。
- counter `buzhou.tools.input-rejected`（tag tool 有界）。
- manager 缝：executeOne 校验后、deadline 前检查——超限不执行、
  recordOutcome VALIDATION_REJECTED、反馈走 REASK 通道。
- yml `buzhou.tools.{input-limit-chars, input-limit-overrides}`，默认 -1
  不限（**零默认行为变化**，opt-in）。

## User Stories

1. 作为宿主，我想给工具入参设体积上限，so 模型拼出的超大 payload 在
   执行前被拦（结构化反馈引导精简重试）而非炸下游。
2. 作为集成方，我想 per-tool 差异化（web 工具限 4K、内部工具不限），
   so 上限不是一刀切。

## Implementation Decisions

- 拒绝而非截断：截断 JSON 入参必坏语义（与结果截断方向相反）。
- 默认关闭：结果限幅默认开是 A 会话决定；入参限幅按 E 会话零默认行为
  变化纪律 opt-in。
- Holder 模式：per-session 可经 toolManager 既有覆盖面后续扩散。

## Testing Decisions

- 默认 disabled 零行为；超限拒绝不回显；glob 覆盖；负值 fail-fast。
- 会话 E2E：超限工具调用不执行（执行标志 false）、反馈经第二次模型
  调用可见（REASK）。
- yml：声明即生效、缺省 disabled。

## Out of Scope

- schema 深度限制；流式入参；自动 spill 入参。

## Further Notes

- 新公共类型 `ToolInputLimiter`、`ToolInputLimiterHolder` 随轮 regenerate
  快照 + api-surface.md 加行。
