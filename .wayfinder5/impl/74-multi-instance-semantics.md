# 74 — 多实例语义显式化（T99 决策落地）

**What to build:** resilience 启动多实例告警 + spec 23 节（runbook §6 文档化已在 impl-72 落）。

**Blocked by:** None.

**Status:** done

## Done

验证：`./mvnw -pl buzhou-resilience clean test` 78/78 绿。
落地：BuzhouResilienceAutoConfiguration 启动告警（store.type≠memory 且限流/日配额/熔断任一启用 → WARN 一次，指向 runbook §6；不拒绝——粘性+独占合法）；spec 23 §多实例语义回填。