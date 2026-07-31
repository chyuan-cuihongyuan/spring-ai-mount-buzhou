# Skill 体系设计

Type: grilling
Status: open
Blocked by: 01

## Question

Skill 作为一等公民的完整模型：内置 Skill 的 classpath 格式（`META-INF/skills/*/SKILL.md`？frontmatter 约定？）；DB 动态 Skill 的数据模型与"后台上架/编辑/绑定"的接口边界（开源版给管理 API 还是连后台都做？）；同名 DB 覆盖内置的解析顺序；清单注入形态（name+description 进系统提示词？）与正文加载工具的设计；`(appId, agentName)` 绑定关系的数据归属（持久化 SPI 的一部分？）；Skill 内引用资源（脚本、模板）的处理。
