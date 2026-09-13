---
id: T1277
title: 导出域三件套联动 e2e 的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 14 轮：导出域本会话已落三件（904 SessionExportAudit / 911 JCS 规范化指纹 / 912 ExportDiff）——与既有 710 协商、733 双校验和的**联动闭环**是否有验证缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 14 轮 = effort #913 / spec 913 / impl 666）：验证缺口成立——三件套各自单测绿，但「协商→审计→规范化→diff」全链编排语义未实证。落点 core.session 测试 `ExportDomainE2ETest`（五场景闭环）：① 键序漂移下 JCS 指纹不变 → 710 协商仍判 UNCHANGED（漂移不误判变更——911 价值闭环）；② 内容真变 → 规范化指纹变 → 协商 EXPORTED；③ 未知顶层字段文档：宽松导入成功 + 审计拒绝（904 与宽松路径正交）；④ 两次导出 diff identical（时戳噪声被 710/912 口径一致排除）；⑤ 内容变更后 diff 显形 + JCS 指纹联动（912×911）。纯编排验证，零生产代码变更（若发现缺陷按 G r39 先例修实现）。
