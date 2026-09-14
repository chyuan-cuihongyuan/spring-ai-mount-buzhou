# 1155 — 熔断启动宽限期

**What to build:** circuit.warmup 配置 + 跳闸判定豁免 + warmupSuppressedCount 观测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ResilienceProperties.Circuit 扩参 warmup（10 参主构造 + 4 兼容构造保留）
- [x] ModelCircuitBreaker：warmupUntil + 判定豁免 + 计数读数
- [x] CircuitWarmupTest 四断言
- [x] resilience 模块全量绿（363 用例）

## Done

验证：`mvn -pl buzhou-resilience test` 全绿。
