# Wayfinder Map — Buzhou 工具入参限幅（effort #506，E 会话第 7 轮）

> E 会话第 7 轮。勘察：31 结果限幅（20K 截断+提示尾+per-tool glob 豁免）
> 是**出站面**；**入站面**（模型拼超大入参——万行 JSON/整文件内联炸下游）
> 无护栏。nginx client_max_body_size 思想。

## Destination

`core.exec.ToolInputLimiter`（31 对称面）：violation(toolName, args) 超限
返回结构化反馈（工具名+超限量+精简指引，**不回显入参**——回显即重新入
上下文）、未超限 null；glob per-tool 覆盖（31 同法）；counter
buzhou.tools.input-rejected（tag tool）。`ToolInputLimiterHolder`（Holder
模式同结果限幅，默认 disabled——零默认行为变化 opt-in）。
HarnessToolCallingManager.executeOne 校验后、deadline 前插检查——超限不
执行、recordOutcome VALIDATION_REJECTED、反馈走 REASK 通道回模型。
yml `buzhou.tools.{input-limit-chars, input-limit-overrides}`（默认 -1）。

## Notes

- 号段：spec 506 / T763–T764 / impl-409。
- 借鉴源：nginx client_max_body_size（413 响应实体过大）。
- 拒绝而非截断：截断 JSON 入参必坏语义（与结果截断方向相反的诚实选择）。

## Out of scope

- 入参 schema 深度限制（嵌套层数）；流式入参；自动 spill 入参。

## Tickets

- [x] [T763 ToolInputLimiter 原语](../tickets/T763-tool-input-limiter.md)
- [x] [T764 manager 缝与装配](../tickets/T764-tool-input-limit-assembly.md)
