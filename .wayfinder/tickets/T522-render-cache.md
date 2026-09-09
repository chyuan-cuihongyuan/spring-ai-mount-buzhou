---
Type: task
Status: closed
---
## Question

目录渲染缓存：内容寻址 + 命中复用 + 变更自然失效 + stats 面。

## Resolution

done（2026-08-30）：impl-295；SkillCatalogRendererImpl 接 PromptPrefixCache
（规范形键 + getOrLoad + renderCacheStats）+ 红队 3 例 + skills 全量 88 绿。
