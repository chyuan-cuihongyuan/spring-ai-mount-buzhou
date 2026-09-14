# 1175 — R19 对账 NPE 修复

**What to build:** TokenBudgetHook.afterModel 对账前置 null 防御。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] request/prompt/instructions 三重缺席跳过
- [x] CounterAtomicitySpreadTest 恢复 + 7 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest='CounterAtomicitySpreadTest,CalibrationAuditHolderTest,TokenBudget*Test'`。
