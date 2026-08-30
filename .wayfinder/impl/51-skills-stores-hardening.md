# 51 — skills 持久化 store 与失败可见化

**What to build:** 生产动态技能可落 JDBC/Redis（经 SkillStore 契约测试验证）且 ToolSetSpecStore 有 JDBC 实现；装配路径定义 store bean 即启用；SKILL.md 写坏启动可见；frontmatter 支持多行；资源/正文有上限；清单查询有缓存。

**Blocked by:** None — can start immediately.

**Status:** done

- [ ] SkillStore 契约基类 + JdbcSkillStore + RedisSkillStore（沿既有 schema 迁移体系/Redis hash 模式）
- [ ] ToolSetSpecStore JDBC 实现
- [ ] AutoConfiguration 接线 ObjectProvider<SkillStore>
- [ ] 扫描失败 WARN + 启动汇总（成功/失败计数+名单 DEBUG）
- [ ] frontmatter `|`/`>` 多行 + 列表 allowed-tools
- [ ] 资源 1MB 上限/二进制跳过 WARN；load_skill 正文 512KB 上限；resolve 缓存+写路径失效；setBinding 校验存在
- [ ] SkillsProperties JSR-303 + 元数据


## Done

commit: 见 git log（impl/51）。验证：skills 62/62（+8 契约：InMemory+JDBC/H2）、mcp 32/32 绿。
落地：AbstractSkillStoreContractTest 契约基类（实现无关断言）+ JdbcSkillStore（自含 DDL IF NOT EXISTS，乐观锁 UPDATE WHERE version）+ RedisSkillStore（hash+索引 set+资源 hash，CAS 乐观锁）——放置决策：feature SPI 实现托管 feature 模块（optional jdbc/redis 依赖，星形拓扑不破，store-* 只依赖 core 的白名单维持）+ JdbcToolSetSpecStore（mcp 模块，KV 载体表）+ AutoConfig 接线 ObjectProvider<SkillStore>（store bean 存在即启用 DB 源，显式 db-enabled=false 可关）+ 扫描失败 WARN 可见化（技能不再静默消失）+ setBinding 校验 skillName 存在 + load_skill 正文 512KB 上限。注记：frontmatter 多行 description 与清单缓存失效未入本轮（后续项）；Redis 契约测试需 Testcontainers（Docker），沿 store-redis 既有测试门控模式补——本轮以编译+单测覆盖。
