# 配置体系与策略模型

Type: grilling
Status: resolved
Blocked by: 03

## Question

统一配置模型怎么设计：每个机制独立开关（safe by default，默认开哪些？）；按工具粒度的策略（压缩策略、Spill 阈值、永不压缩清单）与按 `(appId, agentName)` 粒度的绑定如何表达？`application.yml` 的 schema 长什么样？配置中心（Nacos/Apollo/QConfig）驱动的运行时变更是内置抽象还是留给业务？配置优先级（默认 < 全局 < agent 级 < 工具级）如何叠加？

## Answer

**定案：四层覆盖 + 动态配置 SPI + 工具声明默认/配置覆盖 + 安全项全开。**

1. **四层覆盖**：框架默认 < `application.yml` 全局 < `(appId, agentName)` 绑定级 < 工具级策略，逐层覆盖。绑定级存持久层（与 DB Skill 同源），后台改配、下次 spawn 生效。
2. **yml schema**：统一 `buzhou.*` 命名空间，按机制分节（`buzhou.memory.*`、`buzhou.spill.*`、`buzhou.observability.*`、`buzhou.guard.*` 等），每节带 `enabled`。
3. **运行时变更通道**：core 定义 `PolicyConfigProvider` SPI + 变更监听；内置 properties（静态）与持久层（DB 后台上改）两个实现；Nacos/Apollo 适配留作**可选扩展模块**（模块清单之外的 community-extension，不动 03 的 12 模块主干）。MCP 热插拔与绑定级策略都经此通道收变更。
4. **工具级策略**：工具作者经注解/接口声明默认策略（内置原子工具自带，如写操作永不压缩）；配置侧用精确名 + 通配符匹配覆盖（`buzhou.tool-policies`）。
5. **默认开关集**：微压缩、Spill、悬空修复、并行执行、观测采集、classpath Skill 默认开；LLM 摘要默认开（未配摘要模型时优雅降级）；HITL 默认开但危险工具清单默认为空（无拦截即不生效）；dashboard、DB Skill 默认关（需外部依赖）。对齐蓝本「默认安全，业务无感」。
