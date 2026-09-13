# 918 — 丢弃计数 reason 维度指标

> 来源：I 会话第 19 轮 = effort #918（[T1287](../../.wayfinder/tickets/T1287-drop-reason-metric-shape.md) / [T1288](../../.wayfinder/tickets/T1288-drop-reason-metric-verify.md) / impl 671）。spec 900 读面与 spec 13 指标面的口径统一轮。

## 背景

`buzhou.eventbus.dropped` 无维度 counter 与 EventDropBreakdown（spec 900）结构化读面并存但口径脱节——指标无 reason 维度（运维无法按原因告警），breakdown 的 reason 是内联字符串字面量（与 WARN 文本、指标 tag 三处可能漂移）。

## 目标

- `BufferedEventDispatcher`：DROP_REASON_* 六常量抽出（static final String——drop-oldest / drop-oldest-race / block-timeout / interrupted / dispatcher-closed / closed-undelivered），countDrop 内三处调用点与 dropsByReason 键统一引常量；
- 指标双轨：保留无维度总量 counter（既有面板零分裂）；新增 `buzhou.eventbus.dropped-reason` 带 reason tag counter（值域封闭 6 值——基数天然有界）；
- `EventDropBreakdown` 文档注明与指标 tag 同源口径；
- 既有 stats()/dropBreakdown() 读面零变化。

## 兼容性

纯增量指标序列 + 常量化重构（行为零变化）；既有无维度 counter 保留。
