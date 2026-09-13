# 789 — 工具权限判定分布读面

**What to build:** ToolPermissions checks/allowed/deniedUndefinedRole/deniedByRules 四计数 + 嵌套 PermissionStats + stats() + 三形判定测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四桶计数埋点（精确/前缀/全放/两类拒）
- [x] PermissionStats 嵌套 record + stats()
- [x] PermissionStatsTest（三形 allowed/未定义拒/规则拒/守恒/fresh 零值）
- [x] spec 1037 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='PermissionStatsTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
