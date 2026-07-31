# 可观测采集的挂接方式

Type: grilling
Status: open
Blocked by: 01, 13

## Question

Span/Event 树在哪里长出来：挂到 Spring AI 的哪个扩展点（Advisor 链？Observation API？包装 ToolCallingManager？）才能覆盖"思考—工具调用"递归的完整嵌套结构？并发工具调用下 Span 归属不串味的上下文传播方案（虚拟线程间的 context 传递）？采集的开销控制（异步落库？批量？采样？）？压缩/Spill 等 Harness 内部动作是否也产生 Span（自观测）？
