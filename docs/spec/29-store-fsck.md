# Spec 29 — Store fsck 一致性校验（StoreFsck）

> effort #6（T108 / impl-83）。运维面对账工具；git fsck / Redis SCAN 审计语义的
> 多 store 版——只读报告 + 可选清除。

## Problem Statement

五 store + 观测长期运行后可能积累孤儿数据：孤儿摘要（会话消息已删/丢失但摘要残留）、
残留 state 键、泄漏租约（占用 spawn 容量语义）、悬挂观测记录。无对账工具意味着
这些问题只能靠肉眼逐 store 翻——生产排障不可接受。

## Solution

`StoreFsck.run(BuzhouStores[, extraSessionIds])` 静态只读校验 → `StoreIntegrityReport`
（四检测项 findings + 计数 + 样例 + renderText 人读渲染）；
`StoreFsck.repair(stores, report, RepairOptions)` 按检测项选择清除（默认全 false），
悬挂观测**永不自动清**（审计保留价值）。

## User Stories

1. As a 平台运维, I want 一条命令对账五 store, so that 孤儿数据可见而非靠翻库。
2. As a 平台运维, I want 只报不清为默认, so that 校验永远安全可跑。
3. As a 平台运维, I want 清除按检测项可选, so that 修复动作精确受控。
4. As a 审计者, I want 观测记录永不被工具清除, so that 审计链完整。
5. As a SRE, I want 人读报告可贴工单, so that 排障沟通有标准载体。

## Implementation Decisions

- **会话全集口径（诚实声明）**：观测 store `listSessionSummaries` 分页（数字游标约定，
  内存/JDBC/Redis 实现一致）+ 调用方 extras 补充——全集完整性依赖观测留痕；
  合成会话 `__buzhou.webhook__`（spec 24）不在全集内天然豁免。
- **四检测项**：orphan-summary（WARN：摘要存在但消息为空）/ state-residue（INFO：三槽
  皆空但残留 state 键）/ dangling-lease（WARN：租约存在但会话无消息——占用容量语义）/
  dangling-observability（INFO：观测留痕但数据面全空——只报不清）。
- **修复**：RepairOptions(removeOrphanSummaries/clearStateResidue/releaseDanglingLeases)
  默认全 false；执行顺序 摘要→state→租约；返回各项实际清除数。
- **spill 不在本工具面**：spill 自有治理（spec 26 引用账本 + sweepOrphans + TTL）。
- **facts 不在 v1**：属 memory 模块内部存储（fog）。
- **模块**：仅 buzhou-core（cleanup 包，与 SessionCleaner 同域）。

## Testing Decisions

- 只测外部行为：findings 计数/sessionId/修复前后 store 状态断言（真实 in-memory stores）。
- 用例矩阵：①干净库零发现 + CLEAN 渲染；②四异常各自命中互不误报（一会话可合法命中
  多项——如 仅租约+观测 会话同时命中 dangling-lease 与 dangling-observability）；
  ③extras 扩全集；④修复选择性清除 + 观测保留验证。

## Out of Scope

- spill / facts 校验（各自模块自有治理/fog）。
- 跨实例一致性（分布式对账）。
- 观测记录清理（永不自动；运维按保留策略手工）。

## Further Notes

- runbook 排查树引用：怀疑存储泄漏时先跑 fsck 只读报告再决定修复面。
- 与 T109（会话索引）的关系：SessionIndexStore 落地后全集可切索引源（fog）。
