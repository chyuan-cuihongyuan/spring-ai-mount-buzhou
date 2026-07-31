# Maven 多模块划分与依赖方向

Type: grilling
Status: open
Blocked by: 01

## Question

多模块 Maven 结构怎么切：每个机制独立可引入（用户硬要求），core、memory、spill、observability、skills（含 MCP 热插拔）、tools、dashboard、starter、BOM、示例模块之间如何划分与依赖？共用内核（会话模型、持久化 SPI、策略模型、token 估算）放哪个模块？模块间的循环依赖如何避免（如 spill 依赖 observability 挂 Span，observability 又需感知 spill）？bom 与 starter 的命名（结合 groupId）。
