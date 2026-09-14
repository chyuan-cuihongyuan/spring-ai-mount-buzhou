# 1096 — 评估通过率趋势审计

**What to build:** EvalPassRateTrend 纯函数（Theil–Sen 成对斜率中位数+方向闭集）+ 六测。

**Blocked by:** None.

**Status:** done

- [x] EvalPassRateTrend（core/eval，private 构造静态面）
- [x] EvalPassRateTrendTest 六测
- [x] spec 1444 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='EvalPassRateTrendTest'` 6/6 绿。
