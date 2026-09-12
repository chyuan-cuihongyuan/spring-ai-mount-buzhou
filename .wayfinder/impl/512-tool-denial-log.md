# 512 — 角色权限拒绝有界日志

**What to build:** ToolDenialLog（128 条环形明细 + 64 键 (role,tool) 聚合 + reason 分流）+ ToolRoleGuardHook 可选接线（拒绝路径双记，默认构造零变化）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ToolDenialLog（环形 + 聚合 + reason + 不可变快照）
- [x] ToolRoleGuardHook 可选 log 构造（拒绝双记）
- [x] 封顶/分流/零回归/不可变用例
- [x] spec 709 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-guard -am test` 绿。commit 见本轮 `feat(guard)` 提交。
