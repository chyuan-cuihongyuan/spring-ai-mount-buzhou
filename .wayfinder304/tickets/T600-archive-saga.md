---
Type: task
Status: closed
---
## Question

archive() saga 接线：归档条目为 undo log；部分失败上抛（诚实化）；补偿全成
净回原状 / 补偿失败归档键保留。

## Resolution

done（2026-09-01）：impl-327；SessionArchiver.archive 两步 saga（写归档→级联删，
step2 失败从条目写回三槽）。SessionArchiverCompensationTest 双场景绿。
