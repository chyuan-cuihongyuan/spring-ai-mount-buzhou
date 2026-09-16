# Spec 2020 — 冷启动豁免的 φ 嫌疑门（effort #2020，R21）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3141–T3142，impl 1571）。
> 借鉴：K8s startup probe + Hayashibara φ-accrual 的工程合体——spec
> 2004 × spec 2013 组合件。

## Problem Statement

φ 检测器对冷启动抖动敏感：启动期失败间隔进模型，真故障基线被拉宽
（新实例的头几次失败 = 模型毒药）；豁免窗单独用又与嫌疑度脱节——
「宽容冷启动」与「不放过真死」两套口径未合流。

## Solution

`GraceAwareFailureDetector`（core/concurrent，单对端一件，时间外注入）：

- `heartbeat(now)`：喂 φ 模型 + **首个成功即毕业**（豁免窗关闭）；
- `failure(now)`：豁免分流——窗内未毕业失败**不喂 φ**（冷启动噪声
  不进故障统计样本，豁免账在 grace 侧）；窗外/毕业后失败以心跳形态
  喂 φ（超长间隔拉高 φ）；
- `verdict(now, threshold)` 三态：φ < threshold → HEALTHY；φ ≥
  threshold 且豁免中 → GRACE_HOLD（观望不判死）；φ ≥ threshold 且
  已毕业/窗外 → CONFIRMED（判失联）；
- 透传观测：`suspicion(now)`（φ 原始读数）/ `graceStats()`（豁免账）；
- 契约：targetId 非空、graceMillis > 0、threshold > 0 fail-fast。

## User Stories

1. 作为健康作者，冷启动抖动不毒化 φ 模型——基线不被头几次失败拉宽。
2. 作为 SRE，GRACE_HOLD 态显形「高嫌疑但仍在宽容」——观望可解释。

## Testing Decisions

- 毕业后高 φ CONFIRMED（10 拍预热 + 10 均值沉默）；豁免期失败全豁
  免且 φ 零样本恒 0；窗外失败三拍建模 + 5 均值沉默 CONFIRMED；健康
  节奏 HEALTHY；首拍毕业 + 毕业后失败直接计账；主路径三态完备；
  畸形四型 fail-fast。

## Out of Scope

- 不做多对端注册表（一目标一件由调用方组合）；GRACE_HOLD 态的完整
  场景（重锚豁免 × 既有样本）归后续轮。

## Further Notes

- 组合件模式首例：两原语（φ + 豁免）语义合流胜过各自独立接线。
