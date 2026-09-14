# 1177 — 危险工具 HITL 豁免征询

**What to build:** DangerousToolGuardHook 豁免征询 + GuardModule 暴露面。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] DangerousToolGuardHook：+exemptions 构造 + 征询点 + EXEMPTION_APPLIED 事件
- [x] GuardModule：registry 恒建传入 + exemptions() 公开面
- [x] 四断言 + guard 343 用例零回归

## Done

验证：`mvn -pl buzhou-guard test` 全绿。
