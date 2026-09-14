# 1171 — Token 校准审计接线

**What to build:** afterModel 同点对账 + CalibrationAuditHolder 读出。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] CalibrationAuditHolder（audit/calibration/install）
- [x] TokenBudgetHook.afterModel 对账（估算器纯函数重估 vs usage.promptTokens）
- [x] 两断言 + 预算/校准 12 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest='CalibrationAuditHolderTest,TokenBudget*Test'`。
