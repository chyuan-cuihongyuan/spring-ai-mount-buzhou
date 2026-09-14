# 1173 — 空闲监控全链接线

**What to build:** IdleMonitorHolder + SessionFeaturesHook 节拍 + 装配 bean。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] IdleMonitorHolder（store/monitor/histogram + sweepAndRecord）
- [x] SessionFeaturesHook：null→Holder store + afterTurn 32 轮节拍
- [x] 装配 bean（buzhou.session.features.enabled 默认开）
- [x] 三断言 + 既有 12 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest='IdleMonitorHolderTest,IdleSessionMonitorTest,SessionFeaturesTest'`。
