# 1164 — 孤类普查 + 熔断旁路遥测接线

**What to build:** 15 项孤类普查入档 + CircuitCrashLoopDetector/HalfOpenProbeStats 接线。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Explore 普查（126 初筛 / 40 深验 / 15 确认 + 6 疑似入档 spec 1611）
- [x] ModelCircuitBreaker.withTelemetry + 三喂点（跳闸/恢复/半开探测成败）
- [x] ResilienceModule 恒挂装配（crash-loop 10min/3 次）
- [x] CircuitTelemetryWiringTest 三断言 + resilience 380 用例零回归

## Done

验证：`mvn -pl buzhou-resilience test` 全绿。
