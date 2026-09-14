# 842 — J 系阶段对账审计轮（R90 周期预检）

**What to build:** R81–R89 工件对账 + 隔离 worktree 全仓 verify + 双门复跑 + 发现入档（spec 1090）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 对账：README 1085 行吞噬修复 / spec 1081–1089 全实存 / 票 T1597–T1634 / impl 823–841 / 台账 61–89 全绿
- [x] 票 T1635–T1636 + spec 1090 + README 行
- [x] README 1085 行吞噬修复（三型之一再现，幂等脚本再修）
- [x] worktree 全仓 verify 绿（第三轮 BUILD SUCCESS 17 模块 2:50 + 双门；前两轮红为 1085 吞噬与 1090 行自漏，均已修 b5d6a21f/5dd12397）

## Done

验证：`/tmp/j90-verify` worktree 全仓 `mvn verify` 结果回填本节（见 spec 1090 验收节）。commit 见本轮 `chore/docs` 提交。
