---
id: T913
title: keyset 验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T912
created: 2026-09-12
---

## Question

游标语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SessionIndexKeysetTest 3/3 + JdbcSessionIndexStoreTest 4/4 含 SQL 下推用例 + 双 store 契约零回归 + core 全模块零回归）：

- 翻页稳定：页间 s3 活跃度飙升——keyset 页边界不动（offset 会跳/重）。
- 平局按 sessionId 字典序升，游标锚点边界正确。
- 编解码往返 + 非法格式 fail-fast + 七参兼容。
