---
id: T1273
title: JCS 规范化内容指纹的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 12 轮：contentFingerprint（spec 710）读 LinkedHashMap 保序——嵌套 Map 键序漂移（跨实现/跨语言生成）会让同内容指纹不同。规范化指纹（JCS RFC 8785 思想）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 12 轮 = effort #911 / spec 911 / impl 664）：缺口成立——指纹应绑定「内容」而非「序列化键序」。落点 `SessionExportChecksum.canonicalContentFingerprint(SessionExport)`：内容投影（同 spec 710 剔除 exportedAtEpochMs）后**递归排序全部 Map 键**（TreeMap 深度规范化，值域 List/Map 递归处理；数字按 Jackson 原样文本——不做 RFC 8785 数字规范化的完整实现，诚实入档：Java 生态内消费场景数字文本已稳定）再 sha256。前缀复用 `sha256-c:` 但新方法名显式区分？——不：**新前缀 `sha256-j:`**（规范化指纹与既有保序指纹可并存且不可混用——710 前缀区分纪律延续）。既有 contentFingerprint 逐字节不动（既有协商状态兼容）。
