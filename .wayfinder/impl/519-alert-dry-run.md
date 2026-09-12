# 519 — 告警规则 dry-run

**What to build:** AlertRuleEngine.dryRun(now)——纯只读推演 DryRunReport{wouldFire, wouldRecover, pending}，三不承诺（状态机/通知/指标零副作用）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] dryRun + DryRunReport/PendingRule record
- [x] 三分类 + 三不承诺 + 实弹零污染用例
- [x] spec 716 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
