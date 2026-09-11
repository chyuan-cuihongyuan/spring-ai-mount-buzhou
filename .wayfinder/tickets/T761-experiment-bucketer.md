---
Type: task
Status: closed
---
## Question

`ExperimentBucketer.assign` 确定性分桶（sha256 低 32 位 mod 100+字典序
累积权重）+ 未入组余量 + 未知实验 null 零状态 + 权重和 fail-fast。

## Resolution

done（2026-09-12）：impl-408；确定性/分布/未入组/越界 fail-fast 用例绿。
