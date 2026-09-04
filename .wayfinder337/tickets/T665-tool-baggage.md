---
Type: task
Status: closed
---
## Question

`ToolBaggage` per-runtime 有界键值面（put/remove/view 快照/64 键 256
字符封顶）。

## Resolution

done（2026-09-04）：impl-360；六用例（往返/快照不可变/空键拒绝/键数
封顶折最旧值？——否：达上限拒绝新键+计数/值长截断拒绝/isEmpty）绿。
