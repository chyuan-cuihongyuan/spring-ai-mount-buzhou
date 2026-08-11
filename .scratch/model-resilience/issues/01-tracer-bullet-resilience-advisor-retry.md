# 01 — 韧性层骨架 + ResilienceAdvisor 端到端贯通（瞬时错误重试）+ 底座重试避让

**What to build:** 打通模型韧性层的最薄端到端链路（tracer bullet）：新增 `buzhou-resilience` 模块（仅依赖 buzhou-core），`ResilienceAdvisor` 经 `SessionAssemblyCustomizer` 注入 ChatClient 链最内层；模型抛瞬时（网络类）错误时按指数退避 + 抖动重试至 max-attempts；重试/耗尽事件进 observability；`buzhou.resilience.*` 配置生效；引入即把底座 `spring.ai.retry` max-attempts 调小、避让双倍重试。从用户视角：一次瞬时模型错误不再让当轮失败。

**Blocked by:** None — 可立即开始

**Status:** done

## 范围

- **模块骨架**：pom（buzhou-core + jackson-databind + spring-boot-autoconfigure；test 依赖 buzhou-core test-jar 复用 `ScriptedChatModel`）、包根 `io.github.chyuan_cuihongyuan.buzhou.resilience`、`BuzhouResilienceAutoConfiguration`（`@ConditionalOnProperty(prefix="buzhou.resilience", name="enabled", matchIfMissing=true)`，safe-by-default 默认开）、`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。
- **聚合**：`buzhou-bom` 新增版本条目、`buzhou-spring-boot-starter` 新增聚合依赖（starter 仅聚合、无代码，既有约定）。
- **ResilienceAdvisor**（`BaseAdvisor`）：经 `SessionAssemblyCustomizer` + `SessionAssemblyContext.addAdvisor` 注入（对齐 `ObservabilityModule` 既有 Advisor 注入模式）；用**显式 order** 落最内层（紧邻 `HookAdvisor`、包裹裸 ChatModel 调用）——实现期须实测确认 order 生效位并固定进文档。
- **ProviderErrorClassifier 接缝**：确立「异常 → 类别」映射的扩展点形态，本票实现最小集（NETWORK + 默认 UNKNOWN），五类全集留 02。
- **重试回路**：指数退避 + 抖动、max-attempts 上限；retryable = {NETWORK}（02 扩全）。
- **配置**：`buzhou.resilience.*`（enabled / max-attempts / initial-backoff / max-backoff / multiplier / jitter），`@ConfigurationProperties` record（boxed 类型、null=未配置，对齐 `SpillProperties` 模板）。
- **事件**：`retry-attempted`（带类别/次数/退避）、`retry-exhausted` 进会话既有事件通道（不新增 SPI）。
- **底座避让**：AutoConfiguration 把 `spring.ai.retry` max-attempts 调到最小（如 1）/文档强约束，避免与不周山重试叠加成双倍重试。

## 验收

- [ ] 引 `buzhou-resilience`（或 starter）默认配置即可让「瞬时网络错误一次后成功」的模型调用返回正确回复（不失败当轮）
- [ ] `retry-attempted` / `retry-exhausted` 事件按预期进 observability 事件流
- [ ] `buzhou.resilience.enabled=false` 时回退底座原生行为（无重试）
- [ ] max-attempts / 退避参数经 yml 可调且生效
- [ ] 底座 `spring.ai.retry` 被避让（不出现双倍重试）
- [ ] 端到端 e2e 测试通过（扩展 `ScriptedChatModel` 支持「第 N 次抛异常」），断言回复 + 事件
- [ ] `mvn verify` 全绿；模块仅依赖 buzhou-core（星形依赖白名单物理无环）

## 备注

- 管辖 Spec：`.scratch/model-resilience/spec.md`；ADR：wayfinder 03 号票（执行点=ResilienceAdvisor、错误感知走 Hook）+ 02 号票（底座 429 默认不重试、无统一超时）。
- 借鉴：LangChain v1 `ModelRetryMiddleware`（容错下沉为中间件）、LangGraph `RetryPolicy`（声明式重试、默认值含抖动）。一手链接见 `docs/production-readiness/references.md` 03 条目。
- ResilienceAdvisor order 生效位是本票也是整组的关键技术核验项（见 spec「执行点」开放项）。
