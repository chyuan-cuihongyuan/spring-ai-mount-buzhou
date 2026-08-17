---
Type: task
Status: closed
---
## Question

观测 pipeline 背压审计：AsyncObservabilityPipeline 的队列上限/丢弃计数/关闭排空语义是否已验证齐全（effort #6/#7 新事件源接入后）？

## Resolution

AFK 自决：审计 + 补测。核实 pipeline 容量配置、丢弃计数指标名、shutdown 排空预算；为 memory.compacted/索引写入失败两类新事件源补「pipeline 满时不阻断主链」断言；runbook 告警清单补丢弃指标（若缺）。产 spec 39 §B + impl-112。
