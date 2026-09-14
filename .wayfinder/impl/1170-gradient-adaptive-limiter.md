# 1170 — 梯度式自适应并发闸

**What to build:** GradientAdaptiveLimiter（延迟梯度驱动动态上限）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] GradientAdaptiveLimiter（双 EMA + 混合调整 + tryAcquire/release + View 观测）
- [x] 七断言测试全绿

## Done

验证：`mvn -pl buzhou-core test -Dtest=GradientAdaptiveLimiterTest`。
