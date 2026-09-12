---
id: T893
title: 持久档验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T892
created: 2026-09-12
---

## Question

档位语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（ExportBundleDurabilityTest 2/2 + core 全模块零回归）：

- FILE_AND_DIR：force 完成后 zip 结构完整（manifest 首条 + 内容条目可读回）。
- NONE / null：既有行为照常产出。
