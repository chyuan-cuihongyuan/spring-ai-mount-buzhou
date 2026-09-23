---
id: T6030
title: R 会话 R15 Bitcask 合并的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6029]
created: 2026-09-23
---

## Question

R15 合同怎么逐一验绿？（spec 4014 / effort #4014 / R15）

## Resolution

**验证通过**：BitcaskKeydirTest 五测全绿——三写覆盖死账 100/350
+ 最新偏移直读；门 0.2 真 0.5 假 + 计划 2 键 250 字节 + 执行压实
对齐键目录不动；删除并入死账 150/活账 200；空账 NaN 诚实 + 门假；
畸形五型 fail-fast。
