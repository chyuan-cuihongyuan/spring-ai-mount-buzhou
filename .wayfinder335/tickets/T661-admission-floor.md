---
Type: task
Status: closed
---
## Question

`SpawnAdmissionFloor` 共享地板槽 + SpawnGate 地板判定（低于地板先于
容量/排队拒——admission-floor 原因；无地板恒 LOW 零变化）。

## Resolution

done（2026-09-04）：impl-358；gate 增四参构造（Supplier<SpawnPriority>
地板，null=恒 LOW——存量构造器委托）；地板判定在 acquireSlotOrThrow
入口最前，FAIL_FAST 档同样生效。门六用例（拒/放行/原因事件/无地板零
变化/FAIL_FAST 同拒/HIGH 冻结期通行）绿——存量 SpawnGate 测试零改动
全绿即零变化证明。
