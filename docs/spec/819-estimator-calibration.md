# 819 — Token 估算校准偏差审计

> 来源：H 会话第 20 轮 = effort #819 / [T1139](../../.wayfinder/tickets/T1139-estimator-calibration.md) / [T1140](../../.wayfinder/tickets/T1140-estimator-calibration-verify.md) / impl 572。
> 换题注记：原 R20 PII 置信分布不成立（正则检测无分数维）——换入校准题。

## Problem

预算闸按启发式估算、账单按模型真实 usage：估算偏低→预算提前顶爆、偏高→预算虚松浪费。「估算差多少」无量化面。

## Solution

`EstimatorCalibrationAudit`（core.spi，纯记账）：

- **成对入账**：record(estimated, actual)——相对误差 (est−act)/actual；正=高估、负=低估（口径 javadoc 声明）。
- **累计**：meanRelativeError / biasOverRatio / biasUnderRatio。
- **近窗**：128 对 |误差| 最近秩 P95（近期行为灵敏）。
- **防御**：actual≤0/负值忽略；空真全 0。

## 兼容性

纯新增静态工具（喂点=afterModel usage 回报处装配侧接）。

## 诚实边界

事后审计不改估算器；多模型混布需分桶喂；粒度受启发式本征限制。
