# 1111 — 危险工具默认 HITL 自动带入桥（M 系 R9）

**What to build:** core DangerousToolRegistry 桥 + tools 灌注 + guard 默认并入 + auto-dangerous-bridge 开关。

**Blocked by:** T2267 / T2268（同轮 shape+verify；S1 修复 T2265 前置）。

**Status:** done

- [x] core spi DangerousToolRegistry（register/registered/reset）
- [x] tools autoconfig 灌注 + guard autoconfig afterName 时序 + 默认并入（yml 优先）
- [x] 开关 buzhou.guard.auto-dangerous-bridge（默认 true）
- [x] 注册表单测 + 桥单测 + 既有零回归 + design-incompleteness S2 闭环标记

## Done

验证：core/tools/guard 定向测试绿。commit 见本轮 fix 提交。
