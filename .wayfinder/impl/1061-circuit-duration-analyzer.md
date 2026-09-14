# 1061 — 断路器状态时长分析器

**What to build:** CircuitStateDurationAnalyzer 纯函数（journal Transition 积段→逐状态时长+OPEN 占比）+ 五测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] CircuitStateDurationAnalyzer（resilience/circuit，private 构造静态面）
- [x] CircuitStateDurationAnalyzerTest（积分/无序/单段延伸/占比/分组降序）
- [x] spec 1408 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-resilience -am test -Dtest='CircuitStateDurationAnalyzerTest'` 5/5 绿。
