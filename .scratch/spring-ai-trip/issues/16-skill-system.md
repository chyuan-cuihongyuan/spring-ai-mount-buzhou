# Skill 体系设计

Type: grilling
Status: resolved
Blocked by: 01

## Question

Skill 作为一等公民的完整模型：内置 Skill 的 classpath 格式（`META-INF/skills/*/SKILL.md`？frontmatter 约定？）；DB 动态 Skill 的数据模型与"后台上架/编辑/绑定"的接口边界（开源版给管理 API 还是连后台都做？）；同名 DB 覆盖内置的解析顺序；清单注入形态（name+description 进系统提示词？）与正文加载工具的设计；`(appId, agentName)` 绑定关系的数据归属（持久化 SPI 的一部分？）；Skill 内引用资源（脚本、模板）的处理。

## Answer

**定案：classpath SKILL.md + frontmatter + DB 覆盖内置 + 清单入系统提示词 + load_skill 工具 + 绑定并入动态配置。**

1. **内置格式**：`META-INF/skills/<name>/SKILL.md` + YAML frontmatter（name/description/allowed-tools），同目录可放脚本/模板资源；与 Claude Code 生态对齐，移植成本低。
2. **DB 动态 Skill**：数据模型含 name/description/正文/资源/状态/绑定关系；开源版提供**管理 API（CRUD/上架/绑定）+ 并在 buzhou-observe-dashboard 加 Skill 管理页**。解析顺序：**同名 DB 覆盖内置**，内置为兜底。
3. **注入与加载**：清单（name + description）进系统提示词；内置原子工具 `load_skill(name)` 返回正文 + 资源清单，模型按需调用——与「上下文只放清单」原则一致，工具形态与 read_range 同构。Skill 内引用资源以相对路径标识，资源内容按需读取（文本直返，超大走 spill 管道）。
4. **绑定归属**：`(appId, agentName) → skill 清单` 绑定并入 05 的 PolicyConfigProvider 动态配置体系（绑定本质是配置），不新增 SPI。

### 影响面

- buzhou-observe-dashboard 职责扩大：观测后台 + Skill 管理页（15 的「开发调试工具」定位扩为「开发者控制台」）。
- 内置原子工具清单（ticket 19）增补：`load_skill`。
