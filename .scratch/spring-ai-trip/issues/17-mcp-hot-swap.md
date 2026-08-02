# MCP 热插拔设计

Type: grilling
Status: resolved
Blocked by: 01

## Question

MCP 工具集的运行时热更新：配置驱动模型（配置中心里 MCP server 清单长什么样？开源版默认支持哪个配置中心、还是自定义 `ToolSetProvider` SPI 留空？）；差量刷新算法（比对新旧清单，只增删变化项）；引用计数 + 延迟关闭的精确语义（在途调用计数、旧连接 grace period、强制下线兜底）？与 Spring AI MCP client starter 的关系（包装、替换、还是在其连接管理层上加层）？热更新事件如何进可观测层？

## Answer

**定案：ToolSetProvider SPI + starter 之上注册表层 + 差量刷新 + 引用计数延迟关闭带强杀兜底。**

1. **配置驱动**：自定义 `ToolSetProvider` SPI 供给 MCP server 清单（名称/传输/端点/超时/(appId,agentName) 绑定）；内置 properties/yml 与 DB 实现，复用 05 PolicyConfigProvider 体系；Nacos/Apollo 留可选扩展，开源内核不绑特定配置中心。
2. **与 starter 关系**：在 Spring AI MCP client starter 连接管理层之上加 Harness 注册表层——starter 负责协议/传输，Harness 持有 client 注册表；热更新 = 注册表条目增删，不重建 starter bean。
3. **差量刷新**：比对新旧清单，仅增删变化项，未变化连接不动；刷新事件（增/删/保持明细）记 HarnessInternal span + Event 进可观测层。
4. **引用计数 + 延迟关闭**：每个在途工具调用持旧条目引用；引用归零或 grace period（可配，默认 30s）到期即关闭；兜底强制下线阈值（可配，默认 5min）到点强杀并记 Error Event；新调用一律走新清单。
