# 1183 — A/B 胜率 Wilson 置信区间

**What to build:** WilsonInterval 纯函数 + ab.run.completed 事件区间字段。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] WilsonInterval（of/of(z)，95% 默认）
- [x] 事件 payload winRateAciLow/High（decided 口径）
- [x] 四断言 + Pairwise 11 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest='WilsonIntervalTest,PairwiseEvalRunnerTest'`。
