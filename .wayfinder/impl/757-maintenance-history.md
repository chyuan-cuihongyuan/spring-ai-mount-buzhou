# 757 — 维护窗历史读面

**What to build:** MaintenanceGate 闭窗历史环（HistoryEntry + history() 新→旧 + noteRefused 窗内计数）+ Hook 同点补计数 + 生命周期测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] HistoryEntry 嵌套 record（仅已闭窗）
- [x] 有界环 16 新→旧 + history() 不可变快照
- [x] noteRefused（窗外无害无痕）
- [x] Hook counter 旁同点补一行
- [x] MaintenanceGateHistoryTest（闭窗史/计数/有界/幂等/不可变）
- [x] spec 1004 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='MaintenanceGateHistoryTest,MaintenanceGateTest,MaintenanceGateHookTest'` 全绿。commit 见本轮 `feat(core)` 提交。
