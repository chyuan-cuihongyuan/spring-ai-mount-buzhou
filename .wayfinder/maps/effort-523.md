# Wayfinder Map — Buzhou 失败轮快照面（effort #523，E 会话第 24 轮）

> E 会话第 24 轮（Sentry event payload 思想）。勘察：错误轮有采样入集
> （423 TurnErrorSampler——评测域）但**排障域**无读数面：错误轮的错误
> 类/消息/输入最小复现集散在日志里，无环形快照+导出。

## Destination

`core.recovery.FailureTurnSnapshots implements SessionObserver`（缝同
423：onTurnStart 记输入/onTurnError 落快照；实例=会话粒度经
SessionAssemblyContext.addObserver 注册）：Snapshot(turnSeq/errorClass/
errorMessage 256 截断/inputPreview 512 截断+省略号)+环形 128（total
计数环外）+exportJsonl（60/67 导出族同构，转义完备单行）。

## Notes

- 号段：spec 523 / T797–T798 / impl-426。
- 借鉴源：Sentry event payload（复现最小集）。

## Out of scope

- 完整输入持久化（敏感面归宿主管道）；跨会话聚合；自动上报。

## Tickets

- [x] [T797 快照原语与环形](../tickets/T797-failure-snapshots.md)
- [x] [T798 JSONL 导出与 E2E](../tickets/T798-failure-snapshots-export.md)
