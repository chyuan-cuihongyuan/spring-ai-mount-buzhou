# Ticket 11 — Span+Event 认知可观测采集 ✅

**Commit:** `90e610f` on `main`

## 交付内容

### 新增模块
- **`buzhou-observability`** — Span/Event 模型、采集挂接、异步落库管线、Micrometer 双写

### 核心类型（buzhou-core 公开 API）
- `SpanKind`/`SpanStatus`/`EventType`（含 `EventTypeRegistry`）
- `SpanContext` / `SpanHandle` / `SpanRecorder`
- `SpanContextCarrier`（会话作用域显式载体，虚拟线程抗串味）

### 采集挂接点
- `ObservabilityAdvisor`（循环内 +500）：ModelCall span、usage/finish_reason/思维链/最终回复、注入快照
- `ObservableToolCallback`：TOOL_CALL span + TOOL_INPUT/TOOL_OUTPUT Event
- `ObservabilitySessionState`（`SessionObserver`）：SESSION/TURN span 生命周期、TURN 完结聚合

### 思维链适配表
reasoningContent → thinking → thinking_content（Mistral 合并 reference_thinking_content）→ thoughts（Google）；官方 OpenAI 固定降级 `PROVIDER_NOT_RETURNED`；超长截断 + `truncated=true`

### 异步落库管线
有界队列（10000）+ 虚拟线程批量 drain（200/1s）；背压不丢；会话 close + JVM shutdown hook 强制 flush；写库异常不抛

### 测试
- `ObservabilityEndToEndTest`（5）：并发 Span 树归属、思维链采集、OpenAI 降级、注入快照、close flush
- `AsyncObservabilityPipelineTest`（6）：批量/间隔/flush/close flush/背压/写库异常
- `ThinkingChainExtractorTest`（12）：厂商适配表 + 超长截断

**全量 mvn test：160 tests, 0 failures**（core 57 / memory 23 / spill 59 / observability 23）

### 公共 API 变更（兼容性）
- `RuntimeConfig` 新增 `assemblyCustomizers`（record 组件扩展）
- core 新增 `SessionAssemblyCustomizer`/`SessionAssemblyContext`/`SessionObserver` SPI
- `InjectionSnapshot` 新增 `messages`/`policyVersion` 字段（兼容构造保留）

### 推演偏离（记入 ticket Comments）
- advisor order +500（spec +400 与 memory advisor 冲突）
- 思维链超长走截断（spec 走 Spill 管道，本票避免反向依赖）
- OTel/dashboard 不在本票（ticket 18/17）
