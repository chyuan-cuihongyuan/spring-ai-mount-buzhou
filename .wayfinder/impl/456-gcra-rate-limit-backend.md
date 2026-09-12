# 456 — GCRA 平滑限流后端

**What to build:** opt-in 的 `GcraRateLimitBackend`（TAT 匀速、默认 β=0 严格平滑、有限突发可配、consume 强推超限、预检不推进），经构造注入 ModelRateLimiter；默认后端零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] TAT 算法全 SPI 映射（tryAcquire/consume/available/capacity/secondsUntilAvailable/kind）
- [x] nano 时钟注入（测试确定性）
- [x] 8 用例全绿（GcraRateLimitBackendTest）
- [x] spec 603 + README 行

## Done

验证：`mvn -pl buzhou-resilience -am test -Dtest=GcraRateLimitBackendTest` 绿（8/8）。commit 见本轮 `feat(resilience)` 提交。
