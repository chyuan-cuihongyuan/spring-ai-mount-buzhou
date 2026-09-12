# 743 — 导出脱敏命中计数

> 来源：G 会话第 44 轮 = effort #743（SessionExportSanitizer 治理证据面）/ [T1088](../../.wayfinder/tickets/T1088-sanitizer-hit-counts.md) / [T1089](../../.wayfinder/tickets/T1089-sanitizer-hit-counts-verify.md) / impl 644。

## Problem

SessionExportSanitizer 对导出做 PII 脱敏但「动了多少刀、什么类型最多」不可见——导出前想评估脱敏覆盖率（合规审计常问）没有数。PiiHitStats 是输入/流式路径的统计，导出路径独立。

## Solution

- `SessionExportSanitizer.hitCounts()`：Map<类型或规则名, 次数>——PiiType 名（PHONE→CN_PHONE 等枚举名）+`custom:规则名`；
- `totalHits()`：总数；
- 计数累计跨多次 sanitize 调用（同 sanitizer 实例生命周期）；
- CustomPiiRules 加 `rules()` 只读访问器（自定义规则命中归因）。

## Out of Scope

按消息/字段定位（聚合口径先行）；流式路径改造（PiiHitStats 既有）。
