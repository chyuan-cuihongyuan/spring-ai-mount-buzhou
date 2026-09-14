# 1184 — 退避抖动模式可配

**What to build:** JitterMode + withJitterMode + yml jitter-mode + 值域测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] JitterMode 枚举（EQUAL/FULL/DECORRELATED）+ computeBackoff 三分派
- [x] ResilienceProperties 顶层扩参（18 参）+ 17/16 兼容构造 + fail-fast
- [x] ResilienceModule withJitterMode 装配
- [x] 四断言 + resilience 393 用例零回归；ObjectMapper 审计留痕（68 处均合规）

## Done

验证：`mvn -pl buzhou-resilience test` 全绿。
