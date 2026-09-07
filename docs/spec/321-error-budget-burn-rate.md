# Spec 321 — SLO 错误预算燃尽率（effort #321）

> wayfinder map：`.wayfinder321/MAP.md`（T633–T634）。借鉴：Google SRE
> workbook 错误预算 / burn rate（alert on burn, not on raw error）。

## Problem Statement

工具错误率只有"当下反应"面（熔断跳闸 / 重试限流）：高频小错误会被熔断窗
放大成硬跳闸，低频持续错误又不够跳闸——都不到 SRE 的预算语言："以 SLO 推
算的额度正在几倍速燃尽"。缺一个把真实流量错误率换算成燃尽率的观测面，
并能接进既有告警引擎按 for 窗触发。

## Solution

`ErrorBudget`（core.health）：

- 桶环时间窗（默认 10m × 60 桶，Clock 注入）：record(scope, 成/败)；
  惰性旋转（访问时按 now 与桶宽扫过期桶）。
- errorRate(scope) = 窗内错误/窗内总量；burnRate = errorRate/(1−SLO)
  （SLO 百分比配置，如 99 → 除以 0.01）；breaching = burn ≥ 阈值（默认 2）
  **且**窗内样本 ≥ min-samples（默认 20——一败 100% 的噪声不触发）。
- scope 上限 256 折 `__overflow__`（舱拒绝计数同款诚实边界）。
- `ErrorBudgetHook`（order 250，纯观察不 block）：afterTool 记成败——
  结构化标记语义与熔断 hook 131 一致（执行失败 + 校验失败均计败）。
- `ErrorBudgetHealth`（mechanism "error-budget"）：任一 scope breaching →
  DOWN（严格 DOWN 语义 = SLO 保卫失守）；无样本 → UNKNOWN（未启用 ≠ DOWN）；
  details：slo/窗/阈值/top breaching（有界）。
- yml：`buzhou.error-budget.{slo,window,buckets,burn-rate-threshold,
  min-samples}`；slo 未配不装配。健康面自动进 312 告警引擎的机制集
  （for 窗吸收瞬态——multi-window 思想的库内等价物）。

## User Stories

1. 作为 SRE，我想看到"当前错误率在以 2 倍速烧 99% SLO 的预算"，所以
   不等熔断跳闸就能立项排查。
2. 作为 SRE，我想让燃尽超阈走既有告警规则的 for 持续窗，所以瞬态毛刺
   不告警、持续恶化必告警。
3. 作为 SRE，样本不足时我不想被"1 次失败 = 100% 错误率"骗到，所以
   min-samples 之下不判 breach。
4. 作为宿主开发者，我想要纯观察钩子零干预执行路径，所以挂上只计不拦。

## Implementation Decisions

- 桶环惰性旋转（无定时线程——记录/读取路径顺带扫过期桶，与空闲压缩
  310 的"事实驱动"同哲学）。
- burn 阈值/窗/桶数皆可配：multi-window 分层（14.4×/6×）宿主配多个
  ErrorBudget 实例表达——库不预设分级。
- DOWN 判定任一 scope breaching（top 面向人，判定不截断）。

## Testing Decisions

- `ErrorBudgetTest`：burn 计算/min-samples 门/窗旋转过期/overflow 折叠/
  构造校验（MutableClock 先例 131）。
- `ErrorBudgetHookTest`：标记记败/正常记成/beforeTool 恒 CONTINUE
  （DefaultToolCallContext 先例）。
- `ErrorBudgetAssemblyTest`：slo 配置即三 bean；未配不装配；slo=0 红；
  健康面 UNKNOWN→DOWN 迁移（喂败样本驱动）。

## Out of Scope

- multi-window 双窗联判；月度预算时间箱记账；breach 联动自动降载。

## Further Notes

- 错误三消费面齐：熔断（当下硬跳）/ 重试预算（当下软限）/ **错误预算
  （SLO 尺度的燃尽观测）**。
