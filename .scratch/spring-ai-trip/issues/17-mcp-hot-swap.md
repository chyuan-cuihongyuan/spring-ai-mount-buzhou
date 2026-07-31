# MCP 热插拔设计

Type: grilling
Status: open
Blocked by: 01

## Question

MCP 工具集的运行时热更新：配置驱动模型（配置中心里 MCP server 清单长什么样？开源版默认支持哪个配置中心、还是自定义 `ToolSetProvider` SPI 留空？）；差量刷新算法（比对新旧清单，只增删变化项）；引用计数 + 延迟关闭的精确语义（在途调用计数、旧连接 grace period、强制下线兜底）？与 Spring AI MCP client starter 的关系（包装、替换、还是在其连接管理层上加层）？热更新事件如何进可观测层？
