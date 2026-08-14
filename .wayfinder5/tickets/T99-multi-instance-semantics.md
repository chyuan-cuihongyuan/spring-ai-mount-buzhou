---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

多实例语义如何显式化？现状：限流桶/RunawayCounters(InMemory除外)/InMemory 审计环为单进程内存，多实例部署语义未声明。决策点：文档化清单（哪些组件单进程、多实例下的实际行为、推荐部署形态（粘性路由/独占租约））、启动告警或配置校验（检测到多实例迹象时 WARN？如 lease 配置了 jdbc/redis 而 rate-limit 用内存——提示口径）、显式声明分布式配额 out-of-scope 不变。产出 spec 23 增量 + impl 74。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **文档化**：docs/ops-runbook.md §6（T97 已落）——单进程组件清单（限流桶/熔断器/日配额/InMemory 审计环/SpawnGate）、多实例实际行为（每实例独立额度）、推荐部署（粘性路由 + 租约独占 steal 接管）、分布式 out-of-scope 声明。
2. **启动告警**（本轮增量）：resilience auto-config 启动时检测「多实例迹象」——`buzhou.store.type != memory`（jdbc/redis = 跨实例共享存储 = 多实例部署信号）且配置了任一单进程机制（rate-limit / session-quota / circuit）→ **WARN 一次**，指向 runbook §6。不做配置校验拒绝（合法部署形态，只是要知情）。
3. **不做**：分布式限流/配额/熔断（out-of-scope 不变）；RunawayCounters 会话累计本就持久化在 SessionStateStore（跨实例语义正确），不在此列。
