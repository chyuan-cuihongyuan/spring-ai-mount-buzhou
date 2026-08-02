# Span + Event 数据模型与思维链捕获

Type: grilling
Status: resolved
Blocked by: 02

## Question

认知可观测的数据模型：Span 的种类（Session/Turn/ModelCall/ToolCall）与字段（起止、属性、状态）；Event 的种类（Thinking/FinalReply/ToolInput/ToolOutput/Error）与字段；树形关系的存储 Schema（平铺 parent_id 还是嵌套文档？）。思维链捕获：各模型 reasoning 输出的统一抽象（Spring AI 是否已暴露 reasoning content——依赖 01 的结论；不暴露的模型怎么降级）？token 消耗与耗时分布记录在哪一层？与 OpenTelemetry 的互操作（复用 OTel Span 还是自建模型+导出器）？

## Answer

**定案：自建认知模型 + OTel 导出桥 + 平铺 parent_id + 厂商适配表思维链 + Span 属性与指标双写。**

1. **模型归属**：Harness 自建认知 Span/Event 模型（认知语义一等公民），不被 OTel 语义限死；新增可选模块 `buzhou-observe-otel` 作导出桥，把四类 Span 映射为 OTel span 导出，保留运维互通。
   - Span 种类：`Session / Turn / ModelCall / ToolCall`；字段：id、parent_id、起止时间、状态、属性袋（attributes）。
   - Event 种类核心集：`Thinking / FinalReply / ToolInput / ToolOutput / Error`；枚举开放可扩展——框架事件（悬空修复、HITL 请求/授权、护栏动作等）挂入同一模型（衔接 ticket 10、25）。
2. **存储 Schema**：Span/Event 平铺两张表；span 带 `parent_id + session_id + turn_seq` 索引；查询按会话拉全量后内存组树。纳入持久化 SPI 家族——新增 **`ObservabilityStore` SPI**（ticket 06 四 SPI 扩为五 SPI），内存/JDBC/Redis 三实现同步首发。
3. **思维链捕获**：内置厂商适配表——`reasoningContent`（DeepSeek/vLLM/Ollama-OpenAI 端点）、`thinking`（Ollama 原生）、`thinking_content`/`reference_thinking_content`（Mistral）、Anthropic thinking 块（含 signature）、Google thoughts——统一成 ThinkingEvent；官方 OpenAI 不返回推理文本，仅记 `reasoning_tokens` 计数 + 「厂商未返回」标记；Anthropic display=OMITTED 同理降级为元数据。适配表可配置扩展。
4. **token/耗时**：usage（prompt/completion/reasoning tokens）与耗时记为 ModelCall/ToolCall span 属性，Turn/Session span 聚合汇总；**同时双写 Micrometer 指标**（延迟直方图、token 计数）供 Prometheus 等运维面板。

### 影响面

- ticket 03 模块清单增补：`buzhou-observe-otel` 导出桥模块（14 → 15）。
- ticket 06 持久化 SPI 增补：`ObservabilityStore`（四 SPI → 五 SPI），三实现同步首发。
- ticket 14（采集挂接）的挂接点参照 01 调研结论：复用官方三层 Observation 骨架 + 自定义 advisor（循环内 +400）+ 自定义 ObservationHandler。
- ticket 15（可视化后台）的查询输入已定：按 session_id 拉平铺 Span/Event 组树。
