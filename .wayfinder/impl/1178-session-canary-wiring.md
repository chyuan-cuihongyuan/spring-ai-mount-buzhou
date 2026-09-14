# 1178 — 跨会话泄漏金丝雀接线

**What to build:** SessionCanaryHook + GuardModule.leakCanary(salt) 装配。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionCanaryHook（plant + afterModel 扫描 + 泄漏事件）
- [x] GuardModule.Builder.leakCanary opt-in 装配
- [x] 两断言 + guard 全量零回归；LayeredPolicy 裁决入档

## Done

验证：`mvn -pl buzhou-guard test` 全绿。
