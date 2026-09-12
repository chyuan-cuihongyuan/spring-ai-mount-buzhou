---
id: T885
title: 技能漂移看门狗验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T884
created: 2026-09-12
---

## Question

看门狗语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SkillCatalogDriftWatcherTest 2/2 + SkillCatalogFingerprintTest 4/4 修正后；skills 全模块零回归）：

- 首拍建基线（无事件、摘要 64 hex）；同目录再查静默；漂移（改/删/增）事件载荷三分类+新旧摘要；基线推进不重放旧闻。
- 空 emitter / null 清单安全。
