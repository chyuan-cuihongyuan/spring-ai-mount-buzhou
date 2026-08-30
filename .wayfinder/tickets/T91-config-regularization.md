---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

guard/mcp/skills/tools 四模块配置如何正规化进 @ConfigurationProperties+JSR303+元数据？现状：四模块 auto-config 手工 Environment.getProperty（BuzhouSkillsAutoConfiguration:49 等），core ConfigMaps.sub 手工绑定。决策点：properties 类命名与层级（沿用 buzhou.<mod>.* 键不变，向后兼容）、@DefaultValue 处理、元数据 jar 断言扩展、迁移期旧键废弃告警。产出 spec 21 + impl 66 + 里程碑全仓 verify。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **范围重划（基于实现现实）**：四模块的 `ConfigMaps.sub → Module.fromYml(map)` 是**模块自有配置契约**（模块内 Builder 解析、编程式路径共用）——改写为 @ConfigurationProperties 需重写各模块 Builder 解析层，外部价值（键不变、元数据已有 processor 覆盖 map 形态有限）低于风险。**by-design 保留**，spec 21 文档化（含元数据覆盖缺口声明：map 形态键不出 IDE 提示，docs/config-reference 补全）。
2. **真正正规化的缺口**：auto-config 层**散键直读**——skills `buzhou.skills.db-enabled`、mcp `buzhou.mcp.dangerous-tool-patterns / shutdown-budget`。转 `BuzhouSkillsProperties` / `BuzhouMcpProperties` record（@ConfigurationProperties + @Validated + compact 归一 + fail-fast），auto-config 改注入 properties。
3. **guard/tools**：无散键直读（guard 走 GuardAuditConfig.fromGuardMap 结构化解析、tools 走 fromYml map）——无需改，文档确认。
4. **@DefaultValue 不引入**（compact ctor 归一已是本仓范式）；旧键废弃告警不适用（键零变化）。
5. **里程碑**：本轮跑全仓 `mvn clean verify`（11 轮增量后的第一个全量回归点）。
