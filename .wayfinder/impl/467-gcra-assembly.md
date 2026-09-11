# 467 — GCRA 限流 yml 装配

**What to build:** rate-limit.smoothing/gcra-burst-tolerance yml → GcraRateLimitBackend 装配（闭集校验、共享后端优先）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] RateLimit 扩组件（@ConstructorBinding 修多构造绑定坑）+ Module 后端选择
- [x] 装配 3 用例 + yml 绑定 1 用例绿；resilience 237/237 零回归
- [x] spec 614 + README 行

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
