---
id: T1289
title: 加密导出×审计×指纹联动 e2e 的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 20 轮：EncryptedSessionExport（spec 510）与明文域三件（710 校验和 / 904 审计 / 911 规范化指纹）的联动语义是否有验证缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 20 轮 = effort #919 / spec 919 / impl 672）：缺口成立——密文/明文两形态的路由纪律与往返语义无联动实证。落点 `EncryptedExportE2ETest`（core.session 测试域，纯编排）：① 密文载荷进明文审计 fail-fast（IllegalArgumentException——密文不是明文 JSON，fail-closed 正确行为固化）；② seal→open→audit/strict 全链：解封产物审计通过 + 严格导入通过（904×510 咬合）；③ 同内容两次 seal 密文不同（nonce）但 open 后规范化指纹相同（911×510：密文形态变、内容指纹稳定）；④ 既有 seal/open 零回归。零生产代码变更（若实证出缺陷按先例修复）。
