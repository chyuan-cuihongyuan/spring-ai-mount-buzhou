# Spec 541 — 双 judge 一致率（effort #541）

> wayfinder map：`.wayfinder/maps/effort-541.md`（T835–836）。E 会话第 41 轮。

## Problem Statement

judge 换版/双 judge 并用时**间一致率**无计量——朴素一致率被机遇一致
虚高（两 judge 都 95% 判绿 → po 虚高但毫无分辨力）。Cohen's κ 修正
机遇一致。

## Solution

`eval.JudgeAgreement`（纯函数，516 同型）：analyze(judgeA, judgeB) →
AgreementReport——混淆四格（bothRed/bothGreen/aRedOnly/bRedOnly）+
singleSided 排除 + po/pe/κ（分母 0 或 pe≥1 → null 诚实空值）+ strength
分级（Landis-Koch）。二值化判红=fail|error（516 同映射）。

## User Stories

1. 作为评测方，我想量化两个 judge 的一致性（修正机遇后）， so 换 judge
   或并跑裁决有可信度依据。

## Implementation Decisions

- κ = (po−pe)/(1−pe)；pe≥1（退化分布）→ null。
- 单侧排除（513 同口径）。

## Testing Decisions

- 完全一致 κ=1；高 po 高 pe → κ<0.1（机遇修正实证）；反转负 κ；单侧
  排除；null fail-fast。

## Out of Scope

- Fleiss κ；加权 κ。

## Further Notes

- 新公共类型 `JudgeAgreement`（嵌套 `AgreementReport`）随轮 regenerate
  快照 + api-surface.md 加行。
