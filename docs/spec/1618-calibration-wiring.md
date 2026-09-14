# 1618 · Token 校准审计接线（spec 819 孤类救活）

> 来源：N 会话 R19（effort #1618 / T2387–T2388 / impl 1171）。spec 1611 普查修复
> 第六弹：EstimatorCalibrationAudit（spec 819）建成即孤——估算与真实 usage 的
> 对账挂点从未建立。

## Solution

- 接线点：`TokenBudgetHook.afterModel`——真实 usage 提取处同点对当前 prompt 重跑
  CharHeuristic 估算（估算器是纯函数，同点重估忠实校准估算器本身），
  `record(estimated, actual)` 成对入账（actual=0 忽略——替身模型不误记）。
- `CalibrationAuditHolder`：进程级单例 + calibration() 读数便捷面。
- 纯记账有界（128 对近窗），不影响预算语义。

## Testing Decisions

- `CalibrationAuditHolderTest`：对账语义（高估/低估成对、均值相对误差、偏高占比）
  + install 替换/重置。回归：TokenBudgetHookEndToEndTest 5 用例 +
  EstimatorCalibrationAuditTest 5 用例零变化。

## Out of Scope

- 校准偏差的自动补偿（估算系数自调——先让偏差可见，补偿策略需独立裁决）。
- 其余 core 读数孤类（IdleSessionMonitor/SessionQuarantine/SessionCanaryRegistry/
  LayeredPolicy/GuardExemptionRegistry——每项独立轮）。
