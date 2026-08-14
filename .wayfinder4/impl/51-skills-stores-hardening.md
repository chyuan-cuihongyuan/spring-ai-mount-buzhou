# 51 — skills 持久化 store 与失败可见化

**What to build:** 生产动态技能可落 JDBC/Redis（经 SkillStore 契约测试验证）且 ToolSetSpecStore 有 JDBC 实现；装配路径定义 store bean 即启用；SKILL.md 写坏启动可见；frontmatter 支持多行；资源/正文有上限；清单查询有缓存。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] SkillStore 契约基类 + JdbcSkillStore + RedisSkillStore（沿既有 schema 迁移体系/Redis hash 模式）
- [ ] ToolSetSpecStore JDBC 实现
- [ ] AutoConfiguration 接线 ObjectProvider<SkillStore>
- [ ] 扫描失败 WARN + 启动汇总（成功/失败计数+名单 DEBUG）
- [ ] frontmatter `|`/`>` 多行 + 列表 allowed-tools
- [ ] 资源 1MB 上限/二进制跳过 WARN；load_skill 正文 512KB 上限；resolve 缓存+写路径失效；setBinding 校验存在
- [ ] SkillsProperties JSR-303 + 元数据
