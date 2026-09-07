---
Type: task
Status: closed
---
## Question

AlertRuleEngine（周期评估/for 防抖/FIRING-RECOVERED 双向通知/计数）+
BuzhouAlertProperties。

## Resolution

done（2026-09-01）：impl-335；core.health 双类 + AlertFiring record +
fail-fast 机制校验。AlertRuleEngineTest 四用例绿。
