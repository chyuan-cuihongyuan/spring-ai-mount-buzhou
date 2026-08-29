# Spec 207 — 降级链单窗视图（effort #223）

> wayfinder map：`.wayfinder223/MAP.md`（T581–T582）。Grafana 单窗思想——
> 链/驱逐/演练三源合成一页备胎全景。

## Problem Statement

备模型状态散在三处：链配置（谁在链上什么序）、驱逐器（谁被逐）、演练器
（谁验证过）——回答「现在备胎可用性全景」要拼三个面。运维排障时拼面即
浪费黄金时间。

## Solution

`FallbackChainView`（resilience/fallback，纯函数）：

- **合成**：`report(chain, ejection, drill, freshMaxAge)` → 每链成员一行
  `Row(name, position, ejected, fresh, lastVerifiedAt, lastFailedAt)` +
  `recommendedOrder`（链序剔除被逐者；过期未验<b>保留但标注</b>——标注与
  剔除分明，决策归人）。
- **空链**：空报告（诚实空态）。

## User Stories

1. 作为运维，一页看全：谁在链、谁被逐、谁验证过——排障入口单窗化。
2. 作为策略，recommendedOrder 即「健康池 + 验证标注」的即用序。
3. 作为宿主，纯函数零状态——任何时点快照可存档对比。

## Implementation Decisions

- 只读组合（三源零改动）；行字段保留原始事实（面板自决呈现）。

## Testing Decimals

- 三源字段正确合成；被逐者剔除出建议序；过期未验保留+标注；空链空报告；
  无演练记录（null drill）降级处理。

## Out of Scope

- JSON 导出；健康端点；自动重排。

## Further Notes

- 备胎治理面板化：驱逐（149）+ 演练（195）+ 链（15）→ 单窗（本轮）。
