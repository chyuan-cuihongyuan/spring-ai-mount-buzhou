# Wayfinder Map — Buzhou core/memory/spill/guard 生产级收口（effort #3）

> effort #3，延续 [effort #1](effort-01.md)（#1：Tier-1 落地）与 [effort #2](effort-02.md)（#2：Tier-2/3 全量 + 27 切片落地）。

## Destination

把 `buzhou-core` / `buzhou-memory` / `buzhou-spill` / `buzhou-guard`（含其 store-jdbc/redis 落地面）从「**功能完备**」推进到「**真正生产级**」：停机与生命周期、挂起与预算、租约与 fence、事件背压、存储增长治理（迁移/清理/保留/配额）、事务正确性、可诊断性（错误分类/日志/泄漏检测/健康指标）、配置校验与默认值安全、guard 运维闭环（密钥轮换/策略热更/沙箱限额）、故障注入韧性测试。全部以 **GitHub stars ≥ 10K 的开源项目**为采纳事实源（不达标者仅注记）。到达 = 四机制具备运行时库的外围防护层，七类系统性缺口（见研究文档 §0）全部闭合且全量测试绿。

## Notes

- **领域**：Spring AI 2.0.0 之上的 Agent 运行时 Harness（JDK 21 / Spring Boot 4.1 / 虚拟线程）。术语见仓库 `CONTEXT.md`，机制详设见 `docs/spec/`。
- **每会话**：先读本 MAP → 从 frontier 取一张 ticket → resolve 后回写「Decisions so far」。
- **用户常设授权（2026-08-14）**：全程「不需询问意见、全部按推荐迭代」——决策票允许以 ratify 研究推荐的方式 AFK 闭合（Resolution 注明可推翻）。
- **事实源**：[docs/research/oss-production-grade.md](../../docs/research/oss-production-grade.md)（6 并行子 agent 2026-08-14：2 本地勘察 + 4 外部研究；载荷性结论已复核）。
- **10K+ stars 政策**：沿用 effort #2——采纳事实源只认 ≥10K★ OSS；JMH/Reactor/SLSA/cosign 等不达标者注记；不达标依赖不得进 classpath。
- **上游刻度**：effort #1/#2 已交付 Tier-1/2/3 全量机制（FakeChatModel 测试基建、CancelMode、RunRegistry、ToolCallLog、interrupt/resume、批提交、四模 recall、context-clearing、chunk hash、AST 切片、FIDES taint、ECDSA 审计链、policy 子集、ONNX 编排、Deno 沙箱）。本轮**不重做机制**、只补生产级外围防护层；机制语义变更须同步修订 docs/spec 对应篇。
- **测试哲学不变**：好测试只测外部行为；主接缝 = examples 端到端（FakeChatModel 驱动）；store 契约测试沿用 `AbstractBuzhouStoresContractTest` 范式。
- **tracker 约定**：见 [effort #3 约定存档](readme-effort-03.md)；票号 T55 起全局续用。
- **建造 Spec（ready-for-agent）**：`docs/spec/13-production-hardening.md`（/to-spec 产出）；执行切片 = `impl/`（/to-tickets 切出）。

## Decisions so far

- [生产级两轮研究——本地缺口勘察 + ≥10K★ 运维实践核验](../tickets/T55-oss-production-grade-verification.md) — 6 并行子 agent（2 本地勘察 + 4 外部研究）产出 [docs/research/oss-production-grade.md](../../docs/research/oss-production-grade.md)；七类系统性缺口定性；五项载荷性结论复核证实；五条数据治理公理入档。
- **core T56–T59（4 票，ratify → spec 13 §core-1..4）** — [优雅停机与生命周期](../tickets/T56-core-graceful-shutdown.md)（SmartLifecycle 分 phase + 排空 + stream 收尾）、[Turn Deadline 贯穿与挂起修复](../tickets/T57-core-turn-deadline.md)（绝对时刻 + 三阻塞点修复）、[租约续租/fence/LeaseLost](../tickets/T58-core-lease-renew-fence.md)（双路径续租 + 中止语义）、[事件背压与线程卫生](../tickets/T59-core-event-backpressure.md)（opt-in buffered + 异常隔离 + 线程命名）。
- **stores T60–T62（3 票，ratify → spec 13 §stores-5..7）** — [Schema 版本化迁移](../tickets/T60-stores-schema-migration.md)（自建零依赖 + 基线 + advisory lock + 恢复设施装配）、[级联清理与保留策略族](../tickets/T61-stores-retention-cascade.md)（deleteSession + 五公理形状）、[事务正确性与降级语义](../tickets/T62-stores-transaction-correctness.md)（UoW 接线 + UPSERT + 脏数据隔离 + 熔断半开）。
- [memory+spill 增长治理与 embedding 成本护栏](../tickets/T63-memory-spill-growth.md) — ratify → spec 13 §growth-8（InMemory 有界化 noeviction/volatile 族 + spill 生命周期 + CachedEmbeddingProvider + Episode 序号持久）。
- [guard 审计链/密钥/策略运维闭环](../tickets/T64-guard-ops-loop.md) — ratify → spec 13 §guard-9..10（AuditRecordStore 持久化 + SigningKeyRing 轮换 + Verifier + PolicySource 热加载 + SandboxLimits）。
- [横切配置校验、元数据与默认值安全化](../tickets/T65-config-validation-metadata.md) — ratify → spec 13 §cross-12（全参数可配 + JSR-303 + 元数据 + FailureAnalyzer + 默认值修正带迁移注记）。
- [横切可诊断性](../tickets/T66-diagnosability-observability.md) — ratify → spec 13 §cross-11（BuzhouException 分类 + 日志基线 + LeakDetector + Health/MeterBinder optional）。
- [横切故障注入与韧性测试基建](../tickets/T67-fault-injection-testbed.md) — ratify → spec 13 §cross-13（FaultInjectingToolCallback + 14 项韧性矩阵 + ApplicationContextRunner）。
- [生产级收口范围切定与优先级](../tickets/T68-scope-and-priority.md) — ratify → spec 13 Phase 0–5 排布（地基→致命→正确性→治理→运维→配置收口）。

## Not yet specified

- **InMemory 后端的有界化精确参数**（各类 store 的默认 max entries / 逐出策略选择）——实现时按 SessionQuota 语义定。
- **审计链持久化的存储介质选择**（复用 JDBC store 新表 vs 独立文件 append）——T64 落地时按「与 ToolCallLog 同介质」原则定。
- **policy 热加载的触发方式**（定时轮询 etag vs 文件 watch vs 手动 API）——T64 落地时定，默认轮询。
- **健康端点与 @Endpoint(id="buzhou") 的具体暴露面**（哪些 detail 进健康、哪些进端点）——T66 落地时定。
- **故障注入测试是否引入 Toxiproxy 容器**——T67 先交付进程内 FaultInjectingToolCallback，网络级注入按需再议。

## Out of scope

- **多实例分布式接管**（lease 升级分布式锁+心跳、Run 注册表跨实例）——单实例语义先行（沿用 effort #2）；PG advisory lock 仅作并发冷启动防炸修复。
- **buzhou-observability / otel / dashboard / mcp / skills / tools 模块做深**——健康/指标放四机制模块自身（micrometer optional 探测），观测模块维持现状。
- **独立 JMH 基准模块**（JMH 2,663★ 不达标）——性能护栏由 examples 集成测试承载。
- **发布 Maven Central、SLSA provenance、Trivy CI 门禁**——沿既有边界，可作文档注记。
- **Firecracker / E2B 沙箱档完整实现**（沿用 effort #2）。
- **FIDES 二期、sub-agent/multi-agent、跨 agent 共享记忆**（沿用 effort #2 边界）。

## Tickets

全部 ticket 已闭合（T55 research 子 agent 闭合；T56–T68 随 spec 13 批准 ratify 闭合，用户常设授权可推翻）。**Frontier**：∅（决策图已走完）。**建造 Spec**：[docs/spec/13-production-hardening.md](../../docs/spec/13-production-hardening.md)；执行切片 = `impl/`（`/to-tickets` 切出）。

**落地记录**：impl 28–43 已全部实现并合入 main（impl-28..38 于 2026-08-15 前夜落地；
impl-39 审计链持久化/密钥环/独立校验、impl-40 policy 热加载/沙箱限额、impl-41 泄漏检测/
健康/指标、impl-42 启动校验/FailureAnalyzer/默认值安全化、impl-43 配置元数据/韧性矩阵/
spec 同步/终验 于 2026-08-15 落地）。机制 Spec 同步：00 概览注记、01 增长治理+熔断半开、
05 停机/Deadline/租约、07 审计/密钥/policy/沙箱限额/健康指标、09 存储运维节。
