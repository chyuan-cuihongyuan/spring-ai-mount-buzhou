---
Type: task
Status: open
---
## Question

流取消原因分类计数 buzhou.stream.cancelled{reason=client|deadline|guard}（DefaultAgentSession doFinally + ObservabilityAdvisor doOnCancel 归一）；流累计时长上限可配（默认 10min，超限取消并记 reason=deadline）——修慢滴流无累计上限的注释自认边界。验证：分类计数 + 超限取消路径单测。
