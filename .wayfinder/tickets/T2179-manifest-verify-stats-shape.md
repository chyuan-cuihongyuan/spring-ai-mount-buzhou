---
id: T2179
title: 导出清单校验统计读面（ExportManifestVerifyStats）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 46 轮（1439 补位轮）：清单校验遥测面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：ExportManifest.verify 纯函数零计数——校验频次与失败分布无遥测面。

形状裁决：ExportManifestVerifyStats 静态面——三受踪包装（verifyCanonical/verifySubset/verify 委托+入账 entryKind）+漏斗 verifies=ok+failed 守恒+三明细桶（非互斥可并存）+Snapshot/resetForTest；ExportManifest 本体零改动（包装比侵入纯函数干净）。

Out of scope：校验耗时；调度；条目历史。
