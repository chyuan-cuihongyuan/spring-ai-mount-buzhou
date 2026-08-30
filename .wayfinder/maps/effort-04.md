# Wayfinder Map — Buzhou 全模块生产级收口（effort #4）

> effort #4，延续 [effort #1](effort-01.md)（#1）、[effort #2](effort-02.md)（#2）、[effort #3](effort-03.md)（#3：core/memory/spill/guard 四机制收口，impl 28–43 落地）。

## Destination

把 effort #3 明确划为 out-of-scope 的**全部剩余面**推进到「真正生产级」：外围五模块（observability / observe-otel / observe-dashboard / mcp / skills / tools）补齐 core 四模块已达标的防护层（生命周期、日志、配置校验与元数据、健康与指标、有界化）、**buzhou-resilience 从分支合入 main** 并补齐装配、starter/配置元数据/BOM 基建真正生效、redteam 目标真实化、CI 质量工程、文档口径统一。到达 = 全部 15+1 模块在同一标准下收口、全量测试绿、外围无 P0 缺口。

## Notes

- **领域**：Spring AI 2.0.0 之上的 Agent 运行时 Harness（JDK 21 / Spring Boot 4.1 / 虚拟线程）。术语见 `CONTEXT.md`，机制详设见 `docs/spec/`。
- **每会话**：先读本 MAP → 从 frontier 取一张 ticket → resolve 后回写「Decisions so far」。
- **用户常设授权（2026-08-14，本轮重申）**：全程「不需询问意见、全部按推荐迭代」——决策票允许以 ratify 研究推荐的方式 AFK 闭合（Resolution 注明可推翻）。to-spec 的 seam 确认与 to-tickets 的 breakdown quiz 同样按推荐自决。
- **事实源**：2026-08-15 三路本地勘察（observability/otel/dashboard、mcp/skills/tools、resilience/starter/BOM/examples/redteam/CI/文档）+ [T69 外部核验](../tickets/T69-external-verification.md)。既往研究：[docs/research/oss-production-grade.md](../../docs/research/oss-production-grade.md) 等。
- **10K+ stars 政策**：沿用——采纳事实源只认 ≥10K★ OSS；不达标者注记；不达标依赖不得进 runtime classpath（构建插件不受此限，单独注记）。
- **测试哲学不变**：好测试只测外部行为；主接缝 = examples 端到端（FakeChatModel/ScriptedChatModel 驱动）；store 契约测试沿用 `AbstractBuzhouStoresContractTest` 范式；跨模块集成测试落 examples；测试不得 import 他模块 `internal` 包（dashboard 测试现存违例须修）。
- **tracker 约定**：见 [effort #4 约定存档](readme-effort-04.md)；票号 T69 起全局续用；impl 切片号 44 起续用。
- **建造 Spec（ready-for-agent）**：`docs/spec/14-perimeter-hardening.md`（/to-spec 产出）；执行切片 = `impl/`（/to-tickets 切出）。

## Decisions so far

- [外围收口外部核验](../tickets/T69-external-verification.md) — [docs/research/oss-perimeter-hardening.md](../../docs/research/oss-perimeter-hardening.md)：六条线采纳结论（Actuator 安全模型 / MeterBinder 单口径 / 客户端侧 MCP 危险分类 / OpenHands 沙箱三要素 / configuration-processor 必补 / promptfoo guardrails 契约）+ 不达标注记。
- [resilience 分支增量移植](../tickets/T70-resilience-merge.md) — 不合分支；移植 buzhou-resilience 模块整块 + core/runaway 失控检测 + SpawnGate 容量闸；跳过分支 crash-recovery/graceful-shutdown（与 main impl-30/34 平行）；熔断本轮不做。
- [observability 收口](../tickets/T71-observability-hardening.md) — 管线 bean 化 + 日志基线 + 流取消终态 + 配置 fail-fast + **指标家族收敛进 core MeterBinder 单口径** + 快照空壳最小实现 + 死代码删除。
- [otel 收口](../tickets/T72-otel-hardening.md) — openSpans 有界驱逐 + sessionTrace 清理 + 静默 catch 日志化 + tracer 缺 bean 启动失败 + 枚举校验 + OTLP headers/timeout + 文档/pom 修正。
- [dashboard 安全](../tickets/T73-dashboard-security.md) — 默认 127.0.0.1；非 loopback 无 token 拒启动；bearer token 鉴权；500 不回显；body/分页上限；executor/esc/pathPrefix 修复；测试去 core.internal。
- [mcp 收口](../tickets/T74-mcp-hardening.md) — SDK 进程内真实协议测试 + DB 源接线 + 配置校验元数据 + 健康/指标 + 日志 + shutdown 预算 + 客户端侧危险工具模式登记；快照重发现不做。
- [skills 收口](../tickets/T75-skills-hardening.md) — JDBC/Redis SkillStore（含 ToolSetSpecStore JDBC）+ 装配接线 + 扫描失败可见化 + frontmatter 多行 + 资源上限/缓存/正文上限 + setBinding 校验。
- [tools 收口](../tickets/T76-tools-hardening.md) — 取消杀进程树 + env 白名单 + 读入上限 + 头过滤/超时校验 + fe80::/10 + 原子写 + toolTimeout 可配；guard 沙箱合流不做。
- [配置元数据基建](../tickets/T77-starter-config-metadata.md) — 根 pom 统一 configuration-processor + 元数据 jar 断言 + JSR-303 全量 + FailureAnalyzer 扩两个 + BOM 补 test-jar/resilience + starter 反向/冒烟用例。
- [redteam 真实化](../tickets/T78-redteam-truthfulness.md) — 真实回复透传 + x-buzhou-guard-blocked 头 + transformResponse/guardrails 断言 + baseline.md + nightly 审计重放 job + 升硬门标准。
- [CI 质量工程](../tickets/T79-ci-quality-engineering.md) — jaCoCo 观测不设线 + spotbugs 观测 + CodeQL + ci.yml 加固 + action 版本统一 + maven-wrapper；checkstyle/pmd/spotless/多 JDK 不做。

## Not yet specified

（清空——毕业进 impl 或按注记转后续项。后续项清单：模型熔断/重试预算、MCP 工具快照重发现、run_command 与 guard CommandSandbox 合流、frontmatter 多行/清单缓存、skills/tools 配置正规化进 @ConfigurationProperties、redteam F1 数值化、覆盖率阈值卡线。）

## Out of scope

- **多实例分布式接管**（沿用 effort #2/#3 边界）。
- **Firecracker / E2E 沙箱档完整实现、FIDES 二期、sub-agent、跨 agent 共享记忆**（沿用 effort #2 边界）。
- **前端工程化改造 dashboard**（保持零构建单页 HTML；只修安全与健壮性）。
- **观测数据第二存储/OLAP**（dashboard 继续只读 ObservabilityStore SPI）。
- **MCP server 侧实现**（本仓只做 client 侧 harness）。
- **发布流程改造**（Central Portal 发布链已就绪，不改）。

## Tickets

全部闭合：T69–T79 决策票 + T80 终验票（2026-08-15）；impl 44–55 十二个执行切片全部落地并合入 main。
**Frontier**：∅（决策图走完；effort #4 到达目的地——全模块生产级收口、全仓 verify 绿、外围 P0 清零）。

## 收口记录（2026-08-15）

- **impl 44–55 十二切片全部落地**（resilience 移植 → runaway/容量闸 → observability → otel → dashboard 安全 → tools → mcp（真协议测试）→ skills 持久化 → 配置元数据基建 → redteam 真实化 → CI 质量工程 → 文档/示例/终验）。
- **全仓 `mvn clean verify` 绿**；14 模块 jaCoCo 报告；11 模块配置元数据生效。
- 外围 P0 清零：dashboard 零鉴权绑 0.0.0.0、观测零日志、otel 无界 Map、run_command 孤儿进程、redteam 硬编码文案、死元数据文件——全部修复并有测试护栏。
