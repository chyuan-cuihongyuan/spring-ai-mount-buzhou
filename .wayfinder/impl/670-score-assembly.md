# 670 — 健康评分端点装配

**What to build:** BuzhouHealthEndpoint 快照加 score 段（compute 投影 + 异常降级）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] buzhouSnapshot score 段 + safeScore 降级
- [x] ScoreAssemblyTest（score 段正确/mechanisms 共存/既有零回归）
- [x] spec 917 + README 行（欠账累计 906–917 十二行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=ScoreAssemblyTest,BuzhouObservabilityAutoConfigurationTest` 全绿。commit 见本轮 `feat(core)` 提交。
