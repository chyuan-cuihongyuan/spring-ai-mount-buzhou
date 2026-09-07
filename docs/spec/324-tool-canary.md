# Spec 324 — 工具金丝雀发布（effort #324）

> wayfinder map：`.wayfinder324/MAP.md`（T639–T640）。借鉴：Istio 权重
> 分流 + Flagger canary 分析回滚。

## Problem Statement

工具换新版（新模型/新实现）只能全量切或靠 shadow 对照旁路观察：全量切
出错就是生产事故，shadow 又永不上流量——缺一个"小流量真跑、劣化自动退"
的中间档。

## Solution

`CanaryToolCallback`（ToolCallback 装饰器，exec）：

- `wrap(stable, canary, weightPercent, random)`：两臂定义同名校验；
  每次调用按 weight% 独立掷（0=全稳定，100=全金丝雀）。
- per 臂成败计数（结构化错误标记语义同 131——执行/校验失败均计败）。
- **自动回滚**：canary 样本 ≥ min-samples 且 canary 错误率 − stable
  错误率 ≥ tolerance（百分点）→ rolledBack=true（此后全走 stable）。
  粘性：不再自动回升——人工重新 wrap 或调 weight。
- 观测 `View(weight, stableCalls/Errors, canaryCalls/Errors, rolledBack)`；
  回滚计数器 metric。

## User Stories

1. 作为工具作者，我想 10% 流量真跑新版、90% 走旧版，所以新实现的首次
   生产曝光可控。
2. 作为 SRE，新版错误率比旧版高 10 个点以上时我想自动全量回稳定版，
   所以劣化不用等人盯盘。
3. 作为 SRE，回滚后我想保持稳定版直到人工重新发起，所以自动回升这种
   二次抖动不存在。
4. 作为宿主开发者，我拿 View 就能看两臂样本/错误率/回滚态，所以灰度
   健康一目了然。

## Implementation Decisions

- 装饰器族无 yml 装配（169 TransformingToolCallback 先例——宿主 wrap）。
- 概率源可注入（测试确定性）。
- 回滚判定在每次调用后顺带评估（无定时——事实驱动同哲学）。

## Testing Decisions

- `CanaryToolCallbackTest`：权重分流确定性/0 与 100 边界/同名校验/
  劣化回滚粘性/未达样本不回滚/容差内不回滚/View 与计数。
- 无装配测试（装饰器无 yml）。

## Out of Scope

- 时间窗错误率；自动 promote（晋升是人的决定）；多臂灰度。

## Further Notes

- 流量治理族：shadow 对照（309 旁路）/ **金丝雀（324 小流量真跑+自动退）**。
