# 774 — HITL 审批操作分布读面

**What to build:** GuardAuthApi approved/rejected/revoked 三计数 + 嵌套 AuthOperationStats + stats() + 操作分布测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数（approve/reject/revoke 各归其位）
- [x] AuthOperationStats 嵌套 record + stats()
- [x] AuthOperationStatsTest（fresh 零值/三分操作/reject 不写授权/便捷重载同计）
- [x] spec 1021 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='AuthOperationStatsTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
