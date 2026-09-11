# Spec 542 — 死信 JSONL 导出（effort #542）

> wayfinder map：`.wayfinder/maps/effort-542.md`（T837–838）。E 会话第 42 轮。

## Problem Statement

死信清单只有 forwarder 内存查询（上限 100）——与 OLAP/归档管道同构
搬运的 JSONL 导出空白。

## Solution

`webhook.WebhookDeadLetterJsonl.export(Writer, deadLetters)`：一行一
死信（eventId/type/attempts/createdAtEpochMs；转义完备单行）+行数返回。

## User Stories

1. 作为运维，我想死信清单导出 JSONL 喂给归档/分析管道， so 死信治理
   有离线数据面。

## Implementation Decisions

- 转义完备（引号/换行/反斜杠）——单行 JSONL 契约。
- 源经 forwarder.deadLetters() 查询（上限 100 既有语义）。

## Testing Decisions

- 一行一死信断言；引号/换行转义；行数返回；空清单零行。

## Out of Scope

- reason 列；自动导出调度。

## Further Notes

- 新公共类型 `WebhookDeadLetterJsonl` 随轮 regenerate 快照 +
  api-surface.md 加行。
