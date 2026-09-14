# 1176 — 影子读探针接线

**What to build:** 主路成功后备模型影子对照 + Fallback 组采样率配置。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ResilienceAdvisor.withShadowProbe + shadowMirrorIfSampled（REE 防护）
- [x] Fallback 组扩参 shadow-probe-percent（0=关默认）+ Module 装配
- [x] 四断言 + resilience 384 用例零回归

## Done

验证：`mvn -pl buzhou-resilience test` 全绿。
