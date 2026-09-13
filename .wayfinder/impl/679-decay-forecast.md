# 679 — 事实衰减预报读法

**What to build:** FactDecayPolicy.turnsUntilFloor（逆函数解析 + 边界语义）+ 互逆性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] turnsUntilFloor
- [x] DecayForecastTest（边界/互逆/校验）
- [x] spec 926 + README 行（欠账累计 906–926 二十一行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=DecayForecastTest` 全绿。commit 见本轮 `feat(core)` 提交。
