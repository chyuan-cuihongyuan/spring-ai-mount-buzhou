---
Type: task
Status: closed
---
## Question

skills 大目录注入预算（fog）：目录线性全量注入系统提示词，技能上百后注入体积失控。评分/分页如何设计？

## Resolution

AFK 自决：注入预算而非评分（评分需查询语义——模型自会按 description 挑，缺的是体积护栏）。SkillCatalogRendererImpl 注入上限：`buzhou.skills.catalog-max-entries`（默认 50；超限截断并附「另有 N 个未列出，可用 skill_search 查询」提示——新增 SkillSearchTool 按名称/描述子串匹配列出）？**裁剪**：skill_search 工具属新能力面（fog），M1 只做截断+计数提示（模型可让运维调整绑定）。产 spec 35 §B + impl-94。
