# Spring AI 最新版挂接点调研

Type: research
Status: resolved
Blocked by: —

## Question

最新稳定版 Spring AI 的 API 表面事实调研，为所有机制的挂接点设计提供事实依据。需查清：

1. 当前最新稳定版本号与 BOM/依赖坐标；对 JDK/Spring Boot 的版本要求。
2. Advisor 链：`Advisor`/`CallAdvisor`/`StreamAdvisor` 接口现状，自定义 Advisor 如何拦截模型调用与工具调用循环（`ChatClient` 的 advise 链能否包住"思考—工具调用"递归）。
3. ChatMemory：`ChatMemory`/`ChatMemoryRepository` 接口现状，`MessageWindowChatMemory` 的扩展点；读写时机（何时 load/save）。
4. 工具调用执行链：工具调用由谁执行（`ToolCallingManager`/`ToolCallback`？），能否替换或包装执行器以注入并发执行、Spill 接管、结果回注顺序控制。
5. Observation：Spring AI 的观测 API（`ChatClientObservationConvention` 等）覆盖了什么，能否挂自定义 Span/Event，思维链（reasoning content）是否在模型抽象层可得。
6. MCP：Spring AI MCP client 的现状（`spring-ai-starter-mcp-client`），工具集运行时增删的可行性。
7. 多模型厂商的思维链输出（DashScope/OpenAI o系/Claude reasoning 等）在 Spring AI 抽象中的统一程度。

产出：事实清单（带官方文档/源码链接），写入 `.scratch/spring-ai-trip/research/spring-ai-surface.md`，并将摘要与结论追加到本 ticket 的 `## Answer`。

## Answer

基线 **Spring AI 2.0.0**（2026-06 GA；BOM `spring-ai-bom:2.0.0`；JDK 17+；需 Spring Boot 4.x）。结论：①工具循环已移入 Advisor 链（`ToolCallingAdvisor` 递归 advisor，order+300），链可包住整个"思考—工具"递归，模型内部执行已删除；②`ToolCallingManager` 接口可整体替换，注入并行执行与 Spill 结果接管；③`ChatMemory`/`ChatMemoryRepository` 接口极薄，memory advisor 默认在循环外每轮读写一次，与"完结轮次"对齐；④chat_client⊃advisor⊃model/tool 三层 span 全覆盖、convention 均可替换，reasoning 无统一 API（各厂商 metadata key 不同）；⑤MCP 工具变更事件可热刷新，但运行时增删 server 无公开 API，需自建 client 注册表。全文见 `research/spring-ai-surface.md`。
