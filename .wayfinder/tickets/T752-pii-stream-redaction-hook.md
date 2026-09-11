---
Type: task
Status: closed
---
## Question

guard `PiiStreamRedactionHook`（order 75）：滑动窗口缓冲（默认 128、可
构造覆盖）跨 chunk 脱敏 + flush 排空；命中计数复用 pii.redactions +
PiiHitStats OUTPUT 侧；yml `pii.reply-redaction`/`reply-window` 装配。

## Resolution

done（2026-09-11）：impl-403；跨 chunk 断裂/窗界/统计/yml 装配用例绿。
