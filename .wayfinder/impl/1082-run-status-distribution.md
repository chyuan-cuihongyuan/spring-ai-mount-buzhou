# 1082 — 运行状态分布与滞后审计

**What to build:** RunStatusDistribution 纯函数（statusHistogram+runningLagMax+TurnLag worst 3）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] RunStatusDistribution（core/recovery，private 构造静态面）
- [x] RunStatusDistributionTest 五测
- [x] spec 1429 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='RunStatusDistributionTest'` 5/5 绿。
