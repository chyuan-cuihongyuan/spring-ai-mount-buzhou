---
Type: task
Status: open
blocked-by:
---
## Question

多实例语义如何显式化？现状：限流桶/RunawayCounters(InMemory除外)/InMemory 审计环为单进程内存，多实例部署语义未声明。决策点：文档化清单（哪些组件单进程、多实例下的实际行为、推荐部署形态（粘性路由/独占租约））、启动告警或配置校验（检测到多实例迹象时 WARN？如 lease 配置了 jdbc/redis 而 rate-limit 用内存——提示口径）、显式声明分布式配额 out-of-scope 不变。产出 spec 23 增量 + impl 74。
