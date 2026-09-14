---
id: T2166
title: 命名违规类别与首违不短路的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2165
created: 2026-09-14
---

## Question

如何证明与门规则同源及违规闭集完备？

## Resolution

**用户常设授权 AFK（可推翻）**

`MetricNameAuditTest` 六测全绿（`mvn -pl buzhou-core -am test`）：四条门测试合规样例同判合规；大写段/尾空格/双点同判违规（与门样例一致——同源验证）；外语族 PREFIX；**首违不短路**（大写段+双点+尾空格一次报全）；空名哨兵。
