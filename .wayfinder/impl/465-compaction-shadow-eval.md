# 465 — 微压缩影子干跑评估

**What to build:** CompactionShadowEvaluator（evaluate/sweep/evaluateAndEmit——纯函数干跑不应用）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] evaluate + sweep 梯度 + 事件外发（applied=false）
- [x] 4 用例绿（同口径零变异/单调/事件/校验）
- [x] spec 612 + README 行

## Done

验证：`mvn -pl buzhou-memory -am test` 绿。commit 见本轮 `feat(memory)` 提交。
