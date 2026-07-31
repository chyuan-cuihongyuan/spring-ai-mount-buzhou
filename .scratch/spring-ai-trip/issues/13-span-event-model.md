# Span + Event 数据模型与思维链捕获

Type: grilling
Status: open
Blocked by: 02

## Question

认知可观测的数据模型：Span 的种类（Session/Turn/ModelCall/ToolCall）与字段（起止、属性、状态）；Event 的种类（Thinking/FinalReply/ToolInput/ToolOutput/Error）与字段；树形关系的存储 Schema（平铺 parent_id 还是嵌套文档？）。思维链捕获：各模型 reasoning 输出的统一抽象（Spring AI 是否已暴露 reasoning content——依赖 01 的结论；不暴露的模型怎么降级）？token 消耗与耗时分布记录在哪一层？与 OpenTelemetry 的互操作（复用 OTel Span 还是自建模型+导出器）？
