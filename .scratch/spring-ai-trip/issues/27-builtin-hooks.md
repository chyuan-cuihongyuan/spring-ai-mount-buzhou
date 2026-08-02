# 内置通用 Hook 清单

Type: grilling
Status: resolved
Blocked by: 23

## Question

框架自带哪些通用 Hook（DECO 全景里的业务 Hook——血缘 offload、文件树事件、发布条目收集——不进框架，但其通用对应物要评估）：候选包括工具调用日志 Hook（入参/出参/耗时/成功率，与可观测层的关系）、对话持久化 Hook（落库 USER/MODEL/TOOL——是否就是记忆写入路径本身）、工具返回截断器（超阈值 Rerank 重排保留相关片段——与 Spill 的关系）、响应格式化、取消响应（beforeModel 检查用户取消）。每个候选：进框架内置 / 降为示例 / 不做；内置 Hook 与 Harness 核心机制的边界怎么划（哪些其实是核心机制而非可选 Hook）？

## Answer

**定案：六核心 Hook 内置默认开 + 取消响应可选 + 两个降示例 + 持久化不 Hook 化；危险清单=三件套默认、MCP 自配。**

1. **处置表**：
   - **内置核心机制 Hook**（默认开、yml 可禁用，order 0–999 预留区间）：Spill offload（24）、写侧 Onload（24）、副本分离拦截（24）、HITL 危险工具守卫（25）、FactCollector（26）、可观测采集（14）。
   - **内置通用可选 Hook**：取消响应 Hook（beforeModel 检查用户取消标记，衔接 04 cancel()）。
   - **降为示例**（examples 模块，不进内核）：工具返回 Rerank 截断器重排、响应格式化 Hook。
   - **不做 Hook 化**：对话持久化——它就是记忆写入路径本身（06 unit-of-work），属框架内核而非可选 Hook。
2. **边界原则**：跨机制、横切、业务可能想替换的 → Hook；数据通路主干（记忆读写、预算计算、视图构建）→ 内核。
3. **危险工具默认清单**：19 三件套（write_file / run_command / http_request 写方法）默认挂守卫；MCP 工具默认不标危险，业务按 05 通配自配；文档给行业常见扩展示例（发版/删库/支付类命名启发式）但不进默认。
