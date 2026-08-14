---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

buzhou-observability 的生产级收口范围：异步管线接 Spring 生命周期（bean + destroyMethod）、全模块日志基线（safeStore/dispatchToSinks 吞异常点）、adviseStream 补 doOnCancel 防 span 泄漏、ObservabilityConfig fail-fast 校验、指标家族与 core BuzhouMetricsBinder 收敛（双轨 → 单一预注册口径）、注入快照 evidence/spill 空壳与死代码处置、MicrometerDualWriter 测试补齐。哪些进本轮、哪些注记不做？

## Resolution

全部进本轮（采纳 T69 §2）：
1. AsyncObservabilityPipeline 经 AutoConfiguration 暴露为 bean（destroyMethod=close），移除 JVM shutdown hook 依赖（保留为非装配路径兜底）；SynchronousObservabilityPipeline 的 doEnqueue 异常语义与 async 对齐（吞+计数+日志，不上抛主链路）。
2. 日志基线：safeStore 失败 WARN（含 store 异常类型）、dispatchToSinks 单 sink 失败 WARN（限频）、advisor 层 ERROR span 记录 DEBUG。
3. adviseStream 补 doOnCancel/doFinally——取消路径 MODEL_CALL span 终态 CANCELLED，杜绝 RUNNING 孤儿。
4. ObservabilityConfig fail-fast 校验（batchSize>=1、flushInterval/flushTimeout 非负非空、maxChildren>=0），迁 @ConfigurationProperties + JSR-303。
5. **指标家族收敛**：删除 MicrometerDualWriter 平行家族（buzhou.model.call.duration/buzhou.tool.call.duration/observability.* ），改经 core BuzhouMetricsBinder 预注册单一口径（保留 queue.wait/persist.errors 语义并入 core 家族新增两个 meter 定义）；ObservableToolCallback 的 duration 记录改走 core 家族 timer。
6. 注入快照 evidence/spill 空壳：本轮实现最小可用（从 ToolResponseMessage 文本提取 evidence id 模式），死代码（recordEvent 空方法/EMPTY）删除；OpenAI 判定改 provider 显式配置键（默认 auto 保留启发式但可覆盖）。
7. 补测试：MicrometerDualWriter→core 家族断言、stream 取消终态、sink 失败隔离、配置非法值 fail-fast、pipeline 生命周期（context close 后排空）。（可推翻）
