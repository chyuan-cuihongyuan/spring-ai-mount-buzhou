# Spring AI Mount Buzhou（不周山）

> **English** · A runtime harness mounted between **Spring AI** and your business agents — layered *on top of* Spring AI rather than replacing it. Buzhou is an **experimental framework designed for production scenarios**, aiming to keep a single agent stable, controllable and explainable through nine mechanisms: progressive memory compaction, spill protection, cognitive observability, skills, hot-pluggable MCP, parallel tool calls, atomic tools, hook guardrails, and a pluggable persistence SPI. Requires JDK 21+, Spring Boot 4.x and Spring AI 2.0.0.

> **中文** · 挂载在 **Spring AI** 与业务 Agent 之间的运行时中间层（Harness）——叠加而非替代 Spring AI。Buzhou 是一个**面向生产场景设计的实验性框架**，旨在让单个 Agent 稳定、可控、可解释地运行。

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F.svg)](https://spring.io/projects/spring-ai)
[![Build](https://github.com/chyuan-cuihongyuan/spring-ai-mount-buzhou/actions/workflows/ci.yml/badge.svg)](https://github.com/chyuan-cuihongyuan/spring-ai-mount-buzhou/actions/workflows/ci.yml)

---

## 为什么需要 Buzhou

Spring AI 解决了「如何把模型、工具、Advisor 链接到一起」的问题，但一个要在生产场景中长期运行的 Agent，还会持续撞上同一类稳定性与可解释性难题：

- **上下文窗口被工具返回撑爆**——一个大日志/查询结果就把历史顶出窗口，关键信息断崖式丢失。
- **工具调用慢、并发低**——串行调工具，单工具超时拖垮整轮。
- **不可观测**——只知道「调用发生了」，不知道模型基于什么证据、为什么得出结论，出问题无法回放。
- **危险操作无护栏**——删库、发版、改线上配置这类不可逆操作，缺乏框架级人工确认（HITL）。
- **状态记不住又不可靠**——跨实例续接、悬空调用修复、长产物读写，每家都要自己造一遍。

Buzhou 把这些「Agent 运行时」该有的能力收敛成九大机制，作为一层 Harness 挂在 Spring AI 之上。你的 `ChatClient` / `ChatModel` 不变，Buzhou 只在外围补齐面向生产场景所需的稳定性与可观测性——目前为实验性（alpha），详见[项目状态](#项目状态)。

## 九大机制

| # | 机制 | 一句话 | 模块 |
|---|------|--------|------|
| 1 | **渐进式记忆压缩** | 微压缩（纯内存回收旧工具返回，替换为带 evidence-id 的占位符）+ 九段式结构化摘要 + 动态预算，信息连续降级、永不断崖丢弃 | `buzhou-memory` |
| 2 | **Spill 溢出保护** | 超大工具返回自动落盘，上下文只留预览 + 回读路径，模型持 `read_range` 按字节区间 / JSON path / 分页回读 | `buzhou-spill` |
| 3 | **Span + Event 认知可观测** | 记录模型基于什么证据、做出什么推理、得到什么结论；内嵌可视化后台可回放会话、还原每轮实际注入的上下文 | `buzhou-observability` / `buzhou-observe-otel` / `buzhou-observe-dashboard` |
| 4 | **Skill 体系** | 能力按需加载，上下文只放清单（name + description），用到再取正文；内置（classpath）与 DB 动态两种来源 | `buzhou-skills` |
| 5 | **MCP 热插拔** | 工具集配置驱动、运行时热更新；靠差量刷新 + 引用计数延迟关闭保证安全 | `buzhou-mcp` |
| 6 | **并行工具调用** | 虚拟线程 fan-out 并行执行、按序回注，单工具超时/取消不拖整轮 | `buzhou-core` |
| 7 | **原子工具** | 框架内置最小可复用工具集：文件读写、命令执行、HTTP 调用、任务清单等 | `buzhou-tools` |
| 8 | **Hook 护栏体系** | 长产物读写护栏、HITL 危险操作人工审核、Hook→state→Attachment 联动闭环（补失忆范式） | `buzhou-guard` |
| 9 | **持久化 SPI** | 五大存储 SPI（Message / Summary / SessionState / SessionLease / Observability）+ 内存/JDBC/Redis 实现，按需切换 | `buzhou-core` / `buzhou-store-jdbc` / `buzhou-store-redis` |

> 领域术语以 [CONTEXT.md](CONTEXT.md) 为准；各机制的完整设计见 [docs/spec/](docs/spec/)（00-overview 总入口 + 机制详设 01–23）。

## 生产级纵深（effort #5 新增）

九大机制之上的运营级能力（详设 spec 15–23）：

| 能力 | 一句话 | 详设 |
|------|--------|------|
| **模型熔断 + 备模型降级链** | 失败率跳闸→半开探测恢复；主模型熔断 OPEN 后请求零重试直达备模型 | [spec 15](docs/spec/15-model-resilience.md) |
| **Token/成本预算** | 会话级 token/成本累计（microUsd 整数口径价目换算）+ 三硬顶预算闸 | [spec 16](docs/spec/16-cost-quota.md) |
| **per-session 日配额** | turns / tool-calls / tokens 每日每会话限额（UTC 日窗，超限 Block） | [spec 16](docs/spec/16-cost-quota.md) |
| **结构化输出** | `chatForEntity`——schema 注入 + 解析失败 REASK 一次 + 结构化异常 | [spec 19](docs/spec/19-structured-output.md) |
| **会话 fork** | 历史完整复制 + 预算重置的重试/探索分支 | [spec 20](docs/spec/20-session-fork-webhook-compact.md) |
| **事件外发 webhook** | 会话事件 at-least-once 投递（HMAC 签名 + 幂等键 + 退避重试） | spec 20 |
| **手动压缩 / 摘要导出** | 宿主侧 ManualCompactor（与 compact_now 同管线）+ 类型化/Markdown 导出 | spec 20 |
| **run_command 沙箱合流** | core CommandBackend SPI：guard 沙箱档（Deno/E2B/Firecracker）可插拔接管命令执行 | [spec 17](docs/spec/17-sandbox-convergence.md) |
| **MCP 工具集漂移检测** | 协议 `tools/list_changed` 订阅 + 基线差量告警 | [spec 18](docs/spec/18-mcp-drift.md) |
| **质量与供应链门** | 覆盖率 LINE≥70% 硬门 / SpotBugs High 硬门 / 红队数值化双硬门 / CycloneDX SBOM / Dependabot / 性能哨兵 | [spec 21](docs/spec/21-config-supply-quality.md) / [spec 22](docs/spec/22-redteam-skills.md) |

运维接手见 **[docs/ops-runbook.md](docs/ops-runbook.md)**；公开 API 面见 [docs/api-surface.md](docs/api-surface.md)。

## 生产级纵深（effort #6 新增）

投递可靠性 / 数据生命周期 / 输入面 / 运维面 / 质量面的第二级纵深（详设 spec 24–32）：

| 能力 | 一句话 | 详设 |
|------|--------|------|
| **Webhook 持久化 Outbox** | 事件 emit 即落 state store，跨重启补投递 + 记录级退避 + 死信可查（at-least-once + 幂等键） | [spec 24](docs/spec/24-webhook-outbox.md) |
| **熔断冷却自适应退避** | 连续跳闸冷却指数放缓（封顶 backoff-cap），探测成功即复位 | [spec 25](docs/spec/25-adaptive-circuit-backoff.md) |
| **fork 证据引用计数** | fork 存续期源证据不被删除（最后引用者关闭）；悬垂读 EVIDENCE_GONE 容错 | [spec 26](docs/spec/26-evidence-refcount.md) |
| **多模态输入（MediaRef）** | chat/stream/chatForEntity 携带媒体 URI；持久化 + 最近重发策略 + 媒体计费 | [spec 27](docs/spec/27-multimodal-input.md) |
| **会话导出/导入** | 单 JSON 文档跨环境移植（默认 Id 重映射 / keepIds 冲突 fail-fast） | [spec 28](docs/spec/28-session-export-import.md) |
| **Store fsck** | 五 store 对账：孤儿摘要/残留 state/泄漏租约/悬挂观测——只读报告 + 按项修复 | [spec 29](docs/spec/29-store-fsck.md) |
| **会话索引** | 五 store 外的枚举/过滤查询面（生命周期维护、最终一致；内存/JDBC/Redis） | [spec 30](docs/spec/30-session-index.md) |
| **工具结果限幅** | 结果入上下文前 20K 字符护栏（截断+提示尾+per-tool 豁免） | [spec 31](docs/spec/31-tool-result-limit.md) |
| **黄金轨迹回归集** | 六大机制「脚本化输入→事件序列断言」行为回归防线 | [spec 32](docs/spec/32-golden-trajectories.md) |

## 技术基线

| 依赖 | 版本 |
|------|------|
| JDK | 21+（虚拟线程） |
| Spring Boot | 4.1.0 |
| Spring AI | 2.0.0 |
| Maven | 3.9+ |

## 模块结构

依赖图是以 [`buzhou-core`](buzhou-core) 为根的两层星形，**物理无环**：各机制模块（memory / spill / skills / mcp / guard / tools / store-*）互不直接依赖，跨机制协作一律走 core 的事件总线或 core SPI。唯一允许的二层边是 `buzhou-observe-otel` / `buzhou-observe-dashboard` 依赖 `buzhou-observability`。

```
                     buzhou-core（内核：session / exec / hook / spi / policy）
                            │
   ┌────────┬──────────┬────┴─────┬──────────┬──────────┬──────────┐
buzhou-   buzhou-   buzhou-    buzhou-    buzhou-    buzhou-    buzhou-
memory    spill     observability skills   mcp        guard      tools
   │                  │   │
   │        buzhou-observe-otel / buzhou-observe-dashboard（二层边）
   │
buzhou-store-jdbc / buzhou-store-redis（只依赖 core SPI，按需引入）

buzhou-spring-boot-starter —— 纯依赖聚合，引入即得全部机制的自装配（无代码）
buzhou-bom                 —— 全模块同版本收口
```

| 模块 | 职责 | 开关 | 默认 |
|------|------|------|------|
| `buzhou-core` | 内核：会话入口 `AgentRuntime.spawn()`、执行脊柱、Hook 链、SPI、四层配置 | — | 始终装配 |
| `buzhou-memory` | 渐进式记忆压缩（微压缩 + 九段摘要 + 动态预算） | `buzhou.memory.enabled` | 开 |
| `buzhou-spill` | Spill 溢出保护与 `read_range` 回读 | `buzhou.spill.enabled` | 开 |
| `buzhou-observability` | Span + Event 认知可观测核心 | `buzhou.observability.enabled` | 开 |
| `buzhou-observe-otel` | OpenTelemetry 导出器 | `buzhou.observe.otel.enabled` | **关** |
| `buzhou-observe-dashboard` | 可视化会话回放后台 | `buzhou.observe.dashboard.enabled` | **关** |
| `buzhou-skills` | Skill 体系（内置 + DB 动态） | `buzhou.skills.enabled` | 开 |
| `buzhou-mcp` | MCP 工具集热插拔 | `buzhou.mcp.enabled` | 开 |
| `buzhou-guard` | Hook 护栏（读写护栏 / HITL / 事实闭环） | `buzhou.guard.enabled` | 开 |
| `buzhou-tools` | 原子工具集 | `buzhou.tools.enabled` | 开 |
| `buzhou-store-jdbc` | JDBC 持久化实现 | `buzhou.store.type=jdbc` | — |
| `buzhou-store-redis` | Redis 持久化实现 | `buzhou.store.type=redis` | — |
| `buzhou-spring-boot-starter` | 依赖聚合 starter（无代码） | — | — |
| `buzhou-bom` | 版本 BOM | — | — |

> safe-by-default：多数机制默认开启；otel / dashboard 这类需要外部后端或端口的默认关闭。

## 快速开始

> 当前版本 `0.1.0-SNAPSHOT`，尚未发布到 Maven Central。请先从源码构建安装到本地仓库：
>
> ```bash
> git clone https://github.com/chyuan-cuihongyuan/spring-ai-mount-buzhou.git
> cd spring-ai-mount-buzhou
> mvn clean install -DskipTests
> ```
>
> 待首发 `0.1.0` 后即可直接从 Maven Central 引用，无需本地构建。发布流程见 [RELEASING.md](RELEASING.md)。

### 方式一：Spring Boot starter（推荐）

引入聚合 starter，即得全部机制模块的自动装配：

```xml
<dependency>
  <groupId>io.github.chyuan-cuihongyuan</groupId>
  <artifactId>buzhou-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

各机制模块自带 `AutoConfiguration`，按 `buzhou.<mech>.enabled` 开关装配。直接注入 `AgentRuntime` 使用：

```java
@Component
class TroubleshootAgent {

    private final AgentRuntime runtime;

    TroubleshootAgent(AgentRuntime runtime) {
        this.runtime = runtime;
    }

    void handle(String sessionId, String userMessage) {
        // spawn 拿到一个带租约的 AgentSession（同会话单活跃实例）
        try (AgentSession session = runtime.spawn("my-app", "troubleshooter", sessionId)) {
            String reply = session.chat(userMessage);      // 同步，返回最终回复文本
            // 或流式：session.stream(userMessage) → Flux<ChatResponse>
            System.out.println(reply);
        } // try-with-resources 自动 close()
    }
}
```

`application.yml`：

```yaml
buzhou:
  model-name: gpt-4o          # 供 memory / observability 等模块共享
  store:
    type: memory              # memory（默认）| jdbc | redis
  spill:
    enabled: true             # 默认开
  observe:
    dashboard:
      enabled: true           # 需要可视化回放时显式打开（默认关）
```

### 方式二：叠加到现有 ChatClient（最小侵入）

Buzhou 的核心理念是**叠加而非替代**。如果你已有基于 Spring AI `ChatClient` 的代码，用 `Buzhou.enhance(...)` 把 Harness 能力挂上去即可，原有调用方式不变：

```java
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;

// 把记忆压缩 / Spill / 可观测 / 护栏等能力叠加到现有 ChatClient.Builder
ChatClient client = Buzhou.enhance(ChatClient.builder(chatModel)).build();
```

### 方式三：纯编程式（无 Spring 容器）

适合测试、嵌入式场景或不想引入完整 Spring Boot 上下文：

```java
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;

// 内存存储 + 默认配置，传入你的 ChatModel 与工具
BuzhouStores stores = Buzhou.inMemoryStores();
AgentRuntime runtime = Buzhou.runtime(chatModel, stores);

try (AgentSession session = runtime.spawn("app", "agent", "session-1")) {
    String reply = session.chat("帮我查一下订单 123 为什么失败");
}
```

> 完整可运行示例（记忆压缩链 + Spill 回读、Skill 与 MCP、Guard 与 HITL、可观测回放）见 [examples/](examples/) 模块。

## 配置项一览

| 配置项 | 默认 | 说明 |
|--------|------|------|
| `buzhou.model-name` | `unknown` | 模型名，供 memory / observability 等模块共享 |
| `buzhou.store.type` | `memory` | 持久化实现：`memory` / `jdbc` / `redis` |
| `buzhou.memory.enabled` | `true` | 渐进式记忆压缩 |
| `buzhou.spill.enabled` | `true` | Spill 溢出保护 |
| `buzhou.observability.enabled` | `true` | Span + Event 认知可观测核心 |
| `buzhou.skills.enabled` | `true` | Skill 体系 |
| `buzhou.mcp.enabled` | `true` | MCP 热插拔 |
| `buzhou.guard.enabled` | `true` | Hook 护栏 |
| `buzhou.tools.enabled` | `true` | 原子工具 |
| `buzhou.observe.otel.enabled` | `false` | OpenTelemetry 导出器 |
| `buzhou.observe.dashboard.enabled` | `false` | 可视化回放后台 |
| `buzhou.resilience.enabled` | `true` | 模型韧性（重试/退避/错误分类/超时取消/限流） |
| `buzhou.resilience.max-attempts` | `3` | 最大尝试次数（含首次） |
| `buzhou.resilience.deadline` | `60s` | 模型调用统一超时（0 = 关，不推荐） |
| `buzhou.resilience.rate-limit.requests-per-minute` | 不限 | 模型 RPM 桶 |
| `buzhou.runaway.enabled` | `true` | 失控检测（阈值默认不设 = 不限，safe-by-default） |
| `buzhou.runaway.per-turn.max-steps` | 不限 | 单轮最大步数（软阈值 80% 提醒，硬顶终止） |
| `buzhou.runaway.per-turn.wall-clock` | 不限 | 单轮墙钟上限 |
| `buzhou.backpressure.max-concurrent-sessions` | 不限 | 会话并发容量闸 |
| `buzhou.core.tool-timeout` | `60s` | 单工具执行超时（长任务工具须同步调大） |
| `buzhou.core.event-dispatch.mode` | `sync` | 事件分发：`sync` / `buffered`（有界队列） |
| `buzhou.leak.level` | `SIMPLE` | 资源泄漏检测：`DISABLED`/`SIMPLE`/`ADVANCED`/`PARANOID` |
| `buzhou.lifecycle.timeout-per-shutdown-phase` | `30s` | 优雅停机排空预算 |
| `buzhou.retention.enabled` | `true` | 保留策略族后台清扫 |
| `buzhou.store.in-memory.*` | 有界配额 | InMemory 套件容量配额 |
| `buzhou.observe.dashboard.bind-address` | `127.0.0.1` | 后台绑定地址（非 loopback 必须配 auth-token，否则拒启动） |
| `buzhou.observe.dashboard.auth-token` | 无 | Bearer 鉴权（支持 `${ENV:}` 占位） |
| `buzhou.observe.otel.exporter-mode` | `otlp` | `otlp` / `tracer`（后者需容器 Tracer bean） |
| `buzhou.mcp.dangerous-tool-patterns` | 动词模式集 | 客户端侧危险工具登记（挂 guard HITL） |
| `buzhou.mcp.shutdown-budget` | `35s` | MCP 关闭总预算 |

> 配置遵循四层覆盖模型：默认 < `application.yml` < 绑定级 < 工具级。详见 [docs/spec/08-session-config-persistence.md](docs/spec/08-session-config-persistence.md)。
> 全部键在 IDE 有补全与校验（`spring-configuration-metadata.json` 随 jar 发布，impl-52 起）。

## 文档

- [CONTEXT.md](CONTEXT.md) — 领域术语表（Harness、微压缩、Spill、Span/Event、Hook 链……）
- [docs/spec/00-overview.md](docs/spec/00-overview.md) — 设计总入口
- 机制详设：[01 记忆压缩](docs/spec/01-memory-compaction.md) · [02 Spill](docs/spec/02-spill.md) · [03 可观测](docs/spec/03-observability.md) · [04 Skill/MCP](docs/spec/04-skill-mcp.md) · [05 并行工具](docs/spec/05-parallel-tools.md) · [06 原子工具](docs/spec/06-atomic-tools.md) · [07 Hook 护栏](docs/spec/07-hooks.md) · [08 会话/配置/持久化](docs/spec/08-session-config-persistence.md) · [09 模块与工程化](docs/spec/09-modules-engineering.md)
- [与 Spring AI 2.0 原生能力边界](docs/spec/10-spring-ai-boundary.md) — Buzhou 九机制相对 Spring AI 2.0 的 REPLACES / ADDS / NATIVE 诚实对照（中英）
- [11 最佳实践采纳](docs/spec/11-best-of-breed-adoption.md) / [12 完美采纳](docs/spec/12-perfect-adoption.md) — 生产级机制采纳路线
- [13 生产收口](docs/spec/13-production-hardening.md) — core/memory/spill/guard 生产级收口（生命周期/错误分类/泄漏检测/健康指标/配置校验）
- [14 外围收口](docs/spec/14-perimeter-hardening.md) — 观测三模块安全化 + mcp/skills/tools 收口 + resilience/runaway/容量闸移植 + 配置元数据/红队/CI 基建
- [15 模型韧性与失控防护](docs/spec/15-model-resilience.md) — 重试/退避/错误分类/限流/失控检测四层硬顶/会话容量闸机制详设
- [RELEASING.md](RELEASING.md) — 发布到 Maven Central 的流程

## 兼容矩阵

| Buzhou | JDK | Spring Boot | Spring AI | 备注 |
| --- | --- | --- | --- | --- |
| 0.1.x | 21 | 4.1.x | 2.0.x | 0.x 语义：minor 可破兼容 |

## 项目状态

**早期开发（alpha）**。版本 `0.1.0-SNAPSHOT`，公共 API（`api` 子包）尚未冻结：遵循 `0.x` 语义，minor 版本可能破坏兼容，待公共 API 稳定后再发布 `1.0.0`。设计 Spec 在 `docs/spec/`，改机制先改 Spec。

## 贡献

欢迎贡献！请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。核心约定：领域术语以 `CONTEXT.md` 为准、机制设计以 `docs/spec/` 为准（改机制先改 Spec）、遵循 Conventional Commits、行为变更必须带测试。参与前请遵守 [行为准则](CODE_OF_CONDUCT.md)。

## 致谢

Buzhou 的设计部分借鉴了公开技术文章中描述的 Agent 运行时与 Hook 护栏思路（携程 Spring-Ai-Trip、腾讯 DECO hooks），在此基础上结合 Spring AI 2.0 的 Advisor 链与虚拟线程做了重新落地与推演。设计忠实度说明见 `docs/spec/`。

## License

[Apache License 2.0](LICENSE) © 2024-2026 chyuan (io.github.chyuan-cuihongyuan)
