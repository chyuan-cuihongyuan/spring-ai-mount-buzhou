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
| 成本预算 | 成本归因台账 | 同一笔 microUsd 双维入账（model+虚拟 key）——chargeback 有账面；无 key 诚实桶、万分比 share、JSONL 报表（Kubecost/OpenCost 按标签归因） | [spec 334](docs/spec/334-cost-attribution.md) |

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
