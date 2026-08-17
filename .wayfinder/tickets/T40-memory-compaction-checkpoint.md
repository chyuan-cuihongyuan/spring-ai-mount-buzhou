---
id: T40
title: memory · 压缩前检查点与三档回滚
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

压缩出错（摘要丢关键信息/摘要模型故障）后如何回滚？事实源：Cline（66,136★：检查点与压缩解耦——**每次工具使用后** git 影子仓快照；回滚三档=Restore Files / Restore Task Only（删检查点后消息）/ 两者；压缩失败强制 basic 兜底）；Claude Code（141,341★ 注记：/compact + rewind 同思路）。

## 待定决策（研究推荐已备）

1. `CompactionCheckpoint{sessionId, seq, preWatermark, messageWindowRef}`：compact_now/增量摘要**提交前**把压缩前消息窗不可变快照按水位键存（JSON 入 PG/对象存储）——采纳（检查点时机按压缩事件而非每次工具调用，成本更低且 Buzhou 已有 evidence 持久层）。
2. 回滚三档：①仅恢复消息窗；②恢复消息窗并**撤销摘要生效**（双时序台账 valid_to 直接表达）；③连同事实台账回滚（**默认关**）——采纳。
3. 与 Cline 的差异说明：Buzhou 回滚的是「记忆制品」，文件侧恢复不属 memory 职责（spill/guard 各自管）——spec 注明。

依据：`docs/research/oss-perfect-tier23.md` §3.6（3–4 天，ROI 中高：事故恢复与可调试性）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §memory-12**（用户常设授权 2026-08-14 ratify、可推翻）。压缩提交前按水位不可变快照；三档回滚（双时序 valid_to 表达撤销摘要生效）。
