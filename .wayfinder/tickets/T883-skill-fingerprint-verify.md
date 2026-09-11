---
id: T883
title: 技能指纹验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T882
created: 2026-09-12
---

## Question

指纹语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SkillCatalogFingerprintTest 4/4）：

- 摘要稳定 + 输入序不敏感 + sha256 hex 长度。
- 三分类各归其类（新增/删除/描述变更）。
- allowedTools 变更计 CHANGED；自反 diff 空。
- null/空目录安全（空摘要可算、对空全 REMOVED）。
