# Wayfinder Map — Buzhou 生产级纵深（effort #5）

> effort #5，延续 [`.wayfinder/`](../.wayfinder/MAP.md)（#1）、[`.wayfinder2/`](../.wayfinder2/MAP.md)（#2）、[`.wayfinder3/`](../.wayfinder3/MAP.md)（#3：core/memory/spill/guard，impl 28–43）、[`.wayfinder4/`](../.wayfinder4/MAP.md)（#4：外围六模块收口 + 基建，impl 44–55）。

## Destination

把 effort #4 终验后仍开放的生产级缺口**全部闭合**：韧性与成本纵深（模型熔断、备模型降级链、token/成本预算、per-session 配额）、run_command↔CommandSandbox 合流、MCP 工具集漂移检测、结构化输出、会话 fork、事件外发 webhook、四模块配置正规化、供应链（SBOM/依赖扫描）、性能基准、红队数值化、质量门卡线、运维 runbook 与 API 稳定性审计。到达 = 20+ 轮「wayfinder→to-spec→to-tickets→implement」自迭代全部落地、全仓 verify 绿、硬门与文档齐备、知识库同步收口。

## Notes

- **领域**：Spring AI 2.0.0 之上的单 Agent 运行时 Harness（JDK 21 / Spring Boot 4.1 / 虚拟线程）。术语见 `CONTEXT.md`，机制详设见 `docs/spec/`。
- **用户常设授权（2026-08-15）**：全程「不需询问、全部自决、按推荐迭代」——决策票以 AFK 方式闭合（Resolution 注明可推翻）；to-spec 的 seam 确认与 to-tickets 的 breakdown quiz 同样按推荐自决；20–25 轮完整流程自迭代。
- **10K★ 政策**：借鉴对象只认 ≥10K★ OSS（LangChain/LangGraph ~100K★、OpenHands ~50K★、AutoGen ~40K★、Dify ~100K★、CrewAI ~30K★、aider ~30K★、Spring AI 生态本身等）；**语义借鉴优先、不轻易引新依赖**；不达标依赖不得进 runtime classpath（构建/测试插件单独注记）。事实源：2026-08-15 本地全仓勘察（Top10 硬缺口清单见 [T81](tickets/T81-circuit-breaker.md) 前置勘察记录）+ 既往 [docs/research/](../docs/research/) 四份。
- **测试哲学不变**：好测试只测外部行为；主接缝 = examples 端到端（FakeChatModel/ScriptedChatModel 驱动）；store 契约测试沿用 `AbstractBuzhouStoresContractTest`；测试不得 import 他模块 `internal` 包。
- **每轮流程**：解 1 张决策票 → /to-spec 增量（spec 15 扩展 + 新 16–23 号 spec）→ /to-tickets 切片 → /implement → 模块级测试 → emoji 规范 commit；里程碑（第 11、22 轮）全仓 `mvn clean verify`。
- **tracker 约定**：见 [README.md](README.md)；票号 T81 起全局续用；impl 切片 56 起续用。

## Decisions so far

- [模型熔断器](tickets/T81-circuit-breaker.md) — 手写 CB（不引 resilience4j）：三态结果计数口径（FAILURE=NETWORK/SERVER/TIMEOUT，RATE_LIMIT/CONTENT/AUTH/UNKNOWN IGNORED）、计数窗口失败率跳闸、冷却后半开单探测、进程级注册表按 modelName 分桶、open 时 fail-fast 异常直上 onModelError、circuit.* 配置默认开 + fail-fast 校验。
- [备模型降级链](tickets/T82-fallback-model-chain.md) — 降级发生在 ResilienceAdvisor 逻辑调用内部（外层 advisor 不可见）；fallback.models bean 名列表（Spring 按名解析 fail-fast / 编程式 NamedFallbackModel）；触发=终态失败 category（默认 NETWORK/SERVER/TIMEOUT/AUTH）+ 主模型熔断 OPEN 恒触发；无粘性（每调用先主后备，OPEN 时零成本直达备模型）；备模型各一次尝试、独立熔断记账；全败上抛主因；M1 流式不降级。
- [Token/成本预算](tickets/T83-token-cost-budget.md) — core 新 budget 包 TokenBudgetHook（order 1100）：afterModel 从 usage 累计会话 token/成本进 SessionStateStore（micro-USD long 无浮点）；价目 buzhou.token-budget.pricing.<model>.*（无价=零成本）；硬顶 max-session-prompt/total-tokens/cost-usd（cost 上限无价目 fail-fast），beforeModel 超限 block + 双写事件；safe-by-default null=不限。

## Not yet specified

- 熔断半开参数自适应（静态参数足够则不升级为自适应）。
- fork 与 memory 微压缩的边角：fork 后 evidence-id 指针的归属与生命周期。
- webhook 投递语义（先 at-least-once + 幂等键；exactly-once 不承诺）。
- per-session 配额的持久化跨重启语义（首版内存窗口 + 事件可见，分布式配额显式不做）。

## Out of scope

- **多实例分布式接管、分布式限流/配额**（沿用 #2/#3/#4 边界；单进程组件在 T99 显式文档化）。
- **Firecracker/E2E 沙箱完整档、FIDES 二期、sub-agent、跨 agent 共享记忆**（沿用 #2 边界）。
- **多 agent 编排/workflow 引擎**（本仓定位单 Agent harness；LangGraph 式图编排不做）。
- **LLM-as-judge 强制 CI 门禁**（保持可选方法论，不进硬门）。
- **dashboard 前端工程化、观测 OLAP、MCP server 侧、发布流程改造**（沿用 #4 边界）。

## Tickets

22 张决策票 T81–T102，各对应一轮完整自迭代；frontier 顺序 = 票号序。详见 `tickets/`。
