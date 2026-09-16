# Spec 2045 — 属性白名单过滤器（effort #2045，R46）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3191–T3192，impl 1596）。
> 借鉴：OpenTelemetry View/processor——导出前属性瘦身 + 丢弃显形。

## Problem Statement

观测导出的属性无限生长：业务把 prompt/邮箱/大 payload 塞进 span 与
事件属性——导出体积、基数、敏感面全失控；一刀切裁剪又静默——哪个
属性被丢多少次无人知，治理无对账面。

## Solution

`AttributeWhitelist`（buzhou-observability pipeline，纯函数无状态）：

- 白名单模式：`filter(attributes)` 盘内保留（值原样传递）、盘外丢弃
  **并分属性计数**（droppedByAttribute——丢弃不静默）；
- 通配模式 `allowAll()`：全保留零丢弃（治理未配置不断流）；空表 =
  显式全拒（收紧口径可表达）；
- `allowedAttributes()` 字典序快照 / `isAllowAll()`；
- 契约：名单非 null（null 与空表语义区分——显式拒绝静默歧义）、项
  非空非白 fail-fast。

## User Stories

1. 作为导出作者，白名单内瘦身导出——体积/基数/敏感面三收口。
2. 作为治理者，droppedByAttribute 显形被丢热点——该进白名单还是该
   改埋点，有据可依。

## Testing Decisions

- 盘内留盘外计（email/prompt 各 1）；通配全留零丢；空表全拒显式；
  值同引用原样传递；逐次调用独立计数；快照字典序；畸形四型（null
  名单、名单含 null/空白、null attributes）fail-fast。

## Out of Scope

- 不做模式匹配/前缀白名单（精确名单口径）；不接导出管线（接线归
  后续轮）。

## Further Notes

- 与 tag 基数守卫（#111）互补：守卫计数告警，白名单主动过滤。
