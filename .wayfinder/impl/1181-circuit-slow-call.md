# 1181 — 熔断慢调用率维度

**What to build:** withSlowCallPolicy + 慢样本窗 + 判定叠加 + advisor 喂入。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ModelCircuitBreaker：字段/带时长 record 重载/windowSlow 环形/判定叠加/resetWindow 同步
- [x] ResilienceAdvisor 主路径 nanoTime 喂入
- [x] 五断言 + resilience 389 用例零回归

## Done

验证：`mvn -pl buzhou-resilience test` 全绿。
