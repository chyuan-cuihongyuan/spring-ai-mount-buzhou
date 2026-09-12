# 742 — 静默标记×健康段联动补验

> 来源：G 会话第 42 轮 = effort #742（spec 720/85 联动补验）/ [T1033](../../.wayfinder/tickets/T1033-mute-health-e2e-shape.md) / [T1034](../../.wayfinder/tickets/T1034-mute-health-e2e-verify.md) / impl 544。

## 背景

ErrorSignatures.mute（spec 720）与 ErrorSignaturesHealth（spec 85，details 走 top()）分属两轮——静默自动传导至健康段的语义未显式闭环。

## 目标（测试域补验轮）

- mute 签名 → 健康段 details 不再出现（top 排除自动传导）；
- snapshot 原样含 muted（原始事实可查）；
- unmute → 健康段回归。

## 兼容性

纯测试域增量。
