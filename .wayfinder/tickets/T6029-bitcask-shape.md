---
id: T6029
title: R 会话 R15 Bitcask 合并的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

追加日志的死字节何时值得重写回收？（spec 4014 / effort #4014 / R15）

## Resolution

**BitcaskKeydir（core/cleanup）**：Riak bitcask——追加覆盖入死账、
删除入死账；total/dead/ratio 三账面 + shouldMerge 死比门 +
mergePlan 成本账 + applyMerge 压实（键目录不动）。纯账面模型。
