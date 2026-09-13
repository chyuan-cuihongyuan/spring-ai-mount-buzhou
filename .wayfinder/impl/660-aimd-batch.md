# 660 — outbox 投递批量 AIMD 自适应

**What to build:** WebhookEventForwarder 自适应批量（加性增/乘性减/夹取 + defer 中性 + currentBatchSize 读数）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] setAdaptiveBatchEnabled（默认关）+ AIMD 裁决 + 夹取
- [x] WebhookAimdBatchTest（HttpServer Collector 驱动：增/减/夹取/默认关）
- [x] spec 907 + README 行

## Done

验证：`mvn -pl buzhou-core test -Dtest=WebhookAimdBatchTest` 全绿。commit 见本轮 `feat(core)` 提交。
