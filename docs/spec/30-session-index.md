# Spec 30 — 会话索引与枚举（SessionIndexStore）

> effort #6（T109 / impl-84）。补五 store「只可点查、不可枚举」的查询面；
> LangSmith 会话列表 / OpenHands 会话管理的运维标配。

## Problem Statement

五 store 均为按 sessionId 点查——「列出某 app 的活跃会话」「按 agent 统计」「dashboard
会话列表」「排障找会话」无入口。fsck（spec 29）的会话全集也只能借用观测留痕（依赖
观测完整性）。

## Solution

独立第六查询面 `SessionIndexStore`（SPI）：`upsert/get/list/delete`，`SessionInfo`
（appId/agentName/status/createdAt/lastActiveAt/turnCount/tags）+ `SessionIndexQuery`
（过滤 appId/agentName/status/tag + 分页，lastActiveAt 倒序）。由 `SessionIndexObserver`
在会话生命周期点（onOpen/onTurnEnd/onClose）维护，**最终一致**（更新失败只 WARN
不阻断会话；索引是查询优化面，权威数据在五 store）。未装配 = 无枚举能力，会话功能
零影响（降级语义）。

## User Stories

1. As a 平台运维, I want 按 app/agent/状态列出会话, so that 排障找会话不再翻库。
2. As a 平台运维, I want 最近活跃优先排序, so that 热点会话一眼可见。
3. As a 应用开发者, I want 业务标签挂进索引, so that 按业务维度检索会话。
4. As a SRE, I want 索引故障绝不影响会话主链, so that 查询面故障不放大为业务故障。
5. As a dashboard 消费者, I want 分页查询, so that 大会话量下列表可用。

## Implementation Decisions

- **SPI**：`SessionInfo`（状态常量 ACTIVE/CLOSED/DELETED）+ `SessionIndexQuery`
  （tagKey/tagValue 必须成对；limit≤200）+ `SessionIndexStore` 四方法。
- **维护挂点**：`SessionIndexObserver`（SessionObserver 生命周期回调）经
  `SessionAssemblyCustomizer` 注册——core auto-config 检测到 `SessionIndexStore` bean
  时自动接线（`SessionIndexObserver.wiring(store)`）；编程式用户自行并入
  RuntimeConfig.assemblyCustomizers。
- **不进 BuzhouStores**：索引是可选查询面非权威槽位——独立 bean 避免六参 record 全仓
  破坏性变更（与原决议「BuzhouStores 增可选组件」偏离，理由：record 无可选组件语义）。
- **实现矩阵**：内存（快照排序，重启重建）/ JDBC（表 `buzhou_session_index` V3 迁移，
  UPDATE-then-INSERT 跨方言 upsert + DuplicateKey 竞态收敛；tags JSON 列 LIKE 预筛 +
  内存精确复核）/ Redis（ZSET lastActive + STRING 行，ZREVRANGE 翻页 + 内存过滤；
  独立连接不占事务池）。
- **turnCount 口径**：观察者进程内计数（重启后从 0 续——近似值，文档明示）。
- **DELETED 状态**：预留（delete() 摘行；会话级联删除暂不联动索引——CLOSED 行保留
  供审计查询，fog：SessionCleaner 联动置 DELETED）。
- **装配**：store.type=jdbc / redis 自动配 SessionIndexStore bean；内存部署默认无索引
  （显式定义 bean 即启用）。

## Testing Decisions

- core e2e（ScriptedChatModel + wiring）：①生命周期演化（ACTIVE→turnCount 累计→CLOSED）；
  ②过滤与排序；③索引后端抛异常不阻断会话（最终一致）。
- jdbc（H2）：upsert 收敛 / 过滤组合 / tag 前缀邻键不误报 / delete 幂等 / 迁移版本 3。
- redis（Testcontainers redis:7-alpine，无 Docker 跳过）：upsert/过滤/tag/delete。
- 迁移测试：V1–V3 版本序列 + V3 表就位（SchemaMigrationH2Test 升级）。

## Out of Scope

- 索引强一致（权威在五 store；最终一致是定位）。
- 全文/模糊检索（tags 精确等值即可；OLAP 归观测面）。
- 索引 TTL/归档策略（CLOSED 行保留；清理策略归运维，fog）。

## Further Notes

- fsck（spec 29）会话全集未来可切索引源（fog）。
- dashboard 查询服务消费本索引属 #4 边界内已有实现域（前端工程化仍 out-of-scope）。
