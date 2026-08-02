# 14 — Skill 体系

**What to build:** classpath 内置 Skill（META-INF/skills/*/SKILL.md+frontmatter，同目录资源）注册；清单（name+description）进系统提示词；load_skill 工具返回正文+资源清单（资源文本直返/超大走 spill）；DB 动态 Skill 数据模型+管理 API（CRUD/上架/绑定）+同名 DB 覆盖内置（仅 PUBLISHED 参与解析）；绑定关系并入 PolicyConfigProvider。

**Blocked by:** 02, 03

**Status:** ready-for-agent

- [ ] jar 内置 Skill 引依赖即得，清单出现在系统提示词
- [ ] 模型调 load_skill 拿到正文（端到端）
- [ ] DB 上架同名 Skill 覆盖内置、下架后回退内置
- [ ] spawn 拉取 (appId,agentName) 绑定清单，改绑定下次 spawn 生效
