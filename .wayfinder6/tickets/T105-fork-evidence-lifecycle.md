---
Type: task
Status: closed
---
## Question

fork 后 evidence-id 归属与生命周期（effort #5 fog 毕业，T88 已知边界）：fork 复制消息含 `evidenceId`/`spillUri` 指向源会话的 spill 证据；源会话删除级联清 spill 时，fork 侧消息的引用悬垂（读取致盲/报错）。需要决策：归属语义（引用计数共享 vs fork 时深拷贝证据 vs 悬垂容错读取）、删除级联口径（引用计数>0 不物理删 vs 物理删+fork 悬垂标记）、跨 store 一致性（HandleLifecycleRegistry 是否扩为引用表）、读路径行为（悬垂时返回什么）。产出 spec 26 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **归属语义：fork 时证据引用计数共享（refcount）**——fork 复制消息时对每个 evidenceId 在 spill 侧 `acquire`（引用 +1）；不深拷贝（证据体积大，深拷贝成本与配额双杀）。HandleLifecycleRegistry 增 per-evidence 引用计数（owner set = 引用该证据的 sessionId 集合）。
2. **删除级联口径**：会话删除级联清 spill 前，逐证据查引用集合——只剔除被删会话自身的引用，集合非空则证据**保留**（物理删除改为逻辑摘除：从被删会话可见索引移除）；集合空才物理删。这是「最后引用者关闭」语义。
3. **读路径容错**：`ReadRangeTool`/HotTail 读到悬垂（历史遗留数据，升级前 fork 的会话）时返回结构化错误提示（`EVIDENCE_GONE`：证据已被清理，请让模型重新生成或读取摘要），不抛异常炸 turn。
4. **跨 store 一致性**：refcount 表落在 spill 元数据侧（DiskSpillStore 索引文件/DB spill 表），不进 core state store——契约测试沿用 spill 契约矩阵。

### 闭合细化（实现期定稿）

- 账本**只记 fork 引用、不记属主隐式引用**——属主隐式条目会破坏未 link 证据的 TTL 清理原语义；属主删除门控 = 引用集非空。
- fork 登记挂点为 core `RuntimeConfig` 第 11 槽 `forkListeners`（`SessionForkListener`），监听器异常只 WARN 不回滚 fork。
- 引用获取按 sessionId 全根扫描（会话目录名全局唯一约定），免 agentName 跨模块传递。
- spec 26 落档。
