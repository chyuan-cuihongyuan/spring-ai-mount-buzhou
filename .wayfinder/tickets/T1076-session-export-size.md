---
id: T1076
title: 导出体积去向审计的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

导出体积无归因面——加体积审计吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 39 轮 = effort #738 / spec 738 / impl 638）：`SessionExportSizeAudit.analyze(SessionExport)` 纯函数——按段字符归因（messages/summary/state/ext:*）chars 降序+占比守恒。字符估算口径（精确序列化可 toJson 交叉验证）；自动瘦身不做。
