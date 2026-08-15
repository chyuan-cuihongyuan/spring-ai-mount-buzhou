---
Type: task
Status: open
---
## Question

dashboard 消费会话索引（fog）：listSessions 走观测留痕（无过滤、无状态面）。查询服务升级口径？

## Resolution

AFK 自决：DashboardQueryService 增 `listSessionsFiltered(appId, status, tagKey, tagValue, cursor, size)`——有 SessionIndexStore bean 时走索引（游标=offset 语义适配），无则回退观测留痕并文档降级；DashboardModule 装配注入 ObjectProvider<SessionIndexStore>。前端展示仍 out-of-scope。产 spec 36 §B + impl-97。
