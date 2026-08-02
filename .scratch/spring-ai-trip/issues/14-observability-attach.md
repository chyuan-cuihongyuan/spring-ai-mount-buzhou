# 可观测采集的挂接方式

Type: grilling
Status: resolved
Blocked by: 01, 13

## Question

Span/Event 树在哪里长出来：挂到 Spring AI 的哪个扩展点（Advisor 链？Observation API？包装 ToolCallingManager？）才能覆盖"思考—工具调用"递归的完整嵌套结构？并发工具调用下 Span 归属不串味的上下文传播方案（虚拟线程间的 context 传递）？采集的开销控制（异步落库？批量？采样？）？压缩/Spill 等 Harness 内部动作是否也产生 Span（自观测）？

## Answer

**定案：Advisor + 包装 ToolCallback 挂接 + 显式上下文传递 + 异步批量无采样 + 内部动作全产 Span。**

1. **挂接点**：自定义 advisor（循环内 +400，01 调研点位）开/关 Turn 与 ModelCall span；包装 ToolCallback（工具调用必经点）开/关 ToolCall span 并采 ToolInput/ToolOutput event；官方 ObservationHandler 仅作辅助校正。不替换 ToolCallingManager，Spring AI 升级兼容面最小。
2. **上下文传播**：Span 上下文对象作为显式参数在 Harness 内部调用链传递（advisor → ToolCallback 包装 → 并发执行器提交任务时捕获/恢复）；不用 ThreadLocal/ScopedValue，虚拟线程与并发工具调用天然不串味。
3. **开销控制**：Span/Event 先入内存队列，后台虚拟线程批量异步落库（批大小与 flush 间隔可配）；会话关闭强制 flush。**不引采样**——认知可观测丢事件会破坏排障完整性，开销靠异步+批量消化。
4. **自观测**：压缩（微压缩/摘要）、spill 落盘/回读、Hook 执行、悬空修复等 Harness 内部动作全产 Span/Event，挂在所属 Turn span 下；排障可见「框架自己干了什么、花了多久」。

### 影响面

- ticket 13 的 Span 种类增补：`HarnessInternal`（或按动作细分 Compaction/Spill/Hook/Repair），挂在 Turn 下；13 的「四类」表述在 Spec 中写为「核心四类 + Harness 内部动作 span」。
