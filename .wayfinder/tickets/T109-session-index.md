---
Type: task
Status: closed
---
## Question

会话枚举与元数据索引（新缺口）：五 store 均只有按 sessionId 点查，无列表/过滤；生产 ops（排障找会话、按 app/agent 统计活跃、dashboard 列表）无入口。需要决策：索引载体（新 SessionIndexStore SPI vs 复用某 store 加列表法 vs 派生扫描）、元数据字段（appId/agentName/status/lastActiveAt/tags）、写路径（spawn/heartbeat 时更新）、一致性口径（最终一致即可 vs 强一致）、内存实现与 JDBC/Redis 实现。产出 spec 30 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **载体：新 SPI `SessionIndexStore`**（独立第六 store，`BuzhouStores` 增可选组件，null=无索引降级）——不往五 store 里加列表法（各自存储模型差异大，扫描全键在 Redis/JDBC 都是反模式）；独立索引可针对查询模式设计。
2. **元数据**：`SessionInfo(sessionId, appId, agentName, status ACTIVE/CLOSED/DELETED, createdAt, lastActiveAt, messageCount, tags Map<String,String>)`；tags 供业务挂自定义检索键。
3. **写路径**：spawn 时 create、每轮 afterModel 更新 lastActiveAt/messageCount、close/delete 时更新 status——由 core 内置 hook 驱动（`SessionIndexHook`，索引存在才装配）；**最终一致**（更新失败 WARN 不阻断会话，文档明示）。
4. **实现矩阵**：内存（ConcurrentHashMap + 快照排序）、JDBC（新表 V3 迁移）、Redis（ZSET lastActiveAt + HASH 元数据）；查询 API `list(appId, agentName?, status?, tag?, offset, limit)` 按	lastActiveAt 倒序。

### 闭合细化（实现期定稿）

- **不进 BuzhouStores**（偏离原决议）：record 无可选组件语义，独立 bean 免六参 record 全仓破坏性变更；core auto-config 检测 bean 自动接线（SessionIndexObserver.wiring）。
- turnCount = 观察者进程内计数（重启从 0 续，近似值）；DELETED 状态预留（级联删除联动置 DELETED 记 fog）。
- JDBC upsert = UPDATE-then-INSERT + DuplicateKey 收敛；V3 迁移去掉状态 CHECK 约束（H2 行为异常 + MySQL 8.0.16 前不强制，方言风险不值）。
- 内存部署默认无索引（显式 bean 启用）；jdbc/redis auto-config 自动装配。
- spec 30 落档。
