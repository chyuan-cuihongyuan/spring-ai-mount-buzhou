# Spec 2023 — P 会话 R24 对账轮（effort #2023，R24）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3147–T3148，impl 1574）。
> R6k 对账轮第四例（Wave 4 收口）。

## Problem Statement

Wave 4（R19–R23）新增 5 个公共类型（RecallStrengthReranker /
QosClassifier / GraceAwareFailureDetector / WaitForReadyGate /
ReplicatedCounter）未入快照；R19/R20/R22/R23 push 网络中断积压四提交。

## Solution

R6k 同款四件套：快照 1016→1021（+5 全 P 系）+ api-surface.md 五行 +
CONTEXT 915→920 + 全仓 mvn verify 三门全绿 + P 对账门核账（spec
2000–2023 廿四号四件套）+ push 补推积压。

## Further Notes

- Wave 4 特点：接线轮（R19 重排器）与组合件轮（R21 φ×豁免）模式
  首次引入——原语落地路径从「独立件」扩展到「管线接线/组合件」。
