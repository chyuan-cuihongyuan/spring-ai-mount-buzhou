---
Type: task
Status: closed
assignee: zcode
blocked-by: T82,T84,T86,T88,T89,T90
---
## Question

生产部署指南与 ops runbook 怎么写？决策点：文档结构（docs/ops-runbook.md：故障排查树（常见症状→定位→处置）、配置调优表（各机制参数与推荐值）、容量规划（SpawnGate/限流/预算参数如何定）、升级与回滚（schema migration 注意）、告警项清单（指标→阈值→动作））、与 dashboard/actuator 的对接说明。产出 spec 23 + impl 72。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **落点** `docs/ops-runbook.md` 七节：部署形态与存储选型 / 故障排查树（10 症状→定位→处置，覆盖熔断/降级/预算/配额/失控/沙箱/MCP 漂移/webhook/租约/store 守卫）/ 配置调优表（11 高频键）/ 容量规划（并发/吞吐/成本 + 性能基线引用）/ 升级与回滚（BOM+迁移链+机制级开关灰度）/ 多实例诚实边界（单进程组件清单+粘性+steal；分布式 out-of-scope）/ 告警项清单（9 指标→阈值→动作）。
2. **全部条目锚定既有事实**（事件名/指标名/配置键均与 spec 15/16/21/22 和实现对齐，不虚构）。
3. dashboard/actuator 对接不单列节——健康经 BuzhouHealth→actuator、回放经 dashboard 已有文档（README/spec 03），runbook 引用不重复。
