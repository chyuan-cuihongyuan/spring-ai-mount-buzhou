# 11 — 可观测采集

**What to build:** Span（Session/Turn/ModelCall/ToolCall+HarnessInternal）/Event（Thinking/FinalReply/ToolInput/ToolOutput/Error+开放枚举）模型与 ObservabilityStore（平铺 parent_id，含注入快照表）落地；advisor(+400)+ToolCallback 包装采集、显式上下文传递、并发归属不串味；思维链厂商适配表（reasoningContent/thinking/thinking_content/Anthropic 块/Google thoughts，OpenAI 降级标记）；异步批量落库无采样+关闭强制 flush；token/耗时 Span 属性+Micrometer 双写；每轮注入快照落库。

**Blocked by:** 04, 03

**Status:** resolved

- [x] 一次含工具并行的会话产出完整 Span 树（归属正确）有断言
- [x] 思维链按厂商 key 适配采集、OpenAI 官方降级为计数+标记
- [x] 关闭会话强制 flush，事件不丢；队列背压不丢事件
- [x] 注入快照可按轮次还原「模型实际所见」

## Answer

buzhou-observability 模块落地（Span/Event 认知可观测采集）：

**模型与 SPI**：core.observability 包新增 SpanKind/SpanStatus/EventType/SpanContext/SpanHandle/SpanRecorder 公开 API；复用既有 `SpanRecord/EventRecord/InjectionSnapshot` SPI 记录（新增 `SnapshotMessage` record 承载消息正文/占位符/evidence-id/spill 句柄）。ObservabilityStore 读写两侧完整（saveSpans upsert 语义：RUNNING 开时写、终态关时覆盖）。

**采集挂接**：
- `ObservabilityAdvisor`（循环内 order +500，介于 memory +400 与 hook +600）：开/关 ModelCall span、采 usage/finish_reason/思维链/最终回复、每轮注入快照落库。
- `ObservableToolCallback`：开/关 TOOL_CALL span、发 TOOL_INPUT/TOOL_OUTPUT Event；`tool.parallel.index` 辅助区分同轮并发。
- `SpanContextCarrier`：会话作用域显式载体（非 ThreadLocal，虚拟线程抗串味），`HarnessToolCallingManager` 构造 ToolContext 时写入，ToolCallback 包装层从中取 parent SpanContext；并发 fan-out 任务捕获的是快照（record 不可变），同轮并发工具各开 TOOL_CALL span 且 parent 均正确指向所属 TURN/MODEL_CALL。

**思维链厂商适配表**（`ThinkingChainExtractor`）：reasoningContent（OpenAI 兼容/DeepSeek/vLLM/Ollama OpenAI 端点）、thinking（Ollama 原生）、thinking_content+reference_thinking_content 合并（Mistral）、thoughts（Google GenAI）、reasoning_signature（Anthropic）；官方 OpenAI（GPT-5/o1/o3）固定降级 `thinking.available=PROVIDER_NOT_RETURNED`，其他厂商缺 key 记 `thinking.available=NO`；超长（默认 32768 字符）截断 + `truncated=true` + 记原始长度。

**异步落库管线**（`AsyncObservabilityPipeline`）：有界队列（默认 10000）+ 后台虚拟线程批量 drain（批 200 / flush 间隔 1s）；队列满时 `put` 阻塞 = 背压不丢（写等待记 `buzhou.observability.queue.wait`）；会话 close（`SessionObserver.onClose`）与 JVM shutdown hook 强制 flush；写库异常捕获记日志不抛（`buzhou.observability.persist.errors`）。

**注入快照**：每轮构建完成时落库 `InjectionSnapshot`（sessionId + turnSeq + 消息序列 `SnapshotMessage` + 预算明细 + 策略版本），后台可按轮还原"模型当时实际看到什么"。

**TURN span 完结**：`SessionObserver.onTurnEnd` 回调关闭 TURN span，聚合本轮 `usage.*`/`iteration.count`/`turn.completed=true`。

**Micrometer 双写**（`MicrometerDualWriter`）：`buzhou.model.call.duration`/`buzhou.tool.call.duration` Timer、`buzhou.tokens` Counter（prompt/completion）、`buzhou.observability.queue.wait`/`buzhou.observability.persist.errors`。

**核心改动（公共 API 变更）**：
- `RuntimeConfig` 新增 `assemblyCustomizers` 字段（record 组件扩展，pre-1.0 语义兼容）。
- core 新增 `SessionAssemblyCustomizer/SessionAssemblyContext/SessionObserver` SPI：机制模块经此注入 advisor/工具包装/会话生命周期钩子，保持 core ← 机制模块单向依赖。
- `DefaultAgentSession` 构造函数新增 `observers` 参数（session 生命周期钩子）。

**验收**：四项 checklist 由 `ObservabilityEndToEndTest` 端到端覆盖（并发工具 Span 树归属、reasoningContent 思维链采集、OpenAI 降级标记、注入快照按轮还原）；`AsyncObservabilityPipelineTest` 覆盖批量/间隔/flush/close flush/背压阻塞/写库异常；`ThinkingChainExtractorTest` 覆盖厂商适配表 + 超长截断。全量 mvn test 通过（core 57 / memory 23 / spill 59 / observability 23）。

**推演偏离（记入 Comments）**：
- advisor order：spec 定 +400，与 BuzhouMemoryAdvisor 冲突，用 +500（语义不变：memory 之后、hook 之前）。
- 思维链超长：spec 定走 Spill 管道，本票简化走截断 + `truncated=true`（避免 observability→spill 反向依赖；后续接 Spill 管道时升级）。
- OTel 导出桥（ticket 18）、开发者控制台（ticket 17）不在本票。
- `flush()` 超时降级为调用方线程直 drain（JDBC 实现需线程安全；当前内存实现安全，JDBC/Redis 实现按同一 SPI 契约测试保证）。
