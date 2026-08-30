---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

buzhou-skills 的生产级收口范围：JDBC/Redis 持久化 SkillStore（含 ToolSetSpecStore 是否一并做）、AutoConfiguration 接线 dbStore、ClasspathSkillScanner 吞错日志化+解析失败事件、frontmatter 多行 description 支持、资源读取大小上限与二进制处置、清单渲染缓存、load_skill 正文上限、setBinding 校验 skillName 存在、配置校验+元数据+日志对齐。

## Resolution

进本轮：
1. **JdbcSkillStore + RedisSkillStore**：store-jdbc/store-redis 新增 SkillStore 实现（表 skill/skill_resource/binding 沿用各 store 既有 schema 迁移体系；Redis 用 hash+索引集合），契约测试沿 AbstractBuzhouStoresContractTest 范式新增 SkillStoreContractTest 抽象基类；ToolSetSpecStore 的 JDBC 实现一并落（mcp T74 的 DB 源闭环）。
2. AutoConfiguration 接线 `ObjectProvider<SkillStore>`（db-enabled 或 store bean 存在时启用 DB 注册表源）。
3. ClasspathSkillScanner：IOException/解析失败 WARN 日志 + SkillRegistry 启动汇总（扫描 N 个、失败 N 个、失败名单 DEBUG）——技能不再静默消失。
4. frontmatter 支持多行 description（`|`/`>` 块式）与 `allowed-tools` YAML 列表式；解析器测试扩展。
5. 资源读取上限（单资源默认 1MB，超限跳过+WARN）；二进制（非 UTF-8 可解码）资源按 base64 介质存内存注记——本轮保持文本模型，二进制跳过+WARN。
6. 清单渲染缓存：resolve 结果按 (appId,agentName) 缓存，store 写操作时失效（SkillAdminApi 写路径统一 evict）。
7. load_skill 正文上限（默认 512KB，超限返回错误文本）。
8. setBinding 校验 skillName 存在；version 冲突转 ToolErrorFeedback 风格文案。
9. 配置校验+元数据+日志基线对齐（SkillsProperties JSR-303）。（可推翻）
