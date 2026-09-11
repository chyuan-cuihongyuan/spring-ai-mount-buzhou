---
id: T877
title: 压缩档位验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T876
created: 2026-09-12
---

## Question

档位语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（GzipCompressionLevelTest 3/3 + core 全模块 1761/1761 零回归）：

- 9 档产物 ≤ 1 档（高压缩不劣化体积）；两档解压内容逐字节一致。
- 缺省重载产物可解压（既有行为兼容）。
- -2 / 10 越界拒绝；-1 合法缺省。
