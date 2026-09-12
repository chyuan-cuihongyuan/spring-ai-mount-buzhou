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

> 领域术语以 [CONTEXT.md](CONTEXT.md) 为准；各机制的完整设计见 [docs/spec/](docs/spec/)（00-overview 总入口 + 机制详设 01–55）。

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

## 生产级纵深（effort #7–#35 增量）

> 此后各 effort 的精选主线（全量见 [docs/spec/](docs/spec/) 与 [运维手册](docs/ops-runbook.md)；每项默认零行为变化或 opt-in）。

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 共享与原子性 | 共享限流闸 | Redis 分钟固定窗——全实例同一份 RPM/TPM 额度 | [spec 54](docs/spec/54-shared-rate-limit.md) |
| | 原子计数写 | state store CAS 原语（内存/JDBC/Redis WATCH）+ 配额/runaway/budget 三处统一——多实例不丢计数 | [spec 56](docs/spec/56-shared-quota-atomic.md) / [62](docs/spec/62-counter-atomicity-spread.md) |
| | 共享熔断闸 | 任一实例跳闸全实例 OPEN（TTL 标记）；探测达标任一实例清除恢复 | [spec 57](docs/spec/57-shared-circuit-breaker.md) |
| 缓存与前缀 | 精确响应缓存 | 终态答案键值命中零模型调用（LRU-TTL + 流式重放） | [spec 53](docs/spec/53-response-cache.md) |
| | 语义缓存 | 同义问法 embedding 相似度命中（FAQ 型负载省模型调用） | [spec 55](docs/spec/55-semantic-cache.md) |
| | 技能目录语义排序/检索语义面 | 注入与检索按当前问法相似度排序（预算内保最相关；零命中近邻提示） | [spec 59](docs/spec/59-skill-semantic-ranking.md) / [73](docs/spec/73-skill-search-semantic.md) |
| | 前缀稳定注入序 | 稳定块前置最大化 provider KV-cache 前缀命中 | [spec 66](docs/spec/66-prefix-stable-injection.md) |
| | 延迟感知备模型排序 | 降级链按 EMA 延迟升序尝试（快者优先） | [spec 64](docs/spec/64-latency-aware-fallback.md) |
| 评估闭环 | 评估数据集/runner/查询 | 数据集 + 隔离执行 + 三态记录 + 回流（负反馈/会话轨迹建集） | [spec 52](docs/spec/52-eval-loop.md) / [72](docs/spec/72-trajectory-importer.md) |
| | LLM-as-judge / 成对对比 | judge 评分协议 + A/B 双向裁定消位置偏差 + 一键 A/B 胜率（run 落盘可回溯 + 完成事件） | [spec 61](docs/spec/61-llm-judge-evaluator.md) / [63](docs/spec/63-pairwise-judge.md) / [71](docs/spec/71-pairwise-eval-runner.md) / [74](docs/spec/74-ab-run-persistence.md) / [75](docs/spec/75-ab-run-events.md) |
| | 评估并行执行 | 虚拟线程并行 + 项序聚合确定性 | [spec 68](docs/spec/68-parallel-eval.md) |
| 观测与分析 | 观测 OLAP JSONL 导出 | spans/events 一行一 JSON（DuckDB/ClickHouse 直装）+ 增量水位 | [spec 60](docs/spec/60-observability-jsonl-export.md) / [67](docs/spec/67-olap-incremental-export.md) |
| | outbox 读放大消减 | 容量计数下推 + Redis 流水线批量读 | [spec 58](docs/spec/58-outbox-scan-amplification.md) |
| 恢复与压缩 | 崩溃自愈 watchdog | 启动自动接管疑似崩溃 run（租约门不打扰活跃实例） | [spec 69](docs/spec/69-crash-resume-watchdog.md) |
| | 边界机会压缩 | 待摘积压达阈值在轮边界提前摘要（宽松态生成） | [spec 70](docs/spec/70-boundary-compaction.md) |
| 成本归因 | agent 级成本台账 | 按 agentName 跨会话累计 tokens/microUsd（CAS 原子） | [spec 65](docs/spec/65-agent-cost-ledger.md) |

## 生产级纵深（effort #36–#55 增量）

50 轮自迭代会话前半程的精选主线（详设 spec 75–94；每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 评估闭环 | A/B run 完成事件 + 明细查询 | `ab.run.completed` 家族事件（与落盘正交）+ `abRun(runId)` verdict 面回读 | [spec 75](docs/spec/75-ab-run-events.md) / [76](docs/spec/76-ab-run-detail-query.md) |
| | 活跃 run gauge | `buzhou.eval.runs.active`（tag kind）在飞计数，runId/close 双幂等 | [spec 77](docs/spec/77-run-registry-gauge.md) |
| | 评估回归门 | `EvalGate.enforce`——CI 一行判定（error 计入分母从严 + 失败预览） | [spec 80](docs/spec/80-eval-gate.md) |
| | run 对比 diff | 四态迁移（REGRESSION/FIX/稳定态）+ 数据集漂移显形 + netDelta | [spec 81](docs/spec/81-run-diff.md) / [82](docs/spec/82-dataset-fingerprint.md) |
| | Ragas/G-Eval 数值评估 | faithfulness/answerRelevancy 连续分 + 自定义维度打分（S x/y 协议） | [spec 87](docs/spec/87-ragas-evaluators.md) / [89](docs/spec/89-geval-dimension.md) |
| | run JSONL 导出（eval+AB） | 汇总列反规范化一行一 JSON——质量-行为 OLAP 联合分析 | [spec 88](docs/spec/88-eval-run-jsonl-export.md) / [94](docs/spec/94-ab-run-jsonl-export.md) |
| 存储与索引 | 键序区间扫描 SPI | `scanByKeyRange` 三栈下推（JDBC ORDER BY / Redis 键侧过滤）——时间编键结构底座 | [spec 78](docs/spec/78-key-range-scan.md) / [98](docs/spec/98-redis-key-range.md) |
| | outbox due 索引 | `due.<零垫ts>` 双写 + 键序区间调度 + 自愈 + 审计（投递停摆提前可见） | [spec 79](docs/spec/79-outbox-due-index.md) / [96](docs/spec/96-due-index-audit.md) |
| 韧性与隔离 | agent 并发 Turn 隔离舱 | per-agent 信号量（spawn 闸限会话数、本舱限 Turn 数正交）；resilience4j Bulkhead 借鉴 | [spec 84](docs/spec/84-agent-bulkhead.md) |
| 观测与运维 | 错误签名聚类 + 健康面 | 归一化折叠成有界族（Sentry fingerprint）+ `/actuator/buzhou` top 段 | [spec 83](docs/spec/83-error-signatures.md) / [85](docs/spec/85-error-signatures-health.md) / [92](docs/spec/92-bulkhead-health.md) |
| | 配置体检 doctor | 拼错键近邻建议 + 值域越界 ERROR（启动期一次，只读） | [spec 91](docs/spec/91-config-doctor.md) |
| | 会话归档冷层 | 删除前三槽快照冷存 + restore 回放（fail-fast 不删安全网） | [spec 97](docs/spec/97-session-archiver.md) |
| 护栏 | 工具输出 PII 脱敏 | 5 型规则式（校验位收窄误报）+ `[PII:TYPE]` 占位符（Presidio 借鉴） | [spec 86](docs/spec/86-pii-redaction.md) |
| 记忆 | 语义漂移触发压缩 | 话题漂移提前折入摘要（词面 SPI 可换 embedding）；trigger 溯源事件 | [spec 90](docs/spec/90-semantic-drift-compaction.md) / [95](docs/spec/95-summary-folded-trigger.md) |

## 生产级纵深（effort #56–#83 增量）

50 轮自迭代会话后半程的精选主线（详设 spec 99–121）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 评估闭环 | A/B 胜率门 + 版本查询 | 换版验收一行判定（error 不入分母）+ run/AB run 按数据集版本聚合 | [spec 101](docs/spec/101-ab-win-rate-gate.md) / [113](docs/spec/113-runs-of-version.md) / [114](docs/spec/114-ab-runs-of-version.md) |
| | 数据集快照 + run 对比 | 冻结版本（原 id 复制指纹一致）+ 四态迁移 diff | [spec 100](docs/spec/100-dataset-snapshot.md) / [81](docs/spec/81-run-diff.md) |
| 护栏 | 用户输入 PII 脱敏 + 自定义规则 | beforeTurn 占位符化 + 领域命名正则（Presidio PatternRecognizer） | [spec 106](docs/spec/106-pii-input-redaction.md) / [118](docs/spec/118-custom-pii-rules.md) |
| 运维与治理 | 配置体检 v2 + 健康段 | 跨键矛盾规则（NOOP 空转/孤儿依赖）+ config-doctor 健康缓存 | [spec 115](docs/spec/115-doctor-cross-key-rules.md) / [107](docs/spec/107-config-doctor-health.md) |
| | 会话归档治理 | 删除前三槽冷存 + restore 回放 + TTL 清理 + 审计详情 | [spec 97](docs/spec/97-session-archiver.md) / [103](docs/spec/103-archive-ttl-purge.md) / [120](docs/spec/120-archive-detail-query.md) |
| | 一致性工具 | outbox due 索引审计（孤儿/陈旧/缺失）——投递停摆提前可见 | [spec 96](docs/spec/96-due-index-audit.md) |
| | webhook 订阅过滤 | include-types 命中才入队（被滤不占容量） | [spec 105](docs/spec/105-webhook-type-filter.md) |
| 观测 | 错误签名闭环 | tool+model 双族 + 健康段 + JSONL 导出 + 窗口化清零 | [spec 83](docs/spec/83-error-signatures.md)–[85](docs/spec/85-error-signatures-health.md) / [112](docs/spec/112-signatures-jsonl-export.md) / [121](docs/spec/121-signatures-reset.md) |
| | gzip 导出族 + 时长遥测 | 观测 gzip 三入口（全量/增量/单会话）+ 工具/评估 run 时长 timer | [spec 109](docs/spec/109-observability-gzip-export.md) / [119](docs/spec/119-session-gzip-export.md) / [108](docs/spec/108-tool-duration-timer.md) / [111](docs/spec/111-eval-run-duration-timer.md) |
| | 技能双遥测 | 目录注入（截断率）+ skill_search 三态命中率 | [spec 110](docs/spec/110-catalog-telemetry.md) / [116](docs/spec/116-skill-search-telemetry.md) |
| 韧性 | bulkhead 拒绝计数 | per-agent 进程内表 + 健康 topRejected——限流风暴定位 | [spec 117](docs/spec/117-bulkhead-rejection-stats.md) |

## 生产级纵深 II（effort #86–#144 增量）

第二期自迭代会话（双会话并行 A/B 分工）的精选主线（详设 spec 122–200）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 并发与背压 | superstep 原子批 + spawn 优先级 | harness 前检整批不派发（BATCH_ABORTED 可区分未执行）+ 三级抢占排队 / 完成序快速失败通用原语 | [spec 122](docs/spec/122-atomic-superstep-batch.md) / [123](docs/spec/123-spawn-priority.md) / [122A](docs/spec/122-superstep-batch.md) |
| | 重试预算 | 配额随流量百分比累积——上游故障时重试自动勒紧防雪崩（Finagle） | [spec 178](docs/spec/178-retry-budget.md) |
| 预算与成本 | 虚拟 key 配额闭环 | yml 两键装配 + 跨会话扣减 + 耗尽拦截 + 健康段（LiteLLM） | [spec 124](docs/spec/124-virtual-keys.md) / [148](docs/spec/148-key-budget-gate.md) / [158](docs/spec/158-vkeys-yml.md) / [154](docs/spec/154-vkeys-health.md) |
| | 模型成本台账 | 零配置自动入账 + 排行/总数/JSONL 双口径账单 + 健康段（WandB/Langfuse） | [spec 174](docs/spec/174-model-cost-ledger.md) / [176](docs/spec/176-ledger-wiring.md) / [188](docs/spec/188-cost-export.md) / [190](docs/spec/190-cost-health.md) |
| 韧性观测 | 轮次心跳三件套 | 表 + 钩子自动打点 + 巡检犬（「活着但不动」可见，Temporal/K8s） | [spec 138](docs/spec/138-turn-heartbeat.md) / [152](docs/spec/152-heartbeat-hook.md) / [162](docs/spec/162-stall-watchdog.md) |
| | 多实例单跑 | 文件咨询锁（抢/还/陈旧回收）+ 清理与巡检犬接锁档（ShedLock） | [spec 182](docs/spec/182-advisory-file-lock.md) / [184](docs/spec/184-purge-lock.md) / [186](docs/spec/186-watchdog-lock.md) |
| 评估与合规 | 数据集期望门禁 | 四内置 + 自定义行级期望，run 前脏数据零 token 出局 + 宽松档（Great Expectations） | [spec 134](docs/spec/134-dataset-expectations.md) / [150](docs/spec/150-runner-expectation-gate.md) / [198](docs/spec/198-warn-gate.md) |
| | PII 合规三面 | 双侧命中排行 + JSONL 报表 + 输入侧接线（Presidio 口径） | [spec 144](docs/spec/144-pii-hit-stats.md) / [166](docs/spec/166-pii-report-export.md) / [164](docs/spec/164-input-pii-stats.md) |
| 观测治理 | 尾采样 + 清单导出 | 错误/慢会话全留 + 确定性哈希留样（OTel）+ manifest 六列目录（git pack） | [spec 136](docs/spec/136-tail-sampling-export.md) / [146](docs/spec/146-export-manifest.md) |
| | tag 基数守卫 + 目录渲染缓存 | 「tag 有界」从纪律变机制（opt-in 装配，Loki）+ 内容寻址渲染命中（vLLM radix） | [spec 132](docs/spec/132-tag-cardinality-guard.md) / [160](docs/spec/160-guard-install.md) / [168](docs/spec/168-render-cache.md) |
| | 技能热度账 | load 打点 + 排行/零使用清单 + 窗口报表（Backstage catalog score） | [spec 140](docs/spec/140-skill-usage-stats.md) / [170](docs/spec/170-skill-report-export.md) |
| 基础设施 | 租户隔离 + 归档定时 | tenants/<t> 沙箱严格收窄 + purge 三键默认关定时清理（Milvus/S3） | [spec 125](docs/spec/125-tenant-sandbox.md) / [130](docs/spec/130-archive-purge-job.md) |
| 质量 | 性质测试层 | 五不变量 × 随机输入（jqwik 思想零依赖）+ starter 全量验证轮 | [spec 180](docs/spec/180-property-invariants.md) / [200](docs/spec/200-starter-verify.md) |

## 生产级纵深 III（B 会话 200 系增量）

B 会话（.wayfinder200+ 号段）的精选主线（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 工具韧性 | 工具级熔断 + 幂等重试 | 失败率滑窗跳闸/冷却/半开探测（resilience4j）+ 只读工具异常静默退避重试（Temporal） | [spec 131](docs/spec/131-tool-circuit-breaker.md) / [133](docs/spec/133-idempotent-tool-retry.md) |
| | 在飞合并 + 轮内 memo | 同键并发折叠一次执行扇出（Hystrix collapsing）+ 同轮复读秒回首轮值（request caching） | [spec 139](docs/spec/139-tool-call-coalescer.md) / [147](docs/spec/147-turn-memo.md) |
| | 工具健康探测 | 宿主探针周期探活，翻转才通知——挂了提前知道（Consul） | [spec 165](docs/spec/165-tool-health-probe.md) |
| 模型韧性 | 对冲请求 + 端点离群驱逐 | 长尾并发押注先回先得（gRPC hedging）+ 连错端点逐出备选池窗口复池（Envoy） | [spec 137](docs/spec/137-hedged-model.md) / [149](docs/spec/149-model-outlier-ejection.md) |
| 会话治理 | 会话检疫 + 优雅排水 | 连败指数退避隔离（Erlang supervisor）+ 维护下线拒新等旧排空（K8s drain） | [spec 143](docs/spec/143-session-quarantine.md) / [155](docs/spec/155-session-drain.md) |
| | spawn 优先级 + 自适应舱 | 三级抢占排队同级 FIFO（OS 多级队列）+ AIMD 动态并发上限（TCP/HPA） | [spec 123](docs/spec/123-spawn-priority.md) / [145](docs/spec/145-adaptive-bulkhead.md) |
| 缓存与去重 | 共享 Redis 语义缓存 | 桶 HASH + 客户端 cosine 最近邻跨实例命中（RediSearch 语义可移植实现） | [spec 125](docs/spec/125-redis-semantic-vector-cache.md) |
| 护栏 | PII yml 声明式 + 角色权限 | custom-rules 两形态装配期 fail-fast + 角色通配面 fail-closed（K8s RBAC） | [spec 129](docs/spec/129-pii-yml-custom-rules.md) / [141](docs/spec/141-tool-role-permissions.md) |
| | 凭证租约 | 密钥 TTL 签发/续租/吊销——泄漏面从永久缩到 TTL 内（Vault） | [spec 153](docs/spec/153-secret-leases.md) |
| 投递可靠 | outbox 滞后面 + 序号围栏 | 最老积压 age（含退避中）stalled 判定 + 信封单调 seq 缺口显形（Kafka） | [spec 135](docs/spec/135-outbox-lag.md) / [159](docs/spec/159-delivery-seq-fence.md) |
| | 多 sink 扇出 | N 目的地独立投递语义/outbox 隔离/类型路由（Kafka 多消费组） | [spec 151](docs/spec/151-webhook-fanout.md) |
| 观测与预算 | 舱集群聚合 + 会话特征 | 心跳共享事实双列快照（spec57 范式）+ 行为侧写一次定义多处消费（Feast） | [spec 127](docs/spec/127-bulkhead-cluster-aggregation.md) / [161](docs/spec/161-session-features.md) |
| | 弹性预算池 + 配置热重载 | 保底配额+surplus 借用（Spark AQE）+ volatile 换引用版本化订阅（Caddy） | [spec 157](docs/spec/157-elastic-budget-pool.md) / [163](docs/spec/163-reloadable-config.md) |
| 防泛洪 | 输入泛洪防护 + per-tool 配额 | 相同输入窗口计数超阈拦（网关风暴防护）+ 单工具会话内上限（per-API quota） | [spec 167](docs/spec/167-input-flood-guard.md) / [185](docs/spec/185-tool-session-quota.md) |
| 工具治理 | 结果裁剪 + 跨轮 TTL 缓存 | fail-open 变换提炼字段（VRL/jq）+ 只读工具 maxAge 复用窗（HTTP max-age） | [spec 169](docs/spec/169-tool-result-transform.md) / [183](docs/spec/183-tool-ttl-cache.md) |
| | 工具泳道 + 目录漂移看门狗 | 慢工具独立泳道许可（Hystrix 舱）+ 指纹基线变化即事件（SBOM 闭环） | [spec 173](docs/spec/173-tool-lanes.md) / [201](docs/spec/201-catalog-drift-watcher.md) |
| 模型路由 | 平滑加权分流 + 影子读 | Nginx smooth WRR 比例精确分布平滑 + 采样旁路对照主影子（Istio mirror） | [spec 199](docs/spec/199-weighted-router.md) / [189](docs/spec/189-shadow-probe.md) |
| | 降级链演练 + 目录指纹 | 备胎主动验证持证上岗（Envoy 健康检查）+ 工具面 SBOM 对账 | [spec 195](docs/spec/195-fallback-drill.md) / [175](docs/spec/175-tool-catalog-fingerprint.md) |
| 可靠性闭环 | 导出防篡改清单 + 事件去重 | 逐项摘要+总摘要 verify 三列（OCI manifest）+ 发射侧指纹环拦截重复 | [spec 193](docs/spec/193-export-manifest.md) / [203](docs/spec/203-event-dedup.md) |
| | 序号围栏 + 凭证租约 | 信封单调 seq 缺口显形（Kafka）+ 密钥 TTL 签发续租吊销（Vault） | [spec 159](docs/spec/159-delivery-seq-fence.md) / [153](docs/spec/153-secret-leases.md) |
| 观测治理 | 轮时延计时 + 上下文水位 | 端到端轮 timer+滚动窗读数 + 低水位翻转事件预警线 | [spec 191](docs/spec/191-turn-timing.md) / [181](docs/spec/181-context-watermark.md) |
| | 空闲水位 + 配置指纹 | 特征仓事实驱动的空闲判定（Flink watermark）+ 配置面漂移对账 | [spec 179](docs/spec/179-idle-session-monitor.md) / [187](docs/spec/187-config-fingerprint.md) |
| 运行闸 | 模型调用循环闸 + 维护模式门 | 轮内调用总闸费用硬顶 + 全局温和拒新维护窗（K8s cordon） | [spec 197](docs/spec/197-model-call-cap.md) / [205](docs/spec/205-maintenance-gate.md) |
| 治理与验证 | 降级链单窗 + 事件 schema 门 | 三源合成备胎全景（Grafana 单窗）+ payload 必备键 fail-closed | [spec 207](docs/spec/207-fallback-chain-view.md) / [209](docs/spec/209-event-schema-check.md) |
| | webhook 全链 E2E + spec 覆盖门 | 五件叠链五联断言（集成证明）+ README↔spec 双向完整性测试 | [spec 211](docs/spec/211-webhook-pipeline-e2e.md) / [213](docs/spec/213-spec-coverage-gate.md) |

### spec 全量索引补录

分组表按「精选不穷举」引用；其余详设全清单在此按文件名可寻（覆盖门 213 保证
本索引与 docs/spec 双向一致）：

[23-ops-api-final](docs/spec/23-ops-api-final.md)、[33-index-hardening](docs/spec/33-index-hardening.md)、
[34-golden-and-compaction-events](docs/spec/34-golden-and-compaction-events.md)、[35-resilience-skills-input](docs/spec/35-resilience-skills-input.md)、
[36-export-extensions-dashboard](docs/spec/36-export-extensions-dashboard.md)、[37-search-replay-retention](docs/spec/37-search-replay-retention.md)、
[38-migrator-golden-redteam](docs/spec/38-migrator-golden-redteam.md)、[39-observability-health-redteam](docs/spec/39-observability-health-redteam.md)、
[40-static-security-determinism](docs/spec/40-static-security-determinism.md)、[41-audit-rotation-clock](docs/spec/41-audit-rotation-clock.md)、
[42-migrator-guard-read-degrade](docs/spec/42-migrator-guard-read-degrade.md)、[43-command-limits-config-validation](docs/spec/43-command-limits-config-validation.md)、
[44-drain-and-observability](docs/spec/44-drain-and-observability.md)、[45-golden-redteam-perf](docs/spec/45-golden-redteam-perf.md)、
[46-stream-observability](docs/spec/46-stream-observability.md)、[47-mdc-feedback](docs/spec/47-mdc-feedback.md)、
[48-feedback-export-canary](docs/spec/48-feedback-export-canary.md)、[49-shadow-pool-quota](docs/spec/49-shadow-pool-quota.md)、
[50-error-codes-jitter](docs/spec/50-error-codes-jitter.md)、[51-defenses-4](docs/spec/51-defenses-4.md)、
[93-ab-run-fingerprint](docs/spec/93-ab-run-fingerprint.md)、[99-summary-fold-counters](docs/spec/99-summary-fold-counters.md)、
[102-archive-health](docs/spec/102-archive-health.md)、[104-model-error-signatures](docs/spec/104-model-error-signatures.md)、
[126-prefix-cache](docs/spec/126-prefix-cache.md)、[142-doctor-staleness](docs/spec/142-doctor-staleness.md)、
[171-summary-provenance](docs/spec/171-summary-provenance.md)、[172-archive-matrix](docs/spec/172-archive-matrix.md)、
[177-event-payload-redaction](docs/spec/177-event-payload-redaction.md)、[192-cost-health-bean](docs/spec/192-cost-health-bean.md)、
[194-gzip-resetall](docs/spec/194-gzip-resetall.md)、[196-topkind-stalledsince](docs/spec/196-topkind-stalledsince.md)、
[202-readme-archive](docs/spec/202-readme-archive.md)、[204-fold-counter](docs/spec/204-fold-counter.md)、
[206-bulk-deposit](docs/spec/206-bulk-deposit.md)、[208-properties-2](docs/spec/208-properties-2.md)、
[210-runbook](docs/spec/210-runbook.md)、[212-common-defaults](docs/spec/212-common-defaults.md)、
[214-clearall-usageall](docs/spec/214-clearall-usageall.md)、[216-fog-ledger](docs/spec/216-fog-ledger.md)、
[218-ab-ledger](docs/spec/218-ab-ledger.md)、[220-preverify](docs/spec/220-preverify.md)、
[222-session-close](docs/spec/222-session-close.md)。

## 生产级纵深 IV（C 会话 300 系增量）

C 会话（.wayfinder300+ 号段）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 执行脊柱 | 批内工具合并接线 | 批内同工具同参执行一次、全位共享值逐位重写 id（Hystrix collapsing 装配收尾） | [spec 300](docs/spec/300-batch-coalescing.md) |
| 模型韧性 | 对冲装配面 | yml 三行声明长尾并发押注（gRPC hedging——137 原语装配收尾，@Primary 升位） | [spec 301](docs/spec/301-hedge-assembly.md) |
| 背压 | 重试预算接线 | 模型/工具重试前进程级预算支取、拒即原错上抛（Finagle retry budget——178 原语装配收尾） | [spec 302](docs/spec/302-retry-budget-wiring.md) |
| 投递可靠 | 序号围栏持久纪元 | 发送方每启持久递增 epoch 显式声明重启（Kafka producer epoch——RESET 不再靠猜，旧纪元迟到判 STALE） | [spec 303](docs/spec/303-sequence-fence-epoch.md) |
| 持久化 | 事务批补偿 | 跨步多写 saga 倒序补偿、补偿失败即停（归档条目即 undo log——部分失败净回原状） | [spec 304](docs/spec/304-compensating-batch.md) |
| 工具治理 | 健康探测装配 + 熔断 yml 面 | 探测原语 165 装配收尾（bean+周期+健康面，Consul）；熔断 hook 四参 yml 化（resilience4j，配置面族首项） | [spec 305](docs/spec/305-tool-health-assembly.md) / [306](docs/spec/306-tool-circuit-yml.md) |
| 投递可靠 | 事件 schema yml 声明 | per-type 必备键 yml 化 + fail-open 观察模式 + 挂点去重防双投（JSON Schema required——209 装配收尾） | [spec 307](docs/spec/307-event-schema-yml.md) |
| 执行脊柱 | deadline 跨工具传播 | ToolContext 携带 TurnDeadline 动态视图——自限工具读实时剩余收敛（gRPC 逐跳传播） | [spec 308](docs/spec/308-deadline-propagation.md) |
| 模型韧性 | 影子对照明细 JSONL | shadow.compared 事件逐条落盘、节选封顶、IO 吞计（W&B lineage——detail-path 声明即导出） | [spec 309](docs/spec/309-shadow-jsonl-export.md) |
| 记忆治理 | 空闲会话后台压缩 | 索引 lastActiveAt 事实驱动的空闲窗口自动瘦身（LSM compaction 思想——限批+隔离+默认关） | [spec 310](docs/spec/310-idle-compaction.md) |
| 会话分支 | 时间旅行 fork | 从任意轮重走分支：历史前缀复制、摘要不复制防未来泄漏（LangGraph checkpointer time-travel） | [spec 311](docs/spec/311-time-travel-fork.md) |
| 运维 | 健康告警规则 | yml 声明「机制 DOWN 持续 for 窗即通知」双向触发/恢复、flap 吸收（Grafana ruler） | [spec 312](docs/spec/312-alert-rules.md) |
| 护栏 | PII 命中分侧 | 输入（预防提示面）与输出（脱敏规则面）分列统计+JSONL 双列（Presidio 分侧深化） | [spec 313](docs/spec/313-pii-hit-sides.md) |
| 成本预算 | 价目快照随单 | 账单行自含记账时单价两列——调价后旧账可离线复算（复式记账审计口径） | [spec 314](docs/spec/314-pricing-snapshot.md) |
| 成本预算 | 虚拟 key 配额共享 | Lua 原子扣减跨实例一份额度、后端不可达 fail-closed（Redisson 分布式限额——N 实例≠N 倍烧钱） | [spec 315](docs/spec/315-virtual-keys-redis.md) |
| 工具治理 | 泳道许可共享 | Lua 原子取/还跨实例一套泳道容量、满道超时同词汇（Redisson 分布式信号量——fog 227 共享族收口） | [spec 316](docs/spec/316-lane-redis-shared.md) |
| 观测治理 | 导出族合流打包 | 多 JSONL 报表一个 ZIP + manifest 行数/sha256 清单、单源故障隔离（tarball/OCI artifact——窗口一揽子） | [spec 317](docs/spec/317-export-bundle.md) |
| 运维 | 会话扰乱预算 | voluntary 排水领额度、min-available 保底防全排（K8s PodDisruptionBudget——维护三件套齐） | [spec 318](docs/spec/318-session-disruption-budget.md) |
| 运维 | 舱压伸缩建议 | 舱拒绝窗口增量→实例倍率建议（clamp/回零回落/只建议不执行——K8s HPA custom metrics） | [spec 319](docs/spec/319-bulkhead-scaling-advisor.md) |
| 运维 | 舱容量热调整 | resize 扩/缩/热加/摘在飞不扰、配置刷新事件热重读 yml 容量不重启（Spring Cloud rebind / resilience4j ResizableSemaphore） | [spec 320](docs/spec/320-bulkhead-hot-resize.md) |
| 观测治理 | SLO 错误预算燃尽率 | 桶环窗错误率→burn=rate/(1−SLO)、min-samples 防噪、观察钩子喂数、健康面接告警 for 窗（Google SRE 错误预算） | [spec 321](docs/spec/321-error-budget-burn-rate.md) |
| 韧性演练 | 工具混沌注入 | 按概率延迟/故障袭击（结构化标记模型可改道）、include 清单、运行时启停（Netflix Chaos Monkey——熔断/预算/舱平时真枪演练） | [spec 322](docs/spec/322-tool-chaos-injection.md) |
| 韧性演练 | 干跑拦截/执行计划 | 拦入计划不执行（非错误标记不污染熔断/预算）、args 快照有界计划面、运行时启停（Terraform plan/apply——先计划后执行） | [spec 323](docs/spec/323-dry-run-plan.md) |
| 流量治理 | 工具金丝雀发布 | 同名两实现按权重分流真跑、劣化超容差一次性粘性自动回滚（Istio/Flagger canary——小流量真曝+自动退） | [spec 324](docs/spec/324-tool-canary.md) |
| 事故响应 | 工具紧急停用开关 | 一键全局停/恢复不重启、非错误标记零污染、刷新事件 yml 事实源整体覆盖（LaunchDarkly kill switch / K8s cordon） | [spec 325](docs/spec/325-tool-kill-switch.md) |
| 失控防护 | 轮次重复检测 | 相邻输出词元 Jaccard run 达窗即fire 一次、opt-in 解困回填替换复读输出（context rot / LLM 打转早信号） | [spec 326](docs/spec/326-turn-repetition.md) |
| 失控防护 | 工具循环断路器 | 同工具同参数连续调用达窗即拦、持续干预直到换参/换工具、三选一出路回填（与熔断正交——按调用形态断路） | [spec 327](docs/spec/327-tool-loop-breaker.md) |
| 观测治理 | 干跑计划 JSONL 导出 | 一行一 PlannedCall、args 字符串快照人审口径、dropped 截断尾行（导出族新员——计划单落盘流转） | [spec 328](docs/spec/328-dry-run-plan-export.md) |
| 工程治理 | API 快照收口 | 快照机跨平台修复（Windows 首次真比对/再生）+38 型全量入档 api-surface（半程防线补齐） | [spec 329](docs/spec/329-api-snapshot-closure.md) |
| 运维 | 告警静默窗与抑制规则 | 维护窗/事故一键静默（机制匹配+惰性过期）、根因 firing 遮蔽衍生通知（吞通知不吞事实+留痕计数——Alertmanager silence/inhibit） | [spec 330](docs/spec/330-alert-silence-inhibit.md) |
| 运维 | 后台任务选主 | 家务族（保留清理）跨实例单执行者：TTL 租约+续期+单调纪元围栏+停机让位（K8s leader election/etcd lease——失联宁可少做不可抢做） | [spec 331](docs/spec/331-leader-election.md) |
| 运维 | 健康三探针分层 | 机制归 liveness（重启能治）/readiness（摘流量能治，缺省归类）/startup（等待热身）三类独立裁决+端点（K8s probes——探针用错轻则无效重则重启风暴） | [spec 332](docs/spec/332-probe-classes.md) |
| 安全 | 消息静态信封加密 | AES-GCM 应用层加密——密钥不出进程存储只见密文、AAD 绑定标识防剪贴、双钥轮换窗口、旧明文透传（Vault transit/KMS envelope） | [spec 333](docs/spec/333-message-encryption.md) |
| 安全 | 摘要槽信封加密 | 333 同通道扩散——长期记忆浓缩面（sections 全量密文、版本以底层 UPSERT 为准）；state 槽 CAS 比值面不兼容为诚实边界 | [spec 336](docs/spec/336-summary-encryption.md) |
| 执行脊柱 | 工具上下文行李 | tenant/env 等路由元数据经 ToolContext 带外直达工具（不进提示词模型不可见）、yml 播种+运行时 API、有界封顶（W3C Baggage/OTel baggage） | [spec 337](docs/spec/337-tool-baggage.md) |
| 成本预算 | 预算软预警线 | 会话总量/成本/虚拟 key 三维消耗达硬顶 80% 即发预警事件（一次一发仅事件不拦截，-1 关闭——AWS Budgets 到墙之前先叫人） | [spec 338](docs/spec/338-budget-warning.md) |
| 模型韧性 | 多模型加权路由 | 199 平滑加权原语装配收尾——逐调用按权重分流多模型（比例精确时间平滑 5:1:1 不连五爆发）、yml 按 bean 名配权、@Primary 透明接入（LiteLLM Router） | [spec 339](docs/spec/339-model-routing.md) |
| 运维 | 路由权重热调整 | 刷新事件重读 yml 逐路热调（WRR 动量保留自然收敛）、面外名字跳过不红、运行时 setWeight API（Spring Cloud rebind/320 同模式） | [spec 340](docs/spec/340-routing-weights-hot-reload.md) |
| 运维 | 选主扩散：归档清理与空闲压缩 | 331 选主门接 ArchivePurgeJob 与 IdleCompactionHousekeeper——家务族三任务一个 leader（K8s leader election 扩散轮：不重复扫/不竞写摘要版本） | [spec 341](docs/spec/341-leader-diffusion.md) |
| 运维 | 维护窗口 cordon | 地板多源合成（与 335 冻结正交不互踩）+ yml 声明窗自动 cordon/解除 + 运行时按钮（K8s cordon——窗内不接新会话在途排空，过期窗不追溯） | [spec 342](docs/spec/342-maintenance-cordon.md) |
| 工程治理 | 生效配置自描述端点 | /actuator/buzhou-config 一屏全部 buzhou.* 生效值（含 env 覆盖）、密钥类键宽匹配掩码宁掩勿漏（actuator configprops+sanitization） | [spec 343](docs/spec/343-config-endpoint.md) |
| 护栏 | 审计链完整性巡检 | 可读≠完整——链哈希/签名断链即 DOWN 定位首断点、超窗 UNKNOWN 带修法、自动入 312 告警/332 探针（CT log/区块链全节点验证——事实源失守不被绿掩盖） | [spec 344](docs/spec/344-audit-chain-health.md) |
| 观测治理 | 告警面板端点 | /actuator/buzhou-alerts 一屏 firing 规则+生效静默窗+抑制视图（312/330 状态聚合，缺席段诚实空——Alertmanager UI） | [spec 345](docs/spec/345-alerts-dashboard.md) |
| 观测治理 | 会话面板端点 | /actuator/buzhou-sessions 活跃会话计数（索引分页 50k 封顶）+准入地板多源（谁抬着）+cordon 态（面板三部曲之三） | [spec 346](docs/spec/346-sessions-dashboard.md) |
| 运维 | 告警注解随发 | 规则声明 runbook-url/summary 等注解随触发/恢复通知与面板直达值班端（Alertmanager annotations——收到即可行动） | [spec 347](docs/spec/347-alert-annotations.md) |
| 背压 | 重试预算健康面 | 恒 UP（拦截=保护生效非故障）+余量/已取/被拦快照入聚合健康面（Finagle 预算水位——背压族观测收口：denied 增长=重试风暴被挡的可见信号） | [spec 348](docs/spec/348-retry-budget-health.md) |
| 工程治理 | C 会话收官终验 | 全 reactor 串行终验+快照/覆盖门复验+台账归档（50/50——R30 半程收口同型） | [spec 349](docs/spec/349-session-closeout.md) |
| 成本预算 | 成本归因台账 | 同一笔 microUsd 双维入账（model+虚拟 key）——chargeback 有账面；无 key 诚实桶、万分比 share、JSONL 报表（Kubecost/OpenCost 按标签归因） | [spec 334](docs/spec/334-cost-attribution.md) |
| 背压 | 错误预算政策·烧穿自动降级 | 预算烧穿→spawn 准入地板抬 HIGH（冻结 NORMAL/LOW 只保关键通道）、连续两轮清明解冻防抖（Google SRE error budget policy——从「知道」到「行动」） | [spec 335](docs/spec/335-error-budget-freeze.md) |

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
   ┌────────┬──────────┬────┴─────┬──────────┬──────────┬──────────┬───────────┐
buzhou-   buzhou-   buzhou-    buzhou-    buzhou-    buzhou-    buzhou-    buzhou-
memory    spill     observability skills   mcp        guard      tools      resilience
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
| `buzhou-resilience` | 模型韧性（重试/退避/错误分类/统一超时/限流/熔断降级/响应缓存） | `buzhou.resilience.enabled` | 开 |
| `buzhou-store-jdbc` | JDBC 持久化实现 | `buzhou.store.type=jdbc` | — |
| `buzhou-store-redis` | Redis 持久化实现 | `buzhou.store.type=redis` | — |
| `buzhou-spring-boot-starter` | 依赖聚合 starter（无代码） | — | — |
| `buzhou-bom` | 版本 BOM | — | — |

> safe-by-default：多数机制默认开启；otel / dashboard 这类需要外部后端或端口的默认关闭。

## 生产级纵深 V（D 会话 400 系增量）

D 会话（effort #400+ 号段）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 安全 | 密钥扫描护栏 | 三缝 MASK（输入/出站工具参数/工具结果）——API key/token/JWT/PEM 七型签名占位符化，凭据不进 prompt 观测与外发（gitleaks pre-commit 思想） | [spec 400](docs/spec/400-secret-scanning.md) |
| 工程治理 | 提示词注册表 | 版本+标签双轴——publish 单调版本、production/latest 指针可移动（晋级/回滚同一动作）、按版钉取复现历史、yml 播种重启幂等（Langfuse prompt management） | [spec 401](docs/spec/401-prompt-registry.md) |
| 模型韧性 | 结构化输出执法 | JSON 契约（required+类型表+围栏剥离）验证失败把错误清单喂回模型自修复、耗尽抛违规、每次修复重过观测+韧性层（instructor） | [spec 402](docs/spec/402-structured-output.md) |
| 成本预算 | 成本预测外推 | 分钟桶速率环 + 水平线线性外推（窗内烧钱速率×horizon 对预算 projectedOver）——「会不会超预算」期中就有答案；记账监听缝喂数不二次算成本（AWS Budgets forecast） | [spec 403](docs/spec/403-cost-forecast.md) |
| 安全 | 审计 Merkle 根与包含证明 | 时点封印出紧凑根对外发布、单条记录凭包含证明+根即可验——第三方零全链验证；叶摘要与链 prev_hash 同基互证（Certificate Transparency） | [spec 404](docs/spec/404-audit-merkle.md) |
| 运维 | 健康事件时间线 | 机制状态变迁 diff-only 入环（首次初见/翻转/回归）+per-mechanism 计数辨抖动 + JSONL 落盘 + /actuator/buzhou-timeline——事故复盘「谁先坏的」有数据面（PagerDuty incident timeline） | [spec 405](docs/spec/405-health-timeline.md) |
| 工具治理 | 工具退役通告 | 退役描述前缀随定义下发（模型可见 steering）+调用事件/计数——迁移进度由 usage 数据说话、真删除时机不猜；退役≠移除不阻断（K8s API deprecation） | [spec 406](docs/spec/406-tool-deprecation.md) |
| 评测 | 在线采样入评测集 | 生产轮次确定性按率采样入集（hash(session:turn)%100 同轮同判可复现）、空白/短问过滤、fail-soft 绝不炸轮、provenance 与拉式回流同域天然去重（Honeycomb head sampling） | [spec 407](docs/spec/407-turn-sampling.md) |
| 成本预算 | 预算日历周期 | 月/周/日全进程账期预算——periodTag 入键翻页即隐式重置、tokens+成本双轨计量、下调用拦截、软预警一次一发（AWS Budgets calendar period） | [spec 408](docs/spec/408-period-budget.md) |
| 工具治理 | 工具结果 schema 校验 | per-tool 输出契约（复用入参同一校验器零新逻辑）——坏结果回灌前拦下转结构化反馈（标记词汇复用、文案区隔已执行），模型换参重试而非基于残缺数据瞎猜（MCP outputSchema） | [spec 409](docs/spec/409-tool-result-schema.md) |
| 记忆治理 | 共享事实库 ACL | 跨会话/跨 agent 共享事实——deny-by-default（owner 恒读、显式 grant 才可读、键即所有权抢键 fail-fast）、ttl 过期、拒绝读计数防探测（mem0 共享记忆+隔离） | [spec 410](docs/spec/410-shared-facts.md) |
| 背压 | 泳道优先级原语 | 优先级插队信号量（0-9 有界、数小者优先、同级 FIFO 防饿死、超时让位、不剥夺协作式）+等待快照饥饿可见——原语先行接线扩散候选（Envoy priority levels） | [spec 411](docs/spec/411-priority-lane.md) |
| 观测治理 | 时间桶预聚合 | 跨会话翻页枚举+TURN/MODEL/TOOL 三类 span 按 epoch 对齐固定桶聚合（turns/calls/errors/tokens）+空桶补齐图表连续+桶数上界——小时级趋势一查询即得（M3 downsampling） | [spec 412](docs/spec/412-rollups.md) |
| 观测治理 | 时间桶延迟分位数 | 桶内 TURN 时延 p50/p95/p99 exact 最近秩——「错误率正常但变慢了」的隐蔽退化可见；零样本桶 null 诚实空值不画零假象（Prometheus histogram_quantile） | [spec 416](docs/spec/416-rollup-percentiles.md) |
| 成本预算 | 价目热更新 | 刷新事件整表热载价目（覆盖层优先、删除键回落底表、逐键 WARN diff 留痕）——供应商调价即时生效新账用新价不重启（Spring Cloud rebind/Stripe 即时生效） | [spec 417](docs/spec/417-pricing-hot-reload.md) |
| 安全 | 秘密命中统计与导出 | 类型×缝三侧计数（INPUT/OUTBOUND/OUTPUT）+快照 JSONL 追加导出——泄漏面趋势（哪类凭据/哪条缝最常出）可分析（PiiHitStats 同构镜像） | [spec 418](docs/spec/418-secret-hit-stats.md) |
| 成本预算 | 周期预算健康面 | 恒 UP 观测面：双轨进度（used/limit/pct 截断）+exhausted 布尔+resetsAt 回血时刻（月=下月 1 日/周=epoch 周界/日=次日）——「烧到哪了/还有多久回血」一屏可答（AWS Budgets 面板） | [spec 419](docs/spec/419-period-budget-health.md) |
| 工具治理 | 工具目录 lint | 装配期三规则体检（名字约定/描述长度/跨源重名）——只报不改 WARN+计数+首装配事件，模型选工具的静态失败面上线前可见（ESLint 构建期 lint） | [spec 420](docs/spec/420-catalog-lint.md) |
| 安全 | 审计封印导出 | Merkle 封印逐行 JSONL 追加外存+每行即时建树 verified 比对——最新印期望 true、旧印 false 属正常（印后有新记录）、最新印 false=链被动过；宿主定时调用即 CT 式 STH 公示节奏 | [spec 421](docs/spec/421-audit-seal-export.md) |
| 背压 | 工具泳道优先级装配 | yml 声明泳道容量+per-tool 优先级即装配（0-9 数小者先拿许可、同级 FIFO）——交互工具拥挤时先于批处理工具执行，未声明泳道引用启动即红（Envoy priority levels 接线） | [spec 422](docs/spec/422-tool-lane-priority-assembly.md) |
| 评测 | 错误偏向采样 | 错误轮观察者缝采样入候选池（错误轮不走 afterTurn——onTurnStart 记输入/onTurnError 采错）——error-rate-percent 默认 100 全保、确定性 hash 同轮同判、占位 [TURN-ERROR] 留人工判 golden（OTel tail_sampling ERROR 规则） | [spec 423](docs/spec/423-error-biased-sampling.md) |
| 工程治理 | 提示词使用统计 | 注册表装饰器三 resolve 形态命中记账（latest/标签/钉版→name×version 次数）+快照 JSONL 追加导出——晋级/退役由使用数据说话（Langfuse prompt analytics） | [spec 424](docs/spec/424-prompt-usage-stats.md) |
| 背压 | 轮次限速 | beforeTurn 惰性令牌桶准入（burst 突发桶+每分钟匀速回填、无定时器）——默认 per-session 频次帽、可插拔键做租户整体帽，超限 block 不炸轮（nginx token bucket） | [spec 425](docs/spec/425-turn-rate-limit.md) |
| 背压 | 模型并发舱 | per-model 在飞并发上限（advisor 链 +660：许可在重试外获取一次、持有跨重试；流式 doFinally 释放含 CANCEL）——供应商并发 tier 的 429 上游根因消除，未配置模型 NOOP 零开销（Resilience4j SemaphoreBulkhead） | [spec 426](docs/spec/426-model-concurrency.md) |
| 观测治理 | 轮次错误回调对称化 | 非流式 chat 失败也回调 onTurnError（此前仅流式）——turn span 带 error 立即收口不悬到会话关闭、错误采样（423）非流式也采得到（OTel span status ERROR 语义正确性） | [spec 427](docs/spec/427-nondrain-error-callback.md) |
| 安全 | Webhook 验签与防重放 | 消费端常量时间验签工具（MessageDigest.isEqual 防时序侧信道）+时间戳容差窗重放有界（forwarder 加发 X-Buzhou-Timestamp 不进 MAC 存量零破坏）——签名↔验签两侧同 crypto 路闭环（Stripe signed webhooks） | [spec 428](docs/spec/428-webhook-verify-replay.md) |
| 背压 | 模型并发舱热更新 | refresh 事件重读 limits 热调容（扩容 grow/缩容 shrink 在飞不受扰、释放自然收敛不抢占；移除键摘舱）——供应商调并发额度改 yml 发事件即生效不重启（320/340 rebind 同模式） | [spec 429](docs/spec/429-model-concurrency-hot-reload.md) |
| 并发原语 | 延迟作业 | one-shot 到点执行（fireAt/delay 双形态）——同键重复提交=替换不双跑（键即幂等锚）、cancel 幂等、异常隔离计数、pending 升序快照（Sidekiq delayed_jobs） | [spec 413](docs/spec/413-delayed-jobs.md) |
| 工程治理 | 配置漂移审计 | 周期快照 buzhou.* 全属性 diff（值变更/新增/删除三语义）——变更留痕 WARN 日志+listener 回调+计数；敏感值与 343 同款末段掩码不外泄（ArgoCD drift detection） | [spec 414](docs/spec/414-config-drift.md) |
| 运维 | 会话黏性路由提示 | sha256(appId|sessionId) 确定性亲和键+桶位（跨实例零协调天然一致）——LB 哈希规则的事实源，面板行可见路由分布（Ketama 确定性键） | [spec 415](docs/spec/415-session-affinity.md) |

## 生产级纵深 VI（E 会话 500 系增量）

E 会话（effort #500+ 号段）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 安全 | 流式回复 PII 脱敏 | 模型回复出站第三缝（输入 106/工具输出 86 之外）——流式滑动窗口缓冲跨 chunk 实体不漏、flush 排空、占位符不拆分、非流式整段同滤（Presidio 流式匿名化+流式 WAF 回看窗口） | [spec 500](docs/spec/500-stream-pii-redaction.md) |
| 可靠性 | 请求幂等键 | 调用方供给 `buzhou.idempotency-key` advisor 参数——同键重入重放首次终态响应零二次调用（客户端超时重试不二次计费）、非终态不写半截、键缺席透传零行为（Stripe Idempotency-Key） | [spec 501](docs/spec/501-request-idempotency.md) |
| 模型韧性 | 模型能力注册表与能力门 | yml 声明每模型 vision/tools/context-window——media/工具请求路由前事前拦（ARGS_VALIDATION_FAILED 结构化异常带 yml 键指引）而非供应商 400 事后错，未注册模型零门（LiteLLM Router capabilities） | [spec 502](docs/spec/502-model-capability-gate.md) |
| 模型路由 | 时段路由窗口 | 同日时间窗自动切权重（窗权整表替换、出窗回落基础、WARN 留痕+applied/reverted 计数）——夜间切便宜模型白天回切零人工值守（K8s CronJob/Argo Rollouts schedule） | [spec 503](docs/spec/503-routing-schedule.md) |
| 工具韧性 | MCP 服务器级聚合熔断 | 一台 server 一个键的断路状态机（复用 core ToolCircuitBreaker）——server 宕机全部工具快速失败结构化改道信号、半开探测恢复、snapshot 观测面，与 per-tool 131 正交两层（Envoy per-host 聚合） | [spec 504](docs/spec/504-mcp-server-breaker.md) |
| 评测 | 在线实验分桶 | `buzhou.experiments.<exp>.<variant>` 权重声明——session 确定性分桶（sha256 mod100+字典序累积，跨实例零协调）+未入组余量+曝光计数（GrowthBook/Statsig） | [spec 505](docs/spec/505-experiment-buckets.md) |
| 护栏 | 工具入参限幅 | 执行前体积门（31 结果限幅的入站对称面）——超限拒绝回喂结构化反馈（不回显入参）引导精简重试、per-tool glob 覆盖、默认关 opt-in（nginx client_max_body_size） | [spec 506](docs/spec/506-tool-input-limit.md) |
| 安全 | 可逆 PII 代管库 | vaultize/restore 对称原语（稳定令牌 sha256|salt 前 16hex 去重+TTL+有界+fail-safe 保留过期令牌）——「展示层脱敏、服务端留原值」授权回显工作流（Presidio Vault） | [spec 507](docs/spec/507-pii-vault.md) |
| 成本预算 | 成本异常尖峰检测 | 滚动基线 z-score（当前分钟桶 vs 前 N 桶均/标差）+绝对地板+minSamples+cooldown 防抖——费率在预算内但相对自身基线突刺可见（与 403 forecast 互补：趋势 vs 突刺，Prometheus/Istio） | [spec 508](docs/spec/508-cost-spike.md) |
| 观测治理 | 时延 SLO 燃尽 | 「99% 轮次 < N s」坏事件=latency>阈值喂 321 ErrorBudget（burn/breaching/topBreaching 语义全继承）——「错误率正常但变慢了」的隐蔽退化用 SRE 语言可见可告警（Google SRE） | [spec 509](docs/spec/509-latency-slo.md) |
| 持久化 | store fsck 定时巡检 | StoreFsck 只读对账定时化（选主门 elector 缺席=无门单实例）——findings WARN+计数不自动修复，衰变在 restore 前可见（341 选主扩散第三弹） | [spec 538](docs/spec/538-fsck-housekeeper.md) |
| 持久化 | fsck 巡检健康面 | mechanism=store-fsck 观测面恒 UP——details 聚合 runs/totalFindings/lastFindings/skippedNotLeader，巡检结果从日志面升级到标准健康读数（538 健康面接入） | [spec 548](docs/spec/548-fsck-health.md) |
| 技能治理 | 技能正文规模审计 | 逐技能正文字符规模降序+预算超限标记+聚合统计——load_skill 载荷膨胀的静态审计面（110 目录预算 per-skill 深化），纯读数不拦截 | [spec 546](docs/spec/546-skill-body-audit.md) |
| Spill 治理 | spill 回读审计 | readRange 回读有界样本窗（uri/字节/完整性告警）+per-uri 计数降序+累计读数——热点证据与落盘衰变的排障读数面（60/67 导出族同构） | [spec 539](docs/spec/539-spill-read-audit.md) |
| 安全 | 会话导出加密 | seal/open 密文容器（版本标记头+333 EnvelopeCipher AES-GCM，AAD 用途域绑定防跨域剪贴）——敏感会话导出文件落盘/传输不泄露，错钥/篡改 DATA_CORRUPTION 带修法（age/OCI 加密 artifact） | [spec 510](docs/spec/510-encrypted-session-export.md) |
| 持久化 | 归档冷存完整性校验 | 写时 sha256 校验和随条目落盘（独立命名空间零污染）+verify 五态随时验（MISMATCH/存量 NO_CHECKSUM/CORRUPT 分列不冒充）——可读≠未被改，衰变/误写 restore 前发现（S3 checksum） | [spec 511](docs/spec/511-archive-integrity.md) |
| 安全 | 内容安全词表过滤 | yml 声明违禁词表（大小写不敏感 contains——CJK 无词界正确语义）+双缝（输入/工具结果）BLOCK 结构化告示或 MASK 打码——合规黑名单本地词表面（OpenAI moderation 规则子集） | [spec 515](docs/spec/515-content-moderation.md) |
| 安全 | 会话导出脱敏 | sanitize 不可变副本——消息/摘要/state 三内容域占位符化（86 检测器+custom rules），结构字段原样；与 510 组合先脱敏再封缄=对外分享全链（Presidio anonymize） | [spec 518](docs/spec/518-export-sanitizer.md) |
| 观测治理 | 工具调用图谱统计 | TOOL span 同轮相邻有向边计数+per-tool calls/errors/错误率排行——「哪些工具总被连着用」「哪个工具错误集中」有数据依据（LangSmith trace analytics） | [spec 519](docs/spec/519-tool-graph.md) |
| 观测治理 | span 状态分布读数 | kind×status 计数（大小写归一+UNSET 兜底+TreeMap 稳定序）——「MODEL 错误集中还是 TOOL 错误集中」一屏可读（Prometheus label 聚合；519 同包扩散） | [spec 543](docs/spec/543-span-status-distribution.md) |
| 排障 | guard 装配摘要读数 | `assemblySummary()` 列出装配的 hook 名（装配序）——「guard 到底挂了哪些钩子」支持包/排障一屏可读 | [spec 549](docs/spec/549-assembly-summary.md) |
| 收口 | E 会话收口终验 | 全反应堆串行终验+快照/覆盖门复验+台账归档（C spec 349 / D R30 同型；49 轮实质功能总览） | [spec 532](docs/spec/532-closeout.md) |
| 成本预算 | 评估 run 预算闸 | 逐项 input+expected 字符估算累计超限即早停——剩余项 error 三态 [RUN-BUDGET] 显式可见、run 照常落盘（partial 不冒充完整）；默认关（AWS Budgets/pytest maxfail） | [spec 520](docs/spec/520-eval-run-budget.md) |
| 工程治理 | 装配绑定审计修复 | 409 result-schemas/406 deprecated/505 experiments 单 Map 组件 record 构造绑定在 prefix.组件名 子路径——根前缀 yml 绑空静默 no-op，统一改根绑定直读+内容非空回归断言（R31 发现的系统性坑） | [spec 531](docs/spec/531-assembly-binding-audit.md) |
| 成本预算 | per-model 预算闸 | ModelCostLedger 记账面 vs yml per-model 预算（microUsd）——耗尽 beforeModel 拦截（结构化告示带修法），以记账面为准未喂账恒放行，map 非空才装配 | [spec 530](docs/spec/530-model-budget-gate.md) |
| MCP 韧性 | MCP 建连退避重试 | 建连失败按指数退避重排（base×2^n 封顶 60s、重试耗尽收口既有失败语义）——server 暂时不可达自愈而非刷新前永久缺席，yml `connect-retry` 声明即启用（Resilience4j retry） | [spec 524](docs/spec/524-connect-retry.md) |
| 评测 | 评估集合成扩增 | 种子用例→LLM 生成 N 条同语义改写候选（围栏剥离+逐行容错+零可解析异常带预览）——人审教义只产候选不入库（Ragas testset generation） | [spec 525](docs/spec/525-case-amplifier.md) |
| 评测 | error 项重试一次 | judge 抖动/闪断的 error 项自动重跑一次取第二次结果（detail [RETRIED] 留痕+计数）——语义 fail 不重试（不掩盖真实回归）、默认关（pytest flaky rerun） | [spec 535](docs/spec/535-error-retry-once.md) |
| 观测治理 | 水位告警桥接 | per-session 低水位翻转态聚合为 context-watermark 机制健康面（低水位会话数≥阈值 DOWN、详情聚合读数）——312 告警规则按机制名可订阅「容量压力」（181×312 桥接） | [spec 526](docs/spec/526-watermark-health.md) |
| 投递可靠 | webhook 载荷大小上限 | outbox 单条载荷体积门（默认不限 opt-in）——超限拒入队+oversized 计数可见，拒绝而非截断（截断 JSON 破坏消费端契约）（Kafka max message size） | [spec 533](docs/spec/533-max-payload.md) |
| 投递可靠 | 死信原因分类计数 | `buzhou.webhook.dead-reason` 计数（tag reason 有界：4xx|重试耗尽）——接收端配置错与瞬时故障治理动作分流（135 死信面观测扩散） | [spec 537](docs/spec/537-dead-reason-counter.md) |
| 投递可靠 | 死信 JSONL 导出 | 死信清单一行一 JSON（eventId/type/attempts/createdAt epoch，转义完备）——与 OLAP/归档管道同构搬运（60/67 导出族同构） | [spec 542](docs/spec/542-dead-letter-jsonl.md) |
| 工程治理 | 提示词版本行级 diff | 行级 LCS 最小变更集（equal/insert/delete+added/removed/net）+异名 fail-fast——晋级/回滚评审只看变化（Git diff；401 扩散） | [spec 534](docs/spec/534-prompt-version-diff.md) |
| 安全 | 跨会话泄漏金丝雀 | 会话专属确定性令牌（sha256|salt 前 8hex）+他令牌扫描探测——B 会话回复出现 A 金丝雀=跨会话污染信号，LRU 256 有界（thinkst canarytokens/honeytoken） | [spec 528](docs/spec/528-session-canary.md) |
| 投递可靠 | 签名双密钥轮换验签 | verifyWithRotation（current→previous，blank=null）×容差窗组合——密钥轮换窗口旧签名可验，生产/消费端不必原子同步换钥（Stripe 多签名密钥） | [spec 540](docs/spec/540-signature-rotation.md) |
| 工具韧性 | per-tool 超时预算覆盖 | glob 键 per-tool 超时替换全局值（Deadline 恒天花板）——慢工具长预算快工具紧预算差异化，Holder 默认空零变化（31 per-tool 覆盖同法） | [spec 529](docs/spec/529-tool-timeout-overrides.md) |
| 评测 | 数据集 CSV 互操作 | RFC 4180 转义/解析（逗号/引号/多行字段往返）、表头宽松校验、Writer 导出——与表格工具双向搬运（LangSmith/HF datasets CSV 形态） | [spec 527](docs/spec/527-dataset-csv.md) |
| 安全 | 流式回复秘密扫描 | 秘密第四缝（回复出站流）——滑动窗口跨 chunk 密钥不漏、占位符不拆分、复用 SecretScanner 7 型（500 SPI 第二消费者组合性证明） | [spec 536](docs/spec/536-secret-stream.md) |
| 事故响应 | 事故复盘一键包 | 一个调用产出标准复盘 ZIP（405 时间线+83 错误签名族+334 成本双维 rollup+summary 汇总，manifest 对账）——源缺席跳过不中断（317 ExportBundle 事故域预设组合） | [spec 521](docs/spec/521-postmortem-bundle.md) |
| 执行脊柱 | session.opened 生命周期事件补齐 | spawn 即派发（监听器挂载后、先于任何轮次；payload appId/agentName/sessionId 身份三元组——全局监听可达）与既有 session.closed 配对——生命周期首尾事件闭环 | [spec 522](docs/spec/522-session-opened-event.md) |
| 失控防护 | 失败轮快照面 | SessionObserver 缝（423 同法）——错误轮落复现最小集快照（错误类/消息 256 截断/输入 512 预览）+环形 128+JSONL 导出，Sentry event payload 一屏可读 | [spec 523](docs/spec/523-failure-turn-snapshots.md) |
| 工程治理 | 提示词模板严格渲染 | `{{var}}` 抽取/严格渲染（缺失变量一次列全——杜绝占位符原样漏进 prompt 的静默失败）+预检面；与 401 注册表组合消费（Jinja2 StrictUndefined） | [spec 512](docs/spec/512-prompt-template.md) |
| 评测 | 评估 A/A 抖动检测 | 同数据集同版本跑两遍——同项红绿翻转=抖动（fail/error 同红、方向不区分），单侧项=漂移不进分母；flakyRate+抖动清单先验评估系统自身稳定性（HELM/工业 A/A test） | [spec 513](docs/spec/513-eval-aa-flakiness.md) |
| 评测 | run 项耗时分布 | run 内项耗时 exact 最近秩 p50/p95/max+最慢项 top3（降序稳定）——「整个 run 慢在哪一项」一屏可读（416 分位族同法） | [spec 544](docs/spec/544-run-duration-stats.md) |
| 持久化 | 会话导出校验和 | 明文导出旁写 sha256 校验和+导入前验校（fail-closed）——传输/存储衰变在语义解析前被发现（511 密文封缄的明文通道对偶；S3 checksum） | [spec 547](docs/spec/547-export-checksum.md) |
| 工程治理 | 注册表快照导出/导入 | 全部名称的版本史+标签指针 → 可移植 JSON，导入空注册表按旧版本序重放（版本号对齐+标签重指）——提示词资产迁移可灾备（Langfuse export/import；401 扩散） | [spec 545](docs/spec/545-registry-snapshot.md) |
| 评测 | 双 judge 一致率 | Cohen κ 修正机遇一致的两 judge verdict 一致性+Landis-Koch 分级——朴素一致率被「都判绿」虚高时 κ 揭穿（scikit-learn cohen_kappa_score；516 双 judge 变体） | [spec 541](docs/spec/541-judge-agreement.md) |
| 评测 | judge 校准跟踪 | judge verdict vs 金标准断言的混淆矩阵四率（判红为正类）+agreement/precision/recall/f1（分母 0 null）——judge 宽松倾向先于版本对比被发现（LightEval） | [spec 516](docs/spec/516-judge-calibration.md) |
| 记忆治理 | 记忆压缩率分布观测 | 回收字符分位窗+逐出比直方图+折入 trigger 计数（挂既有 CompactionListener 缝，观测零干预）——梯子参数与折叠驱动信号有数据依据（416 分位族同法） | [spec 517](docs/spec/517-compaction-ratio-stats.md) |
| 投递可靠 | 投递时延分位数 | 成功投递时延滚动窗（512 样本）+exact 最近秩 p50/p95/p99（零样本 null）——「送是送到了但延迟 20 分钟」的劣化可见，与 135 lag 互补（416 分位族同法） | [spec 514](docs/spec/514-delivery-latency.md) |

## 生产级纵深 VII（F 会话 600 系增量）

F 会话（50 轮自迭代，借鉴高价值开源项目思想）的精选主线（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| MCP 治理 | 工具注解观测面 + 注解漂移 | server 自报 readOnly/destructive hint 入目录快照，同名注解翻转独立告警（MCP 规范 annotations；观测口径不裁决危险性） | [spec 600](docs/spec/600-mcp-tool-hints.md) |
| 模型韧性 | 离群驱逐恐慌阈值 | 健康候选跌破占比阈值即忽略驱逐返回全量——全逐比试坏端点更糟（Envoy panic threshold；默认 0=关） | [spec 601](docs/spec/601-outlier-panic-threshold.md) |
| 会话分支 | fork 谱系 | 子会话 state 带 `buzhou.fork.source` 指向源会话、事件带 copy 计数——排障/策略/导出可查「fork 自谁」（OTel span-links 思想的关联面落地） | [spec 602](docs/spec/602-fork-lineage.md) |
| 模型韧性 | GCRA 平滑限流后端 | TAT 匀速整形（默认 β=0 无突发，每 60/容量 秒放行）——突发即 429 的供应商场景 opt-in（redis-cell / Envoy GCRA） | [spec 603](docs/spec/603-gcra-rate-limit-backend.md) |
| 记忆治理 | 事实置信度衰减 | Fact 带 confidence、读时指数半衰过滤——陈年低置信事实不再占提示词预算（letta memory blocks；opt-in 装饰器不写回） | [spec 604](docs/spec/604-fact-confidence-decay.md) |
| Skill 体系 | 技能混合排序 | BM25 词法 + 语义 cosine 两路 RRF 融合——精确词（错误码/型号）补语义判别盲区（weaviate/Qdrant hybrid；opt-in） | [spec 605](docs/spec/605-hybrid-skill-ranking.md) |
| 会话治理 | 取消原因枚举 | `CancelCause` 闭集进 `session.cancelled` 事件与指标——「用户按停」与「停机收割」观测面分列（gRPC status codes） | [spec 606](docs/spec/606-cancel-cause.md) |
| 工程治理 | 黄金轨迹 payload 归一化 | UUID/时刻/时长/epoch → 稳定哨兵后结构断言——payload 级黄金不 flaky（ApprovalTests 思想） | [spec 607](docs/spec/607-golden-payload-normalization.md) |
| 执行脊柱 | 工具幂等键传播 | 每逻辑调用 `sessionId:callId` 键进 ToolContext——出站工具设上游幂等头，重试由上游去重（Stripe X-Idempotency-Key） | [spec 608](docs/spec/608-tool-idempotency-key.md) |
| 评估闭环 | 评估项级超时 | 挂死项中断收敛 error、其余项照跑、run 必完成（pytest-timeout；默认不设零变化） | [spec 609](docs/spec/609-eval-item-timeout.md) |
| MCP 治理 | 每连接并发上限 | per-connection 信号量闸（阻塞可中断、core 工具超时兜底）——stdio 单线程 server 不被并行 fan-out 打挂（默认不设零变化） | [spec 610](docs/spec/610-mcp-connection-concurrency.md) |
| 缓存与前缀 | 语义缓存维度漂移可见性 | 维度不匹配计数 + 首次 WARN——嵌入模型变更后命中率塌方不再静默（模型漂移监控惯例） | [spec 611](docs/spec/611-semantic-dimension-drift.md) |
| 记忆治理 | 微压缩影子干跑 | 纯函数干跑不应用（会压哪些/省多少 + evictRatio 梯度调参表）——策略调参零风险（Istio mirroring 思想） | [spec 612](docs/spec/612-compaction-shadow-eval.md) |
| 观测治理 | gzip 导出压缩档位 | 三个 gzip 导出面档位可配（-1 默认/[0,9]）——热导出省 CPU 冷归档求体积（nginx gzip_comp_level） | [spec 613](docs/spec/613-gzip-compression-level.md) |
| 模型韧性 | GCRA yml 装配 | `rate-limit.smoothing: gcra` 三行声明平滑整形（spec 603 原语装配扩散；共享后端在场则共享语义优先） | [spec 614](docs/spec/614-gcra-assembly.md) |
| 工程治理 | API 快照门硬化 | regenerate 门控为显式维护操作——比对从「恒自愈」变真门（测试副作用吃掉自身断言的治理） | [spec 615](docs/spec/615-api-snapshot-gate-hardening.md) |
| Skill 体系 | 技能目录清单指纹 | per 技能 sha256(description\|allowedTools) + 摘要 + 三分类对账——目录漂移可审计（cosign 清单思想；spec 175 的 skills 镜像） | [spec 616](docs/spec/616-skill-catalog-fingerprint.md) |
| Skill 体系 | 技能目录漂移看门狗 | 首拍建基线、漂移即 `skill.catalog.drifted` 事件 + 基线推进（spec 201 的 skills 镜像；含 616 diff 方向对齐修正） | [spec 617](docs/spec/617-skill-catalog-drift-watcher.md) |
| MCP 治理 | 注解聚入健康面 | `selfReportedDestructiveToolCount` 进 MCP 健康快照——自报危险与客户端认定危险两数对照可见（spec 600 扩散） | [spec 618](docs/spec/618-mcp-hints-health.md) |
| Spill | 预览头尾语义 | 截断预览 = 头 3/4 + 省略标注 + 尾 1/4——大结果的尾部汇总/结论可见（ripgrep context） | [spec 619](docs/spec/619-spill-headtail-preview.md) |
| 模型韧性 | 熔断时间窗衰减 | `circuit.time-window` 老样本出率计算与 min-calls 门——低频调用下陈年失败不再永久占窗（resilience4j TIME-based；默认 0 零变化） | [spec 620](docs/spec/620-circuit-time-window.md) |
| 观测治理 | 导出打包落盘持久档 | Durability NONE/FILE/FILE_AND_DIR——审计归档的「返回即在盘上」FULL 语义（sqlite WAL 同步档位） | [spec 621](docs/spec/621-export-bundle-durability.md) |
| 会话治理 | 归档/还原每会话互斥 | 同会话 archive/restore 串行——并发交错的数据丢失窗关闭（restore 旁路事务的根因收口） | [spec 622](docs/spec/622-archiver-session-mutex.md) |
| 会话治理 | saga per-session 事务域 | CompensatingBatch 会话级锁重载——跨会话归档真并行（全局锁吞吐瓶颈解除） | [spec 623](docs/spec/623-saga-session-transaction.md) |
| 成本预算 | 预算池借比例上限 | 单会话 held ≤ base × ratio——单借方不再吃光 surplus 饿死同伴（K8s LimitRange；默认不设限零变化） | [spec 624](docs/spec/624-budget-pool-borrow-ratio.md) |
| 工程治理 | 启动装配摘要 | `buzhou.assembly-report.enabled=true` 启动后一行 INFO 生效面板（机制开关/store/模型）——「这套进程装了什么」一屏可答（Spring Boot diagnostics report） | [spec 625](docs/spec/625-assembly-report.md) |
| 护栏 | 事实衰减 yml 装配 | `guard.fact-decay.half-life-turns` 声明即衰减（原语移驻 core 接通 GuardModule——spec 604 扩散） | [spec 626](docs/spec/626-fact-decay-assembly.md) |
| 会话治理 | fork 谱系进面板 | sessions 端点 `forkedActive` 段——活跃分支计数即重试/探索流量信号（spec 602+346 接线） | [spec 627](docs/spec/627-forked-sessions-panel.md) |
| MCP 治理 | 每连接并发上限 yml 装配 | `mcp.per-connection-concurrency-limit` 声明即生效（spec 610 扩散；stdio 单线程 server 三行防护） | [spec 628](docs/spec/628-mcp-concurrency-assembly.md) |
| Skill 体系 | 技能漂移看门狗接线 | 渲染节拍即巡查宿主（每轮清单注入顺带指纹 check——零调度，漂移最迟下一轮显形） | [spec 629](docs/spec/629-renderer-drift-watch.md) |
| Skill 体系 | 混合排序装配 | `hybrid-ranking.enabled` 声明即 RRF 融合（SkillRanker 接口抽取双实现互换——spec 605 扩散） | [spec 630](docs/spec/630-hybrid-ranking-assembly.md) |
| 会话索引 | keyset 游标分页 | 行序键锚定翻页——活跃索引不跳行不重行（三实现规范序统一；Redis 平局序偏差一并收口） | [spec 631](docs/spec/631-session-index-keyset.md) |
| 会话治理 | 排水取消原因 E2E | 停机对在途会话的 session.cancelled {SHUTDOWN_DRAIN} 真路径钉住（spec 606 补验） | [spec 632](docs/spec/632-drain-cause-e2e.md) |
| 模型韧性 | panic/时间窗补验 | panicActivations() 编程可读面 + time-window yml 绑定用例（spec 601/620 补齐） | [spec 633](docs/spec/633-resilience-obs-gap.md) |
| 会话分支 | 回放起点 state | forkFromTurn 写 `buzhou.fork.turn`——「fork 自谁+从哪重走」state 面可查（spec 602 补全） | [spec 634](docs/spec/634-fork-turn-state.md) |
| 工具治理 | 变换 fail-open 可观测 | failOpenCount() + 计数 + 首次 WARN——变换常年失效不再静默（spec 169 补全） | [spec 635](docs/spec/635-transform-fail-open-observability.md) |
| 护栏 | 衰减过滤可观测 | `filteredCount()`——衰减确实在滤陈年低置信事实的运行信号（spec 626 补全） | [spec 636](docs/spec/636-decay-filter-observability.md) |
| 模型韧性 | 限流后端形态健康面 | `rateLimitBackend`（memory/memory-gcra/redis/none）——GCRA 声明是否生效一读便知（spec 614 补全） | [spec 637](docs/spec/637-ratelimit-backend-kind-health.md) |
| 模型韧性 | 熔断时间窗生效读面 | `circuitTimeWindowMs`（0=count 窗/正数=声明值）——spec 620 生效确认面 | [spec 638](docs/spec/638-circuit-time-window-readout.md) |
| 缓存与前缀 | 缓存命中率便利 getter | Response/Semantic 两 store `hitRate()`（零请求诚实 0.0——观测便利面） | [spec 639](docs/spec/639-cache-hit-rate.md) |

## 生产级纵深 VIII（G 会话 700 系增量）

G 会话（effort #700+ 号段，借鉴 GitHub >10K star 项目）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 模型韧性 | 能力门决策审计读数 | deny 环形留痕（容量 64+dropped 计数）+admit 计数+denyByModel 聚合+snapshot 不可变报告——「门最近拒了谁」从异常瞬间变成可查询证据面（OPA Decision Logs） | [spec 700](docs/spec/700-capability-decision-audit.md) |
| 缓存与前缀 | 语义缓存权重预算驱逐 | `max-weight-chars`（默认 0=关）——按响应字符数腾挪驱逐，大响应不再挤出高频短条目；weightEvictions 独立口径（Caffeine weigher） | [spec 701](docs/spec/701-semantic-cache-weight-budget.md) |
| 模型韧性 | 断路器变迁事件流读数 | CircuitTransitionJournal 进程级变迁环形留痕+per-model trips/recoveries/halfOpens 聚合——「最近跳了谁/多久恢复」不再散落会话事件通道（Resilience4j EventConsumer） | [spec 702](docs/spec/702-circuit-transition-journal.md) |
| 模型路由 | 健康加权路由抑制原语 | attach(router,breaker,floor)——跳闸压权至地板（全跳不黑洞）恢复回声明值；breaker 加变迁监听缝（HAProxy agent-check） | [spec 703](docs/spec/703-routing-health-dampener.md) |
| 观测治理 | 提示词角色构成拆解 | PromptComposition.analyze 按角色聚合 chars/messages/share 降序——水位告警后「谁在吃预算」的证据面（Langfuse prompt analytics；纯读数） | [spec 704](docs/spec/704-prompt-composition.md) |
| 持久化 | Redis 键命名空间碰撞审计 | RedisKeyLayoutAudit 结构性对抗模拟——spev/event 保留段与 lease 冒号后缀三族潜伏碰撞证据+reservedSegments 读数+isSafeSessionId 摄入守卫（fsck 思想+E R12 教训制度化） | [spec 705](docs/spec/705-redis-key-layout-audit.md) |
| MCP 治理 | 工具目录差异报告 | McpDirectoryDiff 两快照 plan 式 diff——per-server 四态+增/删/翻转明细+危险方向翻转 risky 标记（readOnly→false/destructive→true）（ArgoCD diff） | [spec 706](docs/spec/706-mcp-directory-diff.md) |
| Spill 治理 | spill 双文件配对巡检 | SpillPairAudit 只读扫 .spill/.meta 配对残缺（双写崩溃窗口）——孤 data 带字节量、配额吞噬可见；三层完整性矩阵中层（Git fsck） | [spec 707](docs/spec/707-spill-pair-audit.md) |
| 评估闭环 | 评估项结果记忆化 | setMemoizationKey opt-in——sig(数据+judge 身份)未变复用上轮判定跳过模型调用，detail `[MEMO]` 留痕+hits/misses 计数；ERROR 不缓存（scikit-learn Pipeline memory） | [spec 708](docs/spec/708-eval-item-memoization.md) |
| 评测 | 实验到期自动停 | 构造器扩 expiresAt+Clock——到期按未入组返回 null、曝光计独立 `__expired__` 桶+expiredExperiments 读数（GrowthBook feature expiry） | [spec 709](docs/spec/709-experiment-expiry.md) |
| 评测 | 全局 holdout 层 | holdoutPercent 构造参数——跨实验一致排除的纯控制组、曝光计 `__holdout__` 独立桶（Statsig holdout layer） | [spec 710](docs/spec/710-experiment-holdout.md) |
| 持久化 | 消息序列连续性审计 | TurnSequenceAudit 单遍判 GAP/DUPLICATE/OUT_OF_ORDER——store 级丢数据从「上下文缺段」猜测变结构化证据（Kafka offset 审计） | [spec 711](docs/spec/711-turn-sequence-audit.md) |
| 观测治理 | span 健康摘要（543 补全） | healthSummary——RUNNING 残留（泄漏信号）+errorRate（口径显式）；R13 core 重建撞 543 的修正收敛（OTel span status） | [spec 712](docs/spec/712-span-health-summary.md) |
| 评估闭环 | 数据集标签与过滤 | EvalDatasetMeta 扩 tags（归一升序不可变）+tag/untag 幂等+listDatasetsByTag 圈选；旧记录零迁移（Langfuse dataset tags） | [spec 713](docs/spec/713-dataset-tags.md) |
| 评估闭环 | 相似度阈值判定器 | BuiltInEvaluators.similarity(minRatio)——字符 trigram Jaccard 模糊判定+detail 分数留痕，LLM 输出词序微变不再脆判（HELM grading） | [spec 714](docs/spec/714-similarity-evaluator.md) |
| 护栏 | PII 格式保形掩码 | FormatPreservingMasker——手机/证件/邮箱/IP 保形掩码+形状校验失败全星降级；三形态（占位/掩码/vault）各司其职（Presidio FP） | [spec 715](docs/spec/715-format-preserving-mask.md) |
| 工具治理 | Todo 陈旧度审计读数 | TodoStalenessAudit 轮次年龄+滞留清单+promptHint 一行人话——agent 任务清单烂掉可见（todo 纪律面板） | [spec 716](docs/spec/716-todo-staleness.md) |
| 记忆治理 | 共享事实冲突审计 | FactConflictAudit 按键分组判 CONFLICT/DUPLICATE+entries 证据全列——导出/合并/多实例聚合的「精神分裂」可见（mem0 治理） | [spec 717](docs/spec/717-fact-conflict-audit.md) |
| 评估闭环 | 评估通过率漂移基线 | setDriftBaseline(window,warnShift) opt-in——同数据集近 N 次 passRate 均值基线、|Δ|超线 WARN+计数+lastDriftDelta 读数；防自污染只取早于本次（Evidently drift） | [spec 718](docs/spec/718-eval-drift-baseline.md) |
| 模型韧性 | 供应商限流头前瞻读数 | ProviderRateLimitSignals 解析 x-ratelimit 余量/reset+utilization+三级压力分级——429 之前的拥挤信号（OpenAI 头约定） | [spec 719](docs/spec/719-provider-ratelimit-signals.md) |
| 缓存与前缀 | 嵌入超限分批装饰器 | ChunkingEmbeddingModel 按 maxBatchSize 切块顺序调 delegate+全局 index 重排——批量嵌入超供应商 cap 不再 400（OpenAI embeddings 批限） | [spec 721](docs/spec/721-chunking-embedding-model.md) |
| MCP 治理 | 每连接并发占用视图 | concurrencyViews()——server→limit/available/inFlight 实时占用，610 并发闸从黑盒变读数（etcd/线程池监控惯例） | [spec 722](docs/spec/722-mcp-concurrency-views.md) |

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
- **spec 16–74（effort #5–#35 增量纵深）** — 共享与原子性 / 缓存与前缀 / 评估闭环 / 观测与分析 / 恢复与压缩 / 成本归因（精选主线见上方「生产级纵深（effort #7–#35 增量）」表；全量清单在 [docs/spec/](docs/spec/)，运维口径在 [docs/ops-runbook.md](docs/ops-runbook.md)）
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
