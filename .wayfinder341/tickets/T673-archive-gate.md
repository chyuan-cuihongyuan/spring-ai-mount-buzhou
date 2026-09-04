---
Type: task
Status: closed
---
## Question

ArchivePurgeJob 选主门（调度先取续/非 leader 跳计数/stop 让位/手动
不设门）。

## Resolution

done（2026-09-04）：impl-364；构造器重载（旧委托 null）+ 调度 lambda
门 + stop 让位；三用例（跳周期计数/手动不设门/停机让位）绿。
