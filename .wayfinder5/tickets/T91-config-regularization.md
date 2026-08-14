---
Type: task
Status: open
blocked-by:
---
## Question

guard/mcp/skills/tools 四模块配置如何正规化进 @ConfigurationProperties+JSR303+元数据？现状：四模块 auto-config 手工 Environment.getProperty（BuzhouSkillsAutoConfiguration:49 等），core ConfigMaps.sub 手工绑定。决策点：properties 类命名与层级（沿用 buzhou.<mod>.* 键不变，向后兼容）、@DefaultValue 处理、元数据 jar 断言扩展、迁移期旧键废弃告警。产出 spec 21 + impl 66 + 里程碑全仓 verify。
