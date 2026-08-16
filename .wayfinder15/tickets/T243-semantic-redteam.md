---
Type: task
Status: done
blocked-by: T242-semantic-wiring.md
---
## Question

红队（stub 确定性 EmbeddingModel——向量手构可控）：否定对（"是/不是"前缀嵌入相近时
框架按阈值诚实命中——机制正确性 vs 嵌入质量分离入档：框架保证阈值/分桶/边界正确，
语义判别力归嵌入模型，默认关闭 + runbook 残余风险声明）；跨模型桶隔离；参数变化
（temperature）不同桶；无 bean + enabled fail-fast；带 toolCalls 不写；嵌入异常旁路
降级不阻断主调用。
