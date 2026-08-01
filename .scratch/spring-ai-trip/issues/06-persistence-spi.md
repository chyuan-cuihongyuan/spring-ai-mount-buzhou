# 持久化 SPI 与默认实现选型

Type: grilling
Status: resolved
Blocked by: 01

## Question

记忆/摘要/evidence 的持久化抽象怎么定：消息只追加落库、摘要版本化、evidence 指针回查，三者是一个 SPI 还是分开？默认实现给什么（JDBC？Redis？内存？）——开源项目不能绑定企业内部存储。跨实例会话续接对接口语义的要求（无本地状态、任意实例可加载）。与 Spring AI `ChatMemoryRepository` 是适配还是另立？事务与并发写（多实例同 sessionId）怎么处理？

> 范围追加（ticket 04 决议）：持久化 SPI 还需覆盖**会话 state**（Hook→state→Attachment 闭环的事实、HITL 授权标记）与**会话租约**（同一会话单活跃实例的互斥原语，含 steal 夺权）。

## Answer

**定案：四 SPI 切分 + 三实现首发 + 自研消息模型 + 完整事务。**

1. **SPI 切分**（按关注点，一个实现类可实现多个）：
   - `MessageStore` — 消息只追加落库、按会话读取（写侧零加工，读侧供压缩视图）。
   - `SummaryStore` — 摘要版本化存储 + evidence 指针回查（evidence-id → 原始工具返回）。
   - `SessionStateStore` — 会话 state：联动闭环事实、HITL 授权标记。
   - `SessionLeaseStore` — 会话租约：单活跃实例互斥、steal 夺权、过期回收。
2. **首发三实现**（对 03 模块清单的增补）：
   - 内存实现：core 内置，默认；跑通 demo 用，文档明确警告非持久/不可跨实例。
   - `buzhou-store-jdbc`（扩展模块）：生产主推，纯 spring-jdbc，MySQL/PostgreSQL。
   - `buzhou-store-redis`（扩展模块）：轻量 KV 部署场景；Redis 语义（过期策略、持久化、事务用 Lua/MULTI 原子批）在 Spec 中专项设计。
3. **消息模型**：自研持久化消息模型，完整保真（user / assistant 含 tool_calls / ToolResponseMessage / 思维链 / 附件元数据）——审计、回放、evidence 回查、微压缩判定全依赖它。对外提供 **ChatMemory 适配器**：实现 Spring AI `ChatMemory` 接口挂进官方 Advisor 链，`get` 返回压缩视图；不复用 `ChatMemoryRepository`（官方实现多数不支持工具中间消息）。
4. **事务**：存储层提供**完整事务**——SPI 暴露 unit-of-work 边界（如 `<T> T executeInTransaction(Supplier<T>)`），「一轮消息 + state 变更 + 摘要回写」作为原子提交，可回滚。实现映射：JDBC 用本地事务；Redis 用 Lua/MULTI 原子批；内存用会话级锁。会话租约仍是跨实例互斥的第一道，事务保证单实例内多写操作的原子性。
5. **跨实例续接**：四 SPI 全部无本地状态语义，任意实例凭 sessionId 可完整加载（历史 + 摘要 + state + 租约）。
