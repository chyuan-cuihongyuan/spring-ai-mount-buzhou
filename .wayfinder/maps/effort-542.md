# Wayfinder Map — Buzhou 死信 JSONL 导出（effort #542，E 会话第 42 轮）

> E 会话第 42 轮（60/67 导出族同构轮）。勘察：死信清单只有 forwarder
> 内存查询（上限 100）——与 OLAP/归档管道同构搬运的 JSONL 导出空白。

## Destination

`webhook.WebhookDeadLetterJsonl`（纯函数）：export(Writer, deadLetters)
→ 一行一死信（eventId/type/attempts/createdAtEpochMs；转义完备单行）+
行数返回。

## Notes

- 号段：spec 542 / T837-838 / impl-444。

## Out of scope

- reason 列（537 tag 计数先行）；自动导出调度。

## Tickets

- [x] [T837 导出原语](../tickets/T837-dead-letter-jsonl.md)
- [x] [T838 转义与行数](../tickets/T838-dead-letter-jsonl-escape.md)
