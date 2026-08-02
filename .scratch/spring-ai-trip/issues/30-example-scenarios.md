# 示例模块的场景设计

Type: grilling
Status: resolved
Blocked by: —

## Question

examples 模块用哪个业务 demo 串起所有机制（压缩/Spill/可观测/Skill/MCP 热插拔/并行工具/原子工具/Hook 护栏/HITL）：蓝本文章用的是排障会话——示例忠于蓝本还是另选场景？demo 的结构（几个会话脚本、各自演示哪些机制、是否需要 mock 外部系统如数据库/HTTP 服务）？评测脚本集（28 已定入 examples）与 demo 是同一套还是分开？

## Answer

**定案：排障会话忠于蓝本 + 单场景多脚本 + 评测独立目录。**

1. **主场景**：运维排障 Agent——长日志/大查询结果天然触发 Spill 与微压缩，改库/重启类操作天然触发 HITL 守卫，排障报告产出天然触发写侧护栏；一个场景串全部机制，忠于蓝本与文章互证。
2. **结构**：单场景多脚本——同一排障 Agent 下 3–4 个会话脚本，各侧重机制簇（记忆压缩链 / 可观测回放 / 护栏与 HITL / Skill+MCP 热插拔）；mock 数据库与 HTTP 服务（Testcontainers/WireMock 或纯 stub）。
3. **评测**：28 的评测脚本集独立目录，与 demo 分开，复用同一套 mock 设施。
