# 1112 — design-incompleteness 小缺口清扫 F7+F10（M 系 R10）

**What to build:** canary.selected payload 补 sessionId + spec 07 resume 名回写。

**Blocked by:** T2269 / T2270（同轮 shape+verify）。

**Status:** done

- [x] F7：payload 条件含 sessionId（null 省略）+ CanaryRoutingEndToEndTest 钉住
- [x] F10：spec 07 双处回写指向 SessionInterrupts.resumeWith
- [x] design-incompleteness F7/F10 闭环标记 + F 系查重结论入档（F1/F9 留候选池）

## Done

验证：resilience 定向测试绿。commit 见本轮 fix 提交。
