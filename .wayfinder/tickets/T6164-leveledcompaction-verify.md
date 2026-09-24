---
id: T6164
title: S 会话 S32 Leveled Compaction 分层压实挑选的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6163]
created: 2026-09-24
---

## Question

S32 合同怎么逐一验绿？（spec 5031 / effort #5031 / S32）

## Resolution

**验证通过**：LeveledCompactionTest 八测全绿——容量阶梯圣像；
无超容 null；满层触发最旧源+firstKey 序重叠目标；最高分赢；
并列浅层优先；末层同层；complete 出账回账；畸形 fail-fast
（含字典序 k10<k5 钉住）。
