# Spec 26 — fork 证据归属与生命周期（引用计数共享）

> effort #6（T105 / impl-80）。延续 spec 20 §会话 fork 的已知边界（「源删除级联清 spill
> 致分支引用失效」）；借鉴引用计数 GC「最后引用者关闭」语义。

## Problem Statement

fork 复制的消息携带源会话的 evidence 引用（`evidenceId`/`spillUri`，物理文件在源会话
目录下）。源会话删除级联清理 spill 时，fork 分支的引用悬垂——读回致盲，fork 的历史
完整性被静默破坏。T88 曾将其列为已知边界；生产场景（fork 探索分支长期存续）不可接受。

## Solution

**引用计数共享**：fork 复制完成时，源会话全部证据为新会话登记引用（持久化账本）；
源会话删除时被引用的证据**逻辑摘除而非物理删除**；fork 会话关闭释放引用，**最后引用者
关闭**时证据物理删除。TTL 过期清理与孤儿扫描同样被引用门控。历史悬垂（升级前 fork 的
会话）读路径返回结构化 `EVIDENCE_GONE` 提示（不炸 turn）。

## User Stories

1. As a 应用开发者, I want fork 分支长期存续时其证据不被源会话删除清掉, so that 分支历史可完整回读。
2. As a 平台运维, I want fork 关闭后引用自动释放, so that 证据不因 fork 泄漏堆积。
3. As a 平台运维, I want 账本跨重启持久, so that 重启不丢引用归属。
4. As a 模型, I want 读到已清证据时拿到结构化提示而非报错, so that 我能基于摘要重建而非中断任务。
5. As a 平台运维, I want TTL/孤儿扫描不误删被引用证据, so that 清理机制与 fork 语义一致。

## Implementation Decisions

- **归属语义：引用计数共享（不深拷贝）**——证据体积大，深拷贝成本与磁盘配额双杀；
  共享只读 + 计数释放是既有 GC 语义的存储版。
- **账本**：spill 根 `.evidence-refs.json`（uri → 引用会话集合，TreeSet 稳定序列化，
  每次变更原子重写；写频 = fork 次数非热路径）。损坏/缺失 → 空账本重建（lenient，
  降级为旧行为）。**账本只记 fork 引用，不记属主隐式引用**——否则未 link 证据的
  TTL 清理语义被破坏（属主删除的引用门控 = 「引用集非空」，无需隐式条目）。
- **fork 登记挂点**：core `RuntimeConfig` 增第 11 槽 `forkListeners`
  （`SessionForkListener`，(sourceSessionId, newSessionId) 回调）；fork 复制完成后同步
  调用，监听器异常只 WARN 不回滚（降级为无引用登记的安全态）。spill 模块
  `configure()` 贡献监听器：按 sessionId 全根扫描（目录名全局唯一约定，免 agentName
  传递）登记全部证据。
- **删除级联口径**（`DiskSpillStore.deleteBySession`）：①该会话持有的全部引用摘除，
  引用集清空的证据（属主早已删除、被本会话保留至今）**延迟物理删**即刻执行；
  ②属主目录内文件——仍有其他会话引用则保留 + WARN，否则物理删；目录空才移除。
- **TTL 过期清理**：被引用的未 link 证据不过期；引用释放后回到原语义。
- **孤儿扫描**：死亡会话目录中被存活 fork 引用的文件保留；无引用目录照扫。
- **悬垂读路径**：`readRange` 未命中返回结构化 `EVIDENCE_GONE` 提示（证据 uri + 重建
  指引），不抛异常。
- **兼容**：`RuntimeConfig` 保留 10 参既有构造（源/二进制兼容）；`SpillStore` SPI 无变化。

## Testing Decisions

- 只测外部行为：exists/readRange/deleteBySession 返回值（不窥视账本内部；账本持久性经
  新 store 实例行为验证）。
- spill 用例矩阵：①fork 引用下源删除保留 + fork 关闭延迟物理删；②无引用级联回归；
  ③TTL 被引用门控 + 释放后回原语义；④孤儿扫描引用门控；⑤账本跨实例持久。
- core 用例：fork 监听器复制后回调 + 异常吞掉不回滚（ScriptedChatModel e2e 先例）。
- 先例：`SpillSessionLifecycleTest`、`SessionForkEndToEndTest`。

## Out of Scope

- evidence 深拷贝（fork 私有副本）。
- 引用账本的跨 spill-root 共享（多 root 部署各自独立账本）。
- fork 指定 messageId 截断（沿用 T88 fog）。

## Further Notes

- 与 spec 20 §会话 fork 的关系：原「已知边界」升级为「引用计数共享语义」；
  fork 的 Summary/State 复制口径不变。
- T108（fsck）可消费 `evidenceReferrers` 做引用健康检查（已留公共观测方法）。
