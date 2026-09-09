---
Type: task
Status: closed
---
## Question

SequenceFence 纪元感知判定矩阵（RESET 显式化 / STALE 旧纪元 / 旧路径兼容）
+ 端到端重启纪元递增回归。

## Resolution

done（2026-09-01）：impl-326；三参 observe 重载 + STALE 裁决。
SequenceFenceEpochTest 五象限 + WebhookForwarderEpochTest 重启 E2>E1 全绿。
