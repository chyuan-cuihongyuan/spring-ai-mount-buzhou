---
id: T6115
title: S 会话 S8 Seqlock 序号锁的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

读多写少小数据怎么读者零阻塞达一致？（spec 5007 /
effort #5007 / S8）

## Resolution

**SeqLock（core/concurrent）**：Linux seqlock——序号奇偶标
写入期（偶稳定/奇写入），writeBegin/End 单写者串行，读者
readBegin/readEnd 乐观校验失败整读重试（无阻塞无读者互斥）。
