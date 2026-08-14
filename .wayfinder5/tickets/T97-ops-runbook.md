---
Type: task
Status: open
blocked-by: T82,T84,T86,T88,T89,T90
---
## Question

生产部署指南与 ops runbook 怎么写？决策点：文档结构（docs/ops-runbook.md：故障排查树（常见症状→定位→处置）、配置调优表（各机制参数与推荐值）、容量规划（SpawnGate/限流/预算参数如何定）、升级与回滚（schema migration 注意）、告警项清单（指标→阈值→动作））、与 dashboard/actuator 的对接说明。产出 spec 23 + impl 72。
