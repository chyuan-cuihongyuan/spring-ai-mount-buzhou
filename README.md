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

Buzhou 把这些「Agent 运行时」该有的能力收敛成十大机制，作为一层 Harness 挂在 Spring AI 之上。你的 `ChatClient` / `ChatModel` 不变，Buzhou 只在外围补齐面向生产场景所需的稳定性与可观测性——目前为实验性（alpha），详见[项目状态](#项目状态)。

## 十大机制

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
| 10 | **模型韧性层** | 瞬断重试（幂等门+瞬断白名单）、回退链、金丝雀、统一超时、归一化错误分类、onModelError 兜底 | `buzhou-resilience` |

> 领域术语以 [CONTEXT.md](CONTEXT.md) 为准；各机制的完整设计见 [docs/spec/](docs/spec/)（00-overview 总入口 + 机制详设 01–55）。

## 生产级纵深（effort #5 新增）

十大机制之上的运营级能力（详设 spec 15–23）：

| 能力 | 一句话 | 详设 |
|------|--------|------|
| **模型熔断 + 备模型降级链** | 失败率跳闸→半开探测恢复；主模型熔断 OPEN 后请求零重试直达备模型 | [spec 15](docs/spec/15-model-resilience.md) |
| **Token/成本预算** | 会话级 token/成本累计（microUsd 整数口径价目换算）+ 三硬顶预算闸 | [spec 16](docs/spec/16-cost-quota.md) |
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
| | 一致性工具 | outbox due 索引审计（孤儿/陈旧/缺失）——投递停摆提前可见 | [spec 96](docs/spec/96-due-index-audit.md) |
| | webhook 订阅过滤 | include-types 命中才入队（被滤不占容量） | [spec 105](docs/spec/105-webhook-type-filter.md) |
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
| 会话与 fork | fork 谱系键常量收口 | `SessionForkKeys`（SOURCE/TURN/PRODUCER）写读两侧同源——字符串复制漂移=谱系断 | [spec 640](docs/spec/640-fork-lineage-keys.md) |
| 缓存与前缀 | 响应缓存 miss 惊群合并 | `response-cache.coalescing`（opt-in）：首个 miss 窗口同 key 并发收敛一次模型调用（singleflight；失败不共享） | [spec 641](docs/spec/641-response-cache-stampede-coalescing.md) |
| 观测与运维 | 追加式 JSONL 大小轮转 | `RollingJsonlWriter`（64MB×3 默认开；≤0 显式关）——健康时间线/影子对照/prompt 快照三类明细磁盘封顶 | [spec 642](docs/spec/642-jsonl-rolling.md) |
| 观测与运维 | JSONL 轮转 yml 细调 | `health.timeline.export-max-{bytes,history}` / `shadow.detail-max-{bytes,history}`——磁盘预算档位声明化（缺席默认/显式关） | [spec 643](docs/spec/643-jsonl-rolling-yml.md) |
| 观测与运维 | JSON 行手工拼接收口 | 四处（prompt 快照/导出清单/失败快照/死信）统一 Jackson——注入字符（引号/换行/制表符）行仍合法回读原值 | [spec 644](docs/spec/644-jsonl-jackson-escaping.md) |
| 工程门禁 | API 快照再生收口 | E 会话合并后快照再生成 + api-surface.md 同步（三个新公共类入档）——快照门复绿 | [spec 645](docs/spec/645-api-snapshot-regen.md) |
| Hook 护栏 | hook 链 per-hook 耗时观测 | HookChain 八回调面计时（count/total/max + stats() 快照）+ 慢 hook（>100ms）WARN 原子去重——慢 hook 一查便知 | [spec 646](docs/spec/646-hook-chain-timing.md) |
| Hook 护栏 | hook 计时进程级聚合读面 | HookTimingAggregator（Holder 装配开启）+ hook-timing 健康段（per-hook count/total/max/avg 微秒）——全进程内联预算分布一屏可读 | [spec 647](docs/spec/647-hook-timing-aggregate-health.md) |
| 观测与运维 | JSONL 轮转事件指标化 | `buzhou.jsonl.rotated` / `rotate-failed`（tag file）——磁盘保护在工作/轮转病灶一数可读（spec 642 补全） | [spec 648](docs/spec/648-jsonl-rotate-metrics.md) |
| 工程门禁 | F 会话 600 系收口 | 50 轮自迭代闭环：spec 600–649 / 票 T851–T950 / impl 453–502 全档 + 全仓 verify 终验绿 | [spec 649](docs/spec/649-session-f-closing.md) |
| 观测与运维 | 工具执行 per-tool 耗时聚合读面 | ToolTimingAggregator（Holder 装配开启）+ tool-timing 健康段（per-tool count/total/max/avg 微秒 + failed）——哪个工具吃掉工具耗时预算一屏可读（pg_stat_statements 借鉴） | [spec 700](docs/spec/700-tool-timing-aggregate.md) |
| 观测与运维 | 缓存 stale-while-revalidate | 跨轮工具缓存 swrGrace opt-in：过期后 grace 窗同步回 stale + 虚拟线程后台单飞刷新（失败保旧值）——TTL 边界调用延迟归零（nginx proxy_cache_use_stale 借鉴） | [spec 701](docs/spec/701-cache-stale-while-revalidate.md) |
| 模型韧性 | 路由慢启动权重爬坡 | RoutingSlowStart：权重上调先落 floor 再分 4 步爬到 target（热重载接线 opt-in、降权瞬时）——升配不瞬时打爆恢复端点（nginx upstream slow_start 借鉴） | [spec 702](docs/spec/702-routing-slow-start.md) |
| MCP 治理 | MCP keepalive 空闲探活 | 注册表周期 listToolNames 探活（opt-in），失败重建走 spec-changed 同口径——空闲死连接不再拖到用户 Turn 才暴露（gRPC keepalive 借鉴） | [spec 703](docs/spec/703-mcp-keepalive.md) |
| 观测与运维 | 最小可用水位闸（归档 PDB） | SessionAvailabilityFloor 挂 archive()：存活会话 ≤ minAvailable 拒绝自愿驱逐（未知计数 fail-open、restore 不受闸）——故障期运维动作不再削薄在线容量（k8s PodDisruptionBudget 借鉴） | [spec 704](docs/spec/704-session-availability-floor.md) |
| 工程门禁 | store SPI 契约校验套件 | SessionStateStoreContract.verify 九项语义契约（CAS 消费一次/null-expect 首写/前缀扫描/幂等清场）+ Report 逐项明细——第三方 store 实现一行自证（Pact 借鉴） | [spec 705](docs/spec/705-store-contract.md) |
| 工程门禁 | API 快照 diff 破坏性分级 | SnapshotDiff 分类器（removed=破坏性前置审查 / added=非破坏 regenerate）——门语义不变，失败信息分级带处置指引（oasdiff 借鉴） | [spec 706](docs/spec/706-api-snapshot-diff-grading.md) |
| 工程门禁 | 模块边界守卫 + internal 存量清零 | ModuleBoundaryGuardTest 源码级双规则守卫（internal 跨模块禁引 / feature 互依禁引，自举归属零依赖）——8 处存量违规清零、6 类迁出 internal（ArchUnit 思想自写） | [spec 707](docs/spec/707-module-boundary-guard.md) |
| Hook 护栏 | HookTiming 滚动 max 读面 | RollingMaxCounter 时间桶滚动 max（8×10s 默认窗 + 时钟注入）+ hook-timing 健康行 rollingMaxMicros——修好慢 hook 后「现在还慢不慢」可答（micrometer max decay 借鉴） | [spec 708](docs/spec/708-rolling-max.md) |
| Guard 护栏 | 角色权限拒绝有界日志 | ToolDenialLog（128 条环形明细 + 64 键 (role,tool) 聚合 + unauthorized/undefined-role 分流）——谁在反复试哪些无权工具一查便知（Redis ACL LOG 借鉴） | [spec 709](docs/spec/709-tool-denial-log.md) |
| 会话治理 | 会话导出 unchanged 协商 | contentFingerprint（内容投影剔除 exportedAt，sha256-c: 前缀）+ exportIfChanged（UNCHANGED 不外发 payload，fail-open）——周期同步方免收全量（HTTP ETag 借鉴） | [spec 710](docs/spec/710-export-conditional.md) |
| 会话治理 | fork 谱系游走环防护 | ForkLineageWalker（SOURCE 链上溯 + visited 环检测 + 深度 64 封顶，只读不修）——导入路径注入环/超深链时消费方不死循环（call-graph 环检测借鉴） | [spec 711](docs/spec/711-fork-lineage-walker.md) |
| 观测与运维 | JSONL 轮转旧档 gzip 压缩 | RollingJsonlWriter compressFromGeneration opt-in（代际 ≥N 存 .gz、file.1 恒明文 delaycompress、双形态清理）——观测明细 ~10:1 省盘（logrotate compress 借鉴） | [spec 712](docs/spec/712-jsonl-rotate-gzip.md) |
| Guard 护栏 | PII 格式保持假名化 | PiiDetector.pseudonymize（同长度同形态替身：数字/字母/分隔分层，(seed,type,text) 确定性播种，不可逆零托管）——保形状不保校验位诚实划界（Presidio surrogate 借鉴） | [spec 713](docs/spec/713-pii-pseudonymize.md) |
| Guard 护栏 | 秘密扫描熵阈值过滤 | SecretScanner 可选 Shannon 熵阈值（默认 4.0 bits/char，PRIVATE_KEY_BLOCK 豁免）——AWS 文档示例键/占位串不再误杀（truffleHog entropy 借鉴） | [spec 714](docs/spec/714-secret-entropy-filter.md) |
| 工程门禁 | 指标命名规范守卫 | MetricNamingGuardTest 源码级双正则提取（调用点 + METRIC 常量）+ 命名规则断言（点分隔小写段）——316 指标名漂移即红、动态拼接前缀清零（prometheus naming 借鉴） | [spec 715](docs/spec/715-metric-naming-guard.md) |
| 观测与运维 | 告警规则 dry-run | AlertRuleEngine.dryRun 纯只读推演（wouldFire/wouldRecover/pending 三分类 + 三不承诺：状态机/通知/指标零副作用）——规则上线前验配不实弹（k8s admission dryRun 借鉴） | [spec 716](docs/spec/716-alert-dry-run.md) |
| 观测与运维 | 工具调用图谱环检测 | ToolGraphAnalyzer.cycles 初等环 DFS 枚举（最小节点锚去重 + MAX_CYCLES=16 有界 + 稳定排序）——模型循环调用模式可见（call-graph 环检测借鉴） | [spec 717](docs/spec/717-graph-cycle-detection.md) |
| 观测与运维 | webhook 投递限速 | WebhookRateLimiter 令牌桶 + forwarder 可选接线（defer 留 outbox 原状不进重试状态机、整批 defer 早退防热旋）——事件风暴不冲垮下游消费者（envoy local rate limit 借鉴） | [spec 718](docs/spec/718-webhook-rate-limit.md) |
| 观测与运维 | 生效配置 diff 读面 | ConfigDiff 纯函数（ADDED/REMOVED/CHANGED 三分类 + 字典序 + 掩码同值语义）——热重载/部署改了什么一数可读（kubectl diff 借鉴） | [spec 719](docs/spec/719-config-diff.md) |
| 观测与运维 | 错误签名静默标记 | ErrorSignatures mute/unmute（MUTED_CAP=64 有界）——top 读面降噪但 snapshot 计数照常（静默是降噪不是删除），reset 连带清空（Sentry muted issues 借鉴） | [spec 720](docs/spec/720-signature-mute.md) |
| 观测与运维 | 死信重放审计事件 | replayDeadLetters 动作留痕：dead-replayed 指标（delta=条数）+ replayCount/replayedCount 累计 + 结构化审计日志——运维敏感动作必留痕（审计完整性惯例） | [spec 721](docs/spec/721-dead-replay-audit.md) |
| 观测与运维 | 工具泳道排队时延观测 | LaneLimitingToolCallback acquire 段计时 + WaitStats（waited/total/max/timeouts）+ buzhou.lane.wait/timeout 指标——排队与执行分开计量、容量调参有据（grpc queue 时延借鉴） | [spec 722](docs/spec/722-lane-wait-observability.md) |
| 模型韧性 | 路由金丝雀阶段标签 | RouteStages（STABLE/CANARY/ARCHIVED 有界注册表）+ filter 构造期过滤（未标注恒可见）——端点生命周期显式声明、退役端点不参路由（MLflow stages 借鉴） | [spec 723](docs/spec/723-route-stages.md) |
| MCP 治理 | MCP keepalive yml 装配 | Builder.keepalive + fromYml keepalive-interval 键直通注册表 9 参构造——声明即启用探活，缺省零变化（spec 703 装配轮） | [spec 724](docs/spec/724-mcp-keepalive-assembly.md) |
| 模型韧性 | 路由慢启动 yml 装配 | buzhou.routing.slow-start 属性 + 条件 bean + 热重载 ObjectProvider 接线——权重上调爬坡声明即启用（spec 702 装配轮） | [spec 725](docs/spec/725-slow-start-assembly.md) |
| 观测与运维 | 归档 PDB yml 装配 | buzhou.cleanup.min-available-sessions 属性 + capped probe 计数（limit=min+1 一页即答「>min 否」）——spec 704 声明式入口 | [spec 726](docs/spec/726-pdb-assembly.md) |
| Guard 护栏 | 秘密熵过滤 Builder 装配 | GuardModule.Builder.secretMinEntropy 直通 SecretScanner——声明即滤示例键/占位串（spec 714 装配轮） | [spec 727](docs/spec/727-secret-entropy-assembly.md) |
| 观测与运维 | webhook 限速 yml 装配 | buzhou.webhook.rate-limit-per-second/burst 属性直通 setRateLimiter——声明即节流投递（spec 718 装配轮） | [spec 728](docs/spec/728-webhook-ratelimit-assembly.md) |
| 观测与运维 | 健康时间线 JSONL 压缩线装配 | export-compress-from 属性直通 RollingJsonlWriter 压缩线——观测明细声明即省盘（spec 712 装配轮） | [spec 729](docs/spec/729-timeline-gzip-assembly.md) |
| 模型韧性 | 路由阶段标签 yml 装配 | buzhou.routing.stages/visible-stages 双键声明 → 构造期过滤（<2 路 fail-fast）——spec 723 声明式入口 | [spec 730](docs/spec/730-route-stages-assembly.md) |
| Guard 护栏 | PII 假名化模式装配 | PiiDetector 模式构造（redact 分派 pseudonymize）+ Builder.piiPreserveFormat 直通双缝——同长度替身声明即启用（spec 713 装配轮） | [spec 731](docs/spec/731-pii-mode-assembly.md) |
| 工程门禁 | 契约套件接入示例（H2） | JdbcSessionStateStore 过 SessionStateStoreContract 九项检查（H2 无 Docker CI 口径）——第三方 store 自证价值主张实证（spec 705 复用面） | [spec 732](docs/spec/732-contract-jdbc-demo.md) |
| 会话治理 | 导出协商联动补验 | 往返指纹稳定 + 两轮协商周期 + 双校验和幂等——周期同步脚本语义闭环（spec 710 补验） | [spec 733](docs/spec/733-export-negotiation-e2e.md) |
| 观测与运维 | PDB×空闲压缩联动补验 | sweep 空闲候选×floor 拒绝/放行两态 + 计数一致——候选→闸联动闭环（spec 704/179 补验） | [spec 734](docs/spec/734-pdb-idle-e2e.md) |
| 模型韧性 | 金丝雀×慢启动×热重载联动补验 | filter→构造→热调升配 ramp→tick 到位编排闭环——archived 不入路由（spec 723/702/340 补验） | [spec 735](docs/spec/735-stages-slowstart-e2e.md) |
| 观测与运维 | ConfigDiff×快照端点同源补验 | 端点真快照两份对比——掩码语义/diff 稳定闭环（spec 719 补验） | [spec 736](docs/spec/736-config-diff-e2e.md) |
| 观测与运维 | 工具侧滚动 max 同构扩散 | ToolTimingAggregator windowedMax + 健康行 rollingMaxMicros（RollingMaxCounter 复用）——spec 708 同构扩散 | [spec 738](docs/spec/738-tool-timing-rollingmax.md) |
| Guard 护栏 | 拒绝日志排序稳定性补验 | topDenials 并列字典序 tie-break（实现缺陷补齐）+ 环形窗口读面零副作用——spec 709 补验 | [spec 737](docs/spec/737-denial-log-stability.md) |
| 观测与运维 | 限速×死信路径隔离补验 | defer 零状态机扰动/令牌恢复全路径可达/重放不绕闸——spec 718/24 补验 | [spec 739](docs/spec/739-ratelimit-deadletter.md) |
| 会话治理 | 谱系游走导入场景深链补验 | 100 节点链+尾部环——默认深度截断不 OOM、显式深度走至环处 loopDetected（spec 711 补验） | [spec 740](docs/spec/740-lineage-import-e2e.md) |
| 观测与运维 | 静默标记×健康段联动补验 | mute 自动传导至健康段 top 排除、snapshot 原样、unmute 回归——spec 720/85 联动闭环 | [spec 742](docs/spec/742-mute-health-e2e.md) |
| Guard 护栏 | 假名化×幂等占位符互操作补验 | 假名化输出二次处理幂等（替身不再匹配→原样）+ 跨模式无双重脱敏——spec 713/731 补验 | [spec 741](docs/spec/741-pseudonymize-idempotency.md) |
| 观测与运维 | dryRun×AlertGate 语义确认 | 静默门吞实弹通知但 dryRun 推演如实报告——dry-run=引擎推演/gate=通道策略正交（spec 716 补验） | [spec 746](docs/spec/746-dryrun-gate.md) |
| 观测与运维 | ToolTimingAggregator 并发压测 | 4000 并发 record 不变量守恒（count/total/max 精确断言）+ 多工具隔离 + windowedMax 同窗一致——热路径组件压测实证（spec 700 补验） | [spec 747](docs/spec/747-tool-timing-concurrency.md) |
| 工程门禁 | G 会话 700 系收口 | 50 轮自迭代闭环：spec 700–749 / 票 T951–T1050 / impl 503–552 全档 + 全仓 verify 终验绿（spec 745 台账预检补缺位） | [spec 749](docs/spec/749-session-g-closing.md) |
| 工程门禁 | G 会话中点快照再生 | 全量 reactor regenerate——8 新公共类入档零移除零意外（ConfigDiff/RollingMaxCounter/ForkLineageWalker/SessionExportConditional/MessageStoreContract/WebhookRateLimiter/ToolDenialLog/RouteStages）+ api-surface.md 同步 | [spec 748](docs/spec/748-snapshot-regen-g.md) |
| 工程门禁 | G 会话收口预检（台账核查） | spec 700–748 连续核查（745 缺位本轮填补）/票 96 张全闭环/impl 对账 + SpecCoverage + 快照比对复跑绿 | [spec 745](docs/spec/745-g-session-audit.md) |
| 工程门禁 | MessageStore SPI 契约校验套件 | MessageStoreContract 四项语义契约（append/load 保序、未知会话空读、多次追加保序、deleteSession 幂等）——spec 705 同构扩散 | [spec 743](docs/spec/743-messagestore-contract.md) |
| 工程门禁 | MessageStore 契约接入 H2 | JdbcMessageStore 过四项契约（H2 无 Docker CI 口径）——契约抓出探针会话主键冲突设计缺陷并重构为每检查独立会话（spec 743 复用面） | [spec 744](docs/spec/744-messagestore-h2.md) |
| 观测与运维 | I 会话 900 系启动 · 事件丢弃按原因分类读面 | EventDropBreakdown（drop-oldest/block-timeout/closed-undelivered 分桶）+ eventDropBreakdown() 读面，ΣbyReason 守恒 == dropped——Sentry discarded events 借鉴（spec 900） | [spec 900](docs/spec/900-event-drop-breakdown.md) |
| 评估闭环 | 评估失败率中途剪枝 | EvalPrunePolicy（观察窗+失败率阈值）opt-in，串行路径恰停剩余项标 pruned 不烧预算——Optuna pruner 提前停止借鉴（spec 901） | [spec 901](docs/spec/901-eval-prune.md) |
| 评估闭环 | pass@k 无偏估计器 | EvalPassAtK 连乘无偏公式（HumanEval §2.1）+ 逐项聚合——纯函数不触 store，k 次采样留宿主（spec 902） | [spec 902](docs/spec/902-pass-at-k.md) |
| 评估闭环 | bootstrap 均值置信区间 | EvalScoreAnalytics.bootstrapMeanInterval（Efron percentile，seed 注入可复现）——小样本 mean 抽样误差显形（spec 903） | [spec 903](docs/spec/903-bootstrap-ci.md) |
| 会话治理 | 导入审计与严格模式 | SessionExportAudit.audit（未知顶层字段/缺失推荐字段报告）+ fromJsonStrict opt-in 拒绝——宽松路径零变化，pg_restore --exit-on-error / protobuf unknown fields 思想（spec 904） | [spec 904](docs/spec/904-import-audit.md) |
| 观测与运维 | 健康聚合评分读面 | BuzhouHealthScore（UP=100/UNKNOWN=50 中性/DOWN=0 算术平均 + healthy/degraded/unhealthy 分档常量 + DOWN 清单）——K8s probe aggregate 思想（spec 905） | [spec 905](docs/spec/905-health-score.md) |
| 观测治理 | 工具耗时火焰图数据面 | 工具耗时 self/cumulative 时间分解读面——Brendan Gregg flamegraph 借鉴（spec 906） | [spec 906](docs/spec/906-flame-timing.md) |
| 模型韧性 | outbox 投递批量 AIMD 自适应 | 批量加性增、乘性减自适应——TCP 拥塞控制 RFC 5681 直觉（spec 907） | [spec 907](docs/spec/907-aimd-batch.md) |
| 评估闭环 | k 次 run 稳定性矩阵 | k>2 区分「偶发翻转」与「系统性震荡」——Google FlakyTest 借鉴（spec 908） | [spec 908](docs/spec/908-k-stability.md) |
| 评估闭环 | 评估分数分位数读面 | EvalScoreAnalytics.percentiles（R-7 线性插值 h=(n−1)·q）——numpy percentile 借鉴（spec 909） | [spec 909](docs/spec/909-percentiles.md) |
| 模型韧性 | AIMD 自适应批量 yml 装配 | webhookEventForwarder 批量 AIMD yml 装配面（spec 910） | [spec 910](docs/spec/910-aimd-yml.md) |
| 会话治理 | JCS 规范化内容指纹 | SessionExportChecksum.canonicalContentFingerprint——RFC 8785 JCS 借鉴（spec 911） | [spec 911](docs/spec/911-jcs-fingerprint.md) |
| 会话治理 | 会话导出 diff 读面 | SessionExportDiff 双导出结构化差异（spec 912） | [spec 912](docs/spec/912-export-diff.md) |
| 会话治理 | 导出域三件套联动 e2e | 导出域端到端联动验证（spec 913） | [spec 913](docs/spec/913-export-domain-e2e.md) |
| 观测治理 | gate 判定环形历史读面 | gate 判定历史环时间线（spec 914） | [spec 914](docs/spec/914-gate-history.md) |
| 观测与运维 | EventDropBreakdown 并发压测 | 丢弃分桶并发压测实证（spec 915） | [spec 915](docs/spec/915-drop-breakdown-stress.md) |
| 观测治理 | pruned×稳定性×gate 联动补验 | pruned 不入稳定性分母（按有效样本判定，防假稳定）+ KItemVerdict null 容忍——G r39 补验先例（spec 916） | [spec 916](docs/spec/916-pruned-stability.md) |
| 观测治理 | 健康评分端点装配 | /actuator/buzhou 快照加 score 段（投影+safeScore 降级）；实证修复端点 mechanism/status 裸调用无隔离——spec 905 装配留位兑现（spec 917） | [spec 917](docs/spec/917-score-assembly.md) |
| 观测治理 | 丢弃计数 reason 维度指标 | DROP_REASON_* 六常量统一三处字面量 + 双轨指标（总量保留+dropped-reason 值域封闭）——breakdown 键与 tag 同源（spec 918） | [spec 918](docs/spec/918-drop-reason-metric.md) |
| 会话治理 | 加密导出×审计×指纹联动 e2e | 密文进明文审计 fail-closed 固化/seal→open→严格导入全链/nonce 密文不同内容指纹稳定（spec 919） | [spec 919](docs/spec/919-encrypted-export-e2e.md) |
| 模型韧性 | webhook 限流器余量快照读面 | WebhookRateLimiter.snapshot（同锁强一致 tokens/capacity/refill/deferred 四值投影）——TurnRateLimitHook.availableSnapshot 同构（spec 920） | [spec 920](docs/spec/920-ratelimit-snapshot.md) |
| 会话治理 | TurnDeadline 软截止窗口读法 | withinSoftWindow 预警窗判定 + softDeadlineAt 预警绝对时刻——K8s graceful period 分层语义（spec 921） | [spec 921](docs/spec/921-soft-window.md) |
| 持久化 | SessionLeaseStore 契约校验套件 | 九项租约语义检查静态 verify（acquire 幂等互斥/renew 限定/steal fence 递增/deleteSession 幂等）——spec 705/743 同构（spec 922） | [spec 922](docs/spec/922-lease-contract.md) |
| 观测治理 | 扩缩容建议缩容滞回 | stabilizeWindows opt-in（回零需连续 N 空闲窗，扩容即时不对称）——HPA stabilization window（spec 923） | [spec 923](docs/spec/923-scaling-hysteresis.md) |
| 观测治理 | 观测存储水位读面 | InMemoryObservabilityStore.watermark（activeSessions/totalRecords vs 上限 + 逐出透传）——Redis INFO memory（spec 924） | [spec 924](docs/spec/924-obs-watermark.md) |
| 观测治理 | 会话索引存量水位读面 | InMemorySessionIndexStore.watermark（indexedSessions + maxSessions=-1 显式无界）——spec 924 同构（spec 925） | [spec 925](docs/spec/925-index-watermark.md) |
| memory | 事实衰减预报读法 | FactDecayPolicy.turnsUntilFloor（逆函数 ⌈h×log2(conf/floor)⌉，floor=0 永不衰出 MAX_VALUE）——predict_linear 同思路，衰减预警→主动 reinforce（spec 926） | [spec 926](docs/spec/926-decay-forecast.md) |
| exec 治理 | 软截止预警集成 | HarnessToolCallingManager 软截止窗（setSoftDeadlineWindow + 一次性 WARN/counter + beginTurn 复位）——spec 921 集成留位兑现（spec 927） | [spec 927](docs/spec/927-soft-deadline-integration.md) |
| 会话治理 | pruned run 审计查询 | EvalQueryService.runsWithPruned（pruned 项筛选 + PrunedRunSummary 降序投影）——spec 901 剪枝审计入口（spec 928） | [spec 928](docs/spec/928-pruned-query.md) |
| 模型韧性 | webhook 死信环形上限 | MAX_DEAD_LETTERS=256 + evictOldestDeadIfFull（createdAt 升序丢最旧保最新）——dead.* 存量从无限累积到环形封顶，有界纪律（spec 937） | [spec 937](docs/spec/937-deadletter-cap.md) |
| 观测治理 | Redis 语义向量缓存（补登） | I/J 会话产出引用补全 | [spec 125](docs/spec/125-redis-semantic-vector-cache.md) |
| 观测治理 | 700-capability-decision-audit（补登） | I/J 会话产出引用补全 | [spec 700](docs/spec/700-capability-decision-audit.md) |
| 观测治理 | 语义缓存权重预算驱逐（补登） | I/J 会话产出引用补全 | [spec 701](docs/spec/701-semantic-cache-weight-budget.md) |
| 观测治理 | 702-circuit-transition-journal（补登） | I/J 会话产出引用补全 | [spec 702](docs/spec/702-circuit-transition-journal.md) |
| 观测治理 | 703-routing-health-dampener（补登） | I/J 会话产出引用补全 | [spec 703](docs/spec/703-routing-health-dampener.md) |
| 观测治理 | 704-prompt-composition（补登） | I/J 会话产出引用补全 | [spec 704](docs/spec/704-prompt-composition.md) |
| 观测治理 | Redis 键命名空间碰撞审计（补登） | I/J 会话产出引用补全 | [spec 705](docs/spec/705-redis-key-layout-audit.md) |
| 观测治理 | 706-mcp-directory-diff（补登） | I/J 会话产出引用补全 | [spec 706](docs/spec/706-mcp-directory-diff.md) |
| 观测治理 | 707-spill-pair-audit（补登） | I/J 会话产出引用补全 | [spec 707](docs/spec/707-spill-pair-audit.md) |
| 观测治理 | 评估项结果记忆化（补登） | I/J 会话产出引用补全 | [spec 708](docs/spec/708-eval-item-memoization.md) |
| 观测治理 | 709-experiment-expiry（补登） | I/J 会话产出引用补全 | [spec 709](docs/spec/709-experiment-expiry.md) |
| 观测治理 | 710-experiment-holdout（补登） | I/J 会话产出引用补全 | [spec 710](docs/spec/710-experiment-holdout.md) |
| 观测治理 | 711-turn-sequence-audit（补登） | I/J 会话产出引用补全 | [spec 711](docs/spec/711-turn-sequence-audit.md) |
| 观测治理 | 712-span-health-summary（补登） | I/J 会话产出引用补全 | [spec 712](docs/spec/712-span-health-summary.md) |
| 观测治理 | 数据集标签与过滤（补登） | I/J 会话产出引用补全 | [spec 713](docs/spec/713-dataset-tags.md) |
| 观测治理 | 714-similarity-evaluator（补登） | I/J 会话产出引用补全 | [spec 714](docs/spec/714-similarity-evaluator.md) |
| 观测治理 | 715-format-preserving-mask（补登） | I/J 会话产出引用补全 | [spec 715](docs/spec/715-format-preserving-mask.md) |
| 观测治理 | Todo 陈旧度审计（补登） | I/J 会话产出引用补全 | [spec 716](docs/spec/716-todo-staleness.md) |
| 观测治理 | 717-fact-conflict-audit（补登） | I/J 会话产出引用补全 | [spec 717](docs/spec/717-fact-conflict-audit.md) |
| 观测治理 | 评估通过率漂移基线（补登） | I/J 会话产出引用补全 | [spec 718](docs/spec/718-eval-drift-baseline.md) |
| 观测治理 | 719-provider-ratelimit-signals（补登） | I/J 会话产出引用补全 | [spec 719](docs/spec/719-provider-ratelimit-signals.md) |
| 观测治理 | 721-chunking-embedding-model（补登） | I/J 会话产出引用补全 | [spec 721](docs/spec/721-chunking-embedding-model.md) |
| 观测治理 | MCP 并发占用视图（补登） | I/J 会话产出引用补全 | [spec 722](docs/spec/722-mcp-concurrency-views.md) |
| 观测治理 | 分批嵌入 yml 装配（补登） | I/J 会话产出引用补全 | [spec 723](docs/spec/723-chunking-embedding-assembly.md) |
| 观测治理 | 724-state-ttl-coverage（补登） | I/J 会话产出引用补全 | [spec 724](docs/spec/724-state-ttl-coverage.md) |
| 观测治理 | 725-dampener-ramp（补登） | I/J 会话产出引用补全 | [spec 725](docs/spec/725-dampener-ramp.md) |
| 观测治理 | 事件类型分布读数（补登） | I/J 会话产出引用补全 | [spec 726](docs/spec/726-event-type-distribution.md) |
| 观测治理 | 727-redis-key-layout-health（补登） | I/J 会话产出引用补全 | [spec 727](docs/spec/727-redis-key-layout-health.md) |
| 观测治理 | spill 配对健康面（补登） | I/J 会话产出引用补全 | [spec 728](docs/spec/728-spill-pair-health.md) |
| 观测治理 | 729-observability-capacity-health（补登） | I/J 会话产出引用补全 | [spec 729](docs/spec/729-observability-capacity-health.md) |
| 观测治理 | 限流头跨供应商归一解析（补登） | I/J 会话产出引用补全 | [spec 730](docs/spec/730-ratelimit-header-normalization.md) |
| 观测治理 | 评估分数分布解析（补登） | I/J 会话产出引用补全 | [spec 731](docs/spec/731-eval-score-analytics.md) |
| 观测治理 | 事件 payload 大小审计（补登） | I/J 会话产出引用补全 | [spec 732](docs/spec/732-event-payload-size-audit.md) |
| 观测治理 | 提示词使用缺口读数（补登） | I/J 会话产出引用补全 | [spec 733](docs/spec/733-prompt-usage-gaps.md) |
| 观测治理 | 数据集指纹变更信号（补登） | I/J 会话产出引用补全 | [spec 734](docs/spec/734-fingerprint-change-signal.md) |
| 观测治理 | 735-event-pairing-audit（补登） | I/J 会话产出引用补全 | [spec 735](docs/spec/735-event-pairing-audit.md) |
| 观测治理 | 736-span-parent-integrity（补登） | I/J 会话产出引用补全 | [spec 736](docs/spec/736-span-parent-integrity.md) |
| 观测治理 | 737-response-cache-weight-budget（补登） | I/J 会话产出引用补全 | [spec 737](docs/spec/737-response-cache-weight-budget.md) |
| 观测治理 | 738-session-export-size-audit（补登） | I/J 会话产出引用补全 | [spec 738](docs/spec/738-session-export-size-audit.md) |
| 观测治理 | 739-event-presence-gate（补登） | I/J 会话产出引用补全 | [spec 739](docs/spec/739-event-presence-gate.md) |
| 观测治理 | 供应商限流信号 stats 接线（补登） | I/J 会话产出引用补全 | [spec 740](docs/spec/740-provider-signals-stats.md) |
| 观测治理 | 741-shared-fact-footprint（补登） | I/J 会话产出引用补全 | [spec 741](docs/spec/741-shared-fact-footprint.md) |
| 观测治理 | 742-sweep-retained-readout（补登） | I/J 会话产出引用补全 | [spec 742](docs/spec/742-sweep-retained-readout.md) |
| 观测治理 | 导出脱敏命中计数（补登） | I/J 会话产出引用补全 | [spec 743](docs/spec/743-sanitizer-hit-counts.md) |
| 观测治理 | 744-hybrid-ranker-readout（补登） | I/J 会话产出引用补全 | [spec 744](docs/spec/744-hybrid-ranker-readout.md) |
| 观测治理 | 响应缓存权重预算 yml 装配（补登） | I/J 会话产出引用补全 | [spec 745](docs/spec/745-response-cache-weight-assembly.md) |
| 观测治理 | 746-deny-by-capability（补登） | I/J 会话产出引用补全 | [spec 746](docs/spec/746-deny-by-capability.md) |
| 观测治理 | 747-threshold-counterfactual（补登） | I/J 会话产出引用补全 | [spec 747](docs/spec/747-threshold-counterfactual.md) |
| 观测治理 | 748-execution-policy-readout（补登） | I/J 会话产出引用补全 | [spec 748](docs/spec/748-execution-policy-readout.md) |
| 观测治理 | 749-final-verification（补登） | I/J 会话产出引用补全 | [spec 749](docs/spec/749-final-verification.md) |
| 评估闭环 | gate 阈值漂移读面 | EvalGate.thresholdDrift（相邻判定 threshold 变化次数 + sampled 投影）——「CI 红了就调阈值」流程不健康信号显形，914 历史面聚合视图（spec 938） | [spec 938](docs/spec/938-threshold-drift.md) |
| 模型韧性 | outbox due 索引孤儿审计 | WebhookOutbox.orphanIndexCount（indexEntry 存在但主记录缺失的条目数）——配对完整性思想，删除时序缺陷信号（spec 957） | [spec 957](docs/spec/957-orphan-index-audit.md) |
| 评估闭环 | gate 历史按数据集过滤读面 | EvalGate.historyOf（datasetName 精确匹配新→旧投影，null/blank fail-fast）——spec 914 历史面查询视图（spec 956） | [spec 956](docs/spec/956-history-filter.md) |
| 评估闭环 | 快照数据集隔离性深验 | 三断言（源变靶不变/删源靶活/nextId 续起不碰撞）——spec 187 隔离语义收口薄加固轮（spec 949） | [spec 949](docs/spec/949-snapshot-isolation.md) |
| 评估闭环 | 数据集输入长度画像 | EvalDatasetStore.inputLengthProfile（count/totalChars/avgChars/maxChars/p95Chars）——评估成本画像，超长项预算失控点（spec 942） | [spec 942](docs/spec/942-input-profile.md) |
| 会话治理 | 摘要存储水位读面 | InMemorySummaryStore.watermark（activeSessions/maxSessions）——水位系列第三站（spec 950） | [spec 950](docs/spec/950-summary-watermark.md) |
| 记忆治理 | token 估算统计读面 | TokenEstimateStats——token 估算链路计数对账（spec 1033） | [spec 1033](docs/spec/1033-token-estimate-stats.md) |
| 成本预算 | token 估算调用量与总量读面 | CharHeuristicTokenEstimator 静态三计数（estimateCalls/batchCalls/totalEstimatedTokens）+ stats()/resetForTest()——预算面估算总量显形（spec 1033） | [spec 1033](docs/spec/1033-token-estimate-stats.md) |
| 记忆治理 | 词法排序统计读面 | LexicalRankStats——检索词法排序链路计数（spec 1034） | [spec 1034](docs/spec/1034-lexical-rank-stats.md) |
| 技能治理 | 词法排序生效计数读面 | LexicalSkillRanker 嵌套 RankStats（runs/reordered——排序器空转显形）+ stats()——混合排序生效水位（spec 1034） | [spec 1034](docs/spec/1034-lexical-rank-stats.md) |
| 模型韧性 | MCP 解析统计读面 | McpParseStats——MCP 工具清单解析成败计数（spec 1036） | [spec 1036](docs/spec/1036-mcp-parse-stats.md) |
| MCP 热插拔 | properties 装配解析统计读面 | PropertiesToolSetProvider 静态三计数（servers/bindings/bindingsSkipped——非 Map binding 项静默跳过显形）+ parseStats()——清单解析统计思想（spec 1036） | [spec 1036](docs/spec/1036-mcp-parse-stats.md) |
| 安全 | 权限统计读面 | PermissionStats——权限判定分布聚合（spec 1037） | [spec 1037](docs/spec/1037-permission-stats.md) |
| memory | 摘要桥接统计读面 | SummaryBridgeStats——摘要桥接链路计数对账（spec 1038） | [spec 1038](docs/spec/1038-summary-bridge-stats.md) |
| prompt 域 | 提示词解析统计读面 | PromptResolutionStats——提示词解析成败计数对账（spec 1039） | [spec 1039](docs/spec/1039-prompt-resolution-stats.md) |
| 工具治理 | todo 动作分布读面 | TodoTool 嵌套 TodoActionStats（list/upsert/remove/clear/other 白名单五桶有界）+ actionStats()——操作分布显形（spec 1040） | [spec 1040](docs/spec/1040-todo-action-stats.md) |
| 会话治理 | time-travel fork 操作计数读面 | SessionForks 嵌套 ForkStats（forksCreated/messagesCopied）+ stats()——LangGraph fork 使用水位（spec 1041） | [spec 1041](docs/spec/1041-fork-operation-stats.md) |
| 安全 | 签名验钥分布读面 | SigningKeyRing 嵌套 KeyRingStats（verifyAttempts/verifyKeyMisses/rotations + activeVersion/minVerifyVersion 上下文）+ stats()——cert-manager/keyring ops 思想（spec 1042） | [spec 1042](docs/spec/1042-keyring-stats.md) |
| webhook 投递 | 围栏裁决分布读面 | SequenceFence 嵌套 FenceVerdictStats（CONTINUE/GAP/DUPLICATE/RESET/STALE 五态分桶守恒）+ verdictStats()——Kafka epoch/consumer-lag 谱系续 spec 303（spec 1043） | [spec 1043](docs/spec/1043-fence-verdict-stats.md) |
| 观测治理 | J 系审计 R44（补登） | J 会话 R44 产出引用补全（spec 1044） | [spec 1044](docs/spec/1044-j-audit-r44.md) |
| 观测治理 | 沙箱判定统计读面（补登） | J 会话 R45 产出引用补全（spec 1045） | [spec 1045](docs/spec/1045-sandbox-verdict-stats.md) |
| 工具计量 | write_file 写入量水位与拒绝分桶 | 写入吞吐与拒绝原因分布显形，五桶守恒（spec 1046） | [spec 1046](docs/spec/1046-writefile-stats.md) |
| 工具计量 | read_file 读量水位与拒绝分桶 | 读吞吐与拒绝原因分布显形，四桶守恒，与写侧轴间可比（spec 1047） | [spec 1047](docs/spec/1047-readfile-stats.md) |
| 工具计量 | SSRF 守卫判定分布读面 | 出网校验放行/拒绝按原因分桶显形，五桶守恒（spec 1048） | [spec 1048](docs/spec/1048-ssrf-guard-stats.md) |
| 工具计量 | http_request 请求量水位与结果分布 | 请求送达率与六拒绝桶分布显形，参数/环境分轴（spec 1049） | [spec 1049](docs/spec/1049-httptool-stats.md) |
| 观测治理 | J 系阶段对账审计 R50 | J 会话 R46–R49 工件对账 + spec 1048 跨会话冲突合成留痕（spec 1050） | [spec 1050](docs/spec/1050-j-audit-r50.md) |
| 工具计量 | 命令黑名单拦截判定读面 | 黑名单命中/放行比显形，二桶守恒（spec 1051） | [spec 1051](docs/spec/1051-blacklist-stats.md) |
| 工具计量 | run_command 执行结果分布读面 | 执行结局九桶守恒，参数/运行时分轴（spec 1052） | [spec 1052](docs/spec/1052-runcommand-stats.md) |
| 溢出治理 | evict_handle 逐出判定读面 | 模型主动逐出采用率与拒绝分桶显形，三桶守恒（spec 1053） | [spec 1053](docs/spec/1053-evict-stats.md) |
| 溢出治理 | str_replace 编辑判定读面 | 编辑成功与 notFound/ambiguous 失败模式分桶显形，六桶守恒（spec 1054） | [spec 1054](docs/spec/1054-strreplace-stats.md) |
| 记忆治理 | 情景记忆读写双守恒读面 | 情景库写入量与召回命中率显形，双守恒（spec 1055） | [spec 1055](docs/spec/1055-episodic-stats.md) |
| 模型韧性 | 崩循环探测器类级水位读面 | OPEN 总量/封顶截断量/循环检出/恢复四计数显形（spec 1056） | [spec 1056](docs/spec/1056-crashloop-watch-stats.md) |
| 技能治理 | skill_search 搜索判定读面 | 搜索命中率与零结果率显形，四桶守恒（spec 1057） | [spec 1057](docs/spec/1057-skillsearch-stats.md) |
| 技能治理 | SemanticSkillRanker 排序分布深化 | 排序调用/跳过/降级三计数显形（spec 1136） | [spec 1136](docs/spec/1136-ranker-dist.md) |
| 技能治理 | SkillAdmin×Search 可见性联动组合测试 | 发布/下架→搜索命中联动钉住（spec 1095） | [spec 1095](docs/spec/1095-adminsearch-combo.md) |
| MCP 治理 | 工具集轮询提供器读面 | 热更新轮询三桶守恒显形，失败率可对账（spec 1058） | [spec 1058](docs/spec/1058-toolsetpoll-stats.md) |
| 记忆治理 | compact_now 手动压缩判定读面 | 模型主动压缩采用率与四结局桶守恒显形（spec 1059） | [spec 1059](docs/spec/1059-compactnow-stats.md) |
| 观测治理 | J 系阶段对账审计 R60 | J 会话 R51–R59 工件对账 + 七域读面布局盘点（spec 1060） | [spec 1060](docs/spec/1060-j-audit-r60.md) |
| 观测治理 | Dashboard HTTP 状态分布读面 | 七状态码结局桶守恒显形（spec 1061） | [spec 1061](docs/spec/1061-dashhttp-stats.md) |
| 观测治理 | J 系阶段对账审计 R100 | 百轮节点：R91–R99 工件对账 + 组合测试系列成型盘点（spec 1100） | [spec 1100](docs/spec/1100-j-audit-r100.md) |
| 观测治理 | J 系阶段对账审计 R110 | J 会话 R101–R109 工件对账 + 首验一次通过（spec 1110） | [spec 1110](docs/spec/1110-j-audit-r110.md) |
| 观测治理 | J 系阶段对账审计 R120 | J 会话 R111–R119 工件对账 + 组合测试系列深度盘点（spec 1120） | [spec 1120](docs/spec/1120-j-audit-r120.md) |
| 观测治理 | J 系阶段对账审计 R80 | J 会话 R71–R79 工件对账 + 九域读面布局盘点（spec 1080） | [spec 1080](docs/spec/1080-j-audit-r80.md) |
| 观测治理 | J 系阶段对账审计 R90 | J 会话 R81–R89 工件对账 + README 1085 吞噬修复（spec 1090） | [spec 1090](docs/spec/1090-j-audit-r90.md) |
| 观测治理 | 冒烟补全轮 | TodoTool/ToolSlowLog 独立冒烟纳入（spec 1098） | [spec 1098](docs/spec/1098-smoke-ext2.md) |
| 观测治理 | 冒烟清单扩展轮 | ArchivePurgeJob/SpillCipher 纳入 15 读面冒烟（spec 1091） | [spec 1091](docs/spec/1091-readout-ext.md) |
| 观测治理 | 冒烟清单第三扩展轮 | SkillAdmin/SpillService 纳入 17 读面冒烟（spec 1102） | [spec 1102](docs/spec/1102-smoke-ext3.md) |
| 观测治理 | J 系读面统一契约冒烟轮 | 15 读面非负/归零/稳定三性质元验证（spec 1083） | [spec 1083](docs/spec/1083-readout-contract-smoke.md) |
| 观测治理 | ObservabilityAdvisor 流式路径分支补测 | K 会话 R13：流式回调链 TTFT/TPOT/流错终态测试基建（spec 1212） | [spec 1212](docs/spec/1212-advisor-stream.md) |
| 观测治理 | J 系阶段对账审计 R70 | J 会话 R61–R69 工件对账 + 七域读面布局收口（spec 1070） | [spec 1070](docs/spec/1070-j-audit-r70.md) |
| 工具计量 | http_request 受控头丢弃显形 | 黑名单头试探频次量化（spec 1071） | [spec 1071](docs/spec/1071-headerdrop-stats.md) |
| 内容防御 | PII 检测引擎读面 | 引擎原生匹配数与业务上报数双层对账显形（spec 1072） | [spec 1072](docs/spec/1072-piidetector-stats.md) |
| 内容防御 | PII 出站脱敏读面 | 出站事件脱敏三结局分布与 fail-open 显形（spec 1211） | [spec 1211](docs/spec/1211-pii-red-stats.md) |
| 过程治理 | O 系 R72 周期对账 | R67–R71 五个新公共类型快照补登 + 三门全绿 + Wave 13 排程；对账口径延续十二波惯例（spec 1871） | [spec 1871](docs/spec/1871-o-r72-reconciliation.md) |
| 背压治理 | 突发信用账户 | BurstCreditAccount——基准速率蓄水封顶+突发透支水位+枯竭降速回基准（不拒不崩——限流拒绝之外的第三态）+burstHeadroomMillis 余量换算+exhaustions 枯竭计数——AWS CPU credit/T3 unlimited 语义，与 GCRA（拒绝）/PrefetchCreditWindow（在飞上限）三足（spec 1872） | [spec 1872](docs/spec/1872-burst-credit-account.md) |
| 工具执行 | 调度松弛量 | ScheduleFloat——逐任务松弛量（最晚可延迟不拖总工期）：正向 ES+反向到汇距双向 DP，float=LS−ES（关键任务 0、非关键有缓冲可让路）——CPM float/slack 思想，与 CriticalPathLength 配对成下界+缓冲双面，float 分布=编排刚性度（spec 1873） | [spec 1873](docs/spec/1873-schedule-float.md) |
| 护栏治理 | 正则风险审计 | RegexRiskAudit——ReDoS 形态三档分级（嵌套量词 (a+)+ 指数回溯/量词组内交替/重叠交替分支翻倍；多形态叠加即 DANGEROUS）+findings 可解释+转义感知——OWASP 正则 DoS/SafeRegex 惯例思想，手写正则入库前静态防线（诚实边界：非完备启发式）（spec 1874） | [spec 1874](docs/spec/1874-regex-risk-audit.md) |
| 模型韧性 | 级联失败暴露读面 | CascadeExposure——依赖边风险权重（流量占比×下游失败率=期望损失面）排序最脆边+活风险（下游不健康且权重>0 正在传导）vs 静风险（埋着）分诊+totalWeight 全图损失面——级联失败分析（Hystrix 舱壁思想源头）惯例，熔断逐点防护之外的拓扑脆性事前排序（spec 1875） | [spec 1875](docs/spec/1875-cascade-exposure.md) |
| 工程门禁 | P 会话 2000 系对账门 | PSession2000LedgerAuditTest——150 轮工件链四面互证（spec 2000–2149 ↔ README 行 ↔ T3101+2(N−2000) 票对 ↔ impl 1551+(N−2000)），spec 起点断言 2000 严格递增，L/O 系预防式公式族第三应用（spec 2000） | [spec 2000](docs/spec/2000-psession-ledger-audit.md) |
| 观测计量 | HLL 基数素描 | HllCardinalitySketch——定容寄存器流式 distinct 计数（FNV-1a 64+splitmix64 确定性散列，幂等 rank-max）+调和平均估计（小值域线性计数修正）+merge 逐位 max 并集聚合+1.04/√m 误差界读数——Redis HLL/Flajolet 思想，与布伦粗筛互补（基数 vs 存在性）（spec 2001） | [spec 2001](docs/spec/2001-hll-cardinality-sketch.md) |
| 观测计量 | 指数直方图滑窗计数 | ExponentialWindowCounter——O(log N) 空间滑窗事件计数：(capacity, first, last) 三元组桶同容量≤2 合并翻倍（set 回原位保序）+过期惰性清出+全界内计全/跨界计半+errorBound 自描述误差界——Datar-Indyk 指数直方图思想，与精确窗计数器对照（省空间近似 vs 精确）（spec 2002） | [spec 2002](docs/spec/2002-exponential-window-counter.md) |
| 记忆治理 | 记忆强度三分量评分 | MemoryStrengthScore——mem0 思想：recency 2^(−Δt/半衰期) 衰减+frequency 对数饱和（边际递减）+importance [0,1] 钳制，加权和归一恒 [0,1]，Weights 三旋钮可偏置——recall 排序与压缩淘汰统一强度键（spec 2003） | [spec 2003](docs/spec/2003-memory-strength-score.md) |
| 健康治理 | φ 累积故障嫌疑度 | PhiAccrualFailureDetector——心跳间隔滑动窗正态模型，φ=−log₁₀(P(此久未心跳|正常))∈[0,12] 连续嫌疑度取代二值跳变（std 下界防规律退化，样本不足恒 0，窗滑动重学节奏）——Hayashibara/Finagle φ-accrual 思想，φ>1 预警/φ>4 判失联分档（spec 2004） | [spec 2004](docs/spec/2004-phi-accrual-detector.md) |
| 过程治理 | P 系 R6 周期对账 | Wave 1 四新类型 + O 系四类型代补登（快照 998→1006，CONTEXT 897→905）+ 全仓 verify 三门绿 + 对账门核账；R6k 对账节奏首例（spec 2005） | [spec 2005](docs/spec/2005-p-r6-reconciliation.md) |
| 一致性 | LWW 最后写入胜利寄存器 | LastWriteWinsRegister——多写者无协调收敛：(timestamp, writerId) 字典序定胜负+ts 平局 writer 字典序确定性仲裁（无随机可回放）+完全幂等+merge 跨实例收敛+conflict/superseded 双对账面（时钟平局/乱序到达频率）——Dynamo/CRDT LWW 思想（spec 2006） | [spec 2006](docs/spec/2006-lww-register.md) |
| 缓存治理 | 频率素描 | FrequencySketch——4bit Count-Min 计数板缓存准入门控：相邻两槽较小者 increment（防独占倾斜）+frequency=min 下界语义（碰撞只低估，同 key 反复 ≈n/2 序不变）+饱和 15 封顶——Caffeine W-TinyLFU 思想，newcomer<victim 拒准入防扫描污染，与 HLL/指数直方图三足（频率序/distinct 数/近窗数）（spec 2007） | [spec 2007](docs/spec/2007-frequency-sketch.md) |
| 韧性治理 | 重试主机排除 | RetryHostExclusion——重试不落同一坏端点：冷却窗（默认 30s）内失败者从候选剔除（序保持路由权重序不动），全排除回退全量（排除是偏好不是硬门，Envoy 语义）+再失败冷却顺延+excludedCount 全排除回退态显形——Envoy retry host predicate 思想，与离群驱逐互补（短窗让位 vs 长时统计排除）（spec 2008） | [spec 2008](docs/spec/2008-retry-host-exclusion.md) |
| 策略治理 | 特性开关求值器 | FlagEvaluator——求值永不抛出（OpenFeature 思想）：未注册 FLAG_NOT_FOUND 空值/targeting 命中 TARGETING_MATCH/未命中 STATIC/谓词炸 DEFAULT 兜底且 ERROR 同记——reasonCounts 五态分布让幽灵 flag 与谓词病灶分别显形（spec 2009） | [spec 2009](docs/spec/2009-flag-evaluator.md) |
| 护栏治理 | 判定决策缓存 | DecisionCache——同输入判定 TTL 内短路（OPA/Cedar decision cache 思想）：过期惰性清除（expirations 与 misses 分计——过期非未见过）+重判回填刷新时间戳+超容 LRU 驱逐+invalidate 显式失效（策略热更新精准失效）+hitRate 缓存有效性证（命中率低该摘除直判）（spec 2010） | [spec 2010](docs/spec/2010-decision-cache.md) |
| 过程治理 | P 系 R12 周期对账 | Wave 2 五新类型快照补登（1006→1011，CONTEXT 905→910）+ 全仓 verify 三门绿 + push 网络波动定式（内容轮不阻塞、对账轮集中补推）（spec 2011） | [spec 2011](docs/spec/2011-p-r12-reconciliation.md) |
| 执行治理 | 抢占重算账本 | PreemptionLedger——抢占双面账：victim 已做作废（浪费面）+抢占方提前完成（收益面）同笔入账，netBenefit 负=降阈值信号、wasteRatio=抢占健康度、recomputeRate=victim 重算发生率（幂等一次）——vLLM preemption/recompute 思想，抢占阈值从拍脑袋变读账定夺（spec 2012） | [spec 2012](docs/spec/2012-preemption-ledger.md) |
| 健康治理 | 启动豁免窗追踪 | StartupGraceTracker——慢启动失败不计故障账（K8s startup probe 思想）：豁免窗内未毕业失败豁免/窗外或毕业或未锚定一律计账+首次成功即毕业幂等（豁免给冷启动不给僵尸）+重启重锚窗口重算+activeGraces 受宽容面——冷启动抖动不稀释 φ 检测器真故障信号（spec 2013） | [spec 2013](docs/spec/2013-startup-grace-tracker.md) |
| 恢复治理 | 键压缩日志语义 | KeyCompaction——状态日志按键压缩取终态（Kafka log compaction 思想）：同 key 取 maxSeq（乱序幂等——保留者与输入顺序无关）+payload null 即 tombstone 墓碑显式删除（墓碑后复活/旧墓碑不遮新值）+compactionRatio 历史冗余对账——读取一遍压缩即终态，删除不再各自猜（spec 2014） | [spec 2014](docs/spec/2014-key-compaction.md) |
| 观测计量 | 最小 RTT 滑窗滤波器 | MinRttTracker——对冲/超时的真时延基线（TCP BBR min-RTT 思想）：窗内最小（10 分钟默认——均值被队列膨胀污染，最小最贴真传播时延）+滑出惰性清除次小接管（网络恶化基线可上浮不永久过时）+lastFreshMinAt 新鲜度显形（持平不刷新）——与对冲延迟策略正交供其真基线（spec 2015） | [spec 2015](docs/spec/2015-min-rtt-tracker.md) |
| 会话治理 | 布谷鸟过滤器 | CuckooFilter——近似成员筛可删除（Cuckoo filter 思想）：16bit 指纹双桶（i2=i1^hash(fp) 异或自定位）+双满轮流踢出重排（确定性无随机可回放；踢尽拒插计 overflowed 扩容信号）+delete 撤销指纹槽（布伦硬缺口——检疫集不再只增不减）+无假阴性——与布伦互补（见过吗不可撤 vs 还在吗可撤）（spec 2016） | [spec 2016](docs/spec/2016-cuckoo-filter.md) |
| 过程治理 | P 系 R18 周期对账 | Wave 3 五新类型快照补登（1011→1016，CONTEXT 910→915）+ 全仓 verify 三门绿（spec 2017） | [spec 2017](docs/spec/2017-p-r18-reconciliation.md) |
| 记忆治理 | 检索强度重排接线 | RecallStrengthReranker——mem0 思想管线落地：finalScore = 0.7×相关度 + 0.3×记忆强度（MemoryStrengthScore 接进 recall 排序面——同相关度下新热记忆靠前，「相关且新鲜」优先）；开闭装饰不侵入 RecallSearch 本体，两极退化（1 纯相关度/0 纯强度），TIME 模式退化纯强度序（spec 2018） | [spec 2018](docs/spec/2018-recall-strength-reranker.md) |
| 策略治理 | QoS 资源声明分级 | QosClassifier——按声明三分（K8s QoS Classes 思想）：全维 request==limit 保额保量 GUARANTEED 背压受保护/有保底可突发 BURSTABLE/零声明 BEST_EFFORT 先让位（任一零声明拉低整体）+evictionRank 驱逐序+shouldYield 保护线让位判定——驱逐与保护由分级驱动不再逐实例拍脑袋（spec 2019） | [spec 2019](docs/spec/2019-qos-classifier.md) |
| 健康治理 | 冷启动豁免 φ 嫌疑门 | GraceAwareFailureDetector——spec 2004 φ 检测 × spec 2013 豁免窗组合件：窗内失败不喂 φ（冷启动噪声不毒化故障基线，豁免账另记）、首拍成功即毕业；verdict 三态 HEALTHY/GRACE_HOLD（高嫌疑但豁免中观望）/CONFIRMED（毕业或窗外判失联）——宽容冷启动不放过真死（spec 2020） | [spec 2020](docs/spec/2020-grace-aware-failure-detector.md) |
| 并发治理 | 就绪等待门 | WaitForReadyGate——依赖未就绪时的有预算中间态（gRPC wait_for_ready 思想）：四态询问（PASS/QUEUED 预算内排队/FAIL_FAST 立即失败/QUEUE_FULL 预算满拒绝防积压）+就绪批量排空（drained/drainBatches 对账）+再失就绪重计——冷启动窗口请求不弹掉且积压有界（spec 2021） | [spec 2021](docs/spec/2021-wait-for-ready-gate.md) |
| 一致性 | 复制计数器 | ReplicatedCounter——多实例聚合计数免双计（CRDT G/PN-Counter 思想）：per-writer 分量（正计数负扣减合一）+merge 逐分量 max（at-least-once 重传不双计）+value=Σ分量——幂等/交换/结合三性质齐备，与 LWW 寄存器成对（值域定序 vs 计数收敛）（spec 2022） | [spec 2022](docs/spec/2022-replicated-counter.md) |
| 过程治理 | P 系 R24 周期对账 | Wave 4 五新类型快照补登（1016→1021，CONTEXT 915→920）+ 全仓 verify 三门绿 + 接线轮/组合件模式首档（spec 2023） | [spec 2023](docs/spec/2023-p-r24-reconciliation.md) |
| 执行治理 | 老化优先级队列 | AgingPriorityQueue——等待生息反饥饿（OS 调度 aging 思想）：有效优先级 = base + 等待秒×老化速率（默认 1 点/秒），等得够久的低优先级必反超新来的高优先级——饥饿有时间下界；同分 FIFO 保序、零速率退化静态、快照观测面不出队——与 SpawnPriority 正交互补（静态序+时间升值）（spec 2024） | [spec 2024](docs/spec/2024-aging-priority-queue.md) |
| 路由治理 | 一致性哈希环 | ConsistentHashRing——键→节点归属最小迁移（Dynamo/Ketama 虚节点环思想）：addNode 铺 160 虚节点均匀弧段+顺时针 ceiling 归属回绕+删节点只迁其弧段（迁移量=原份额 ≈1/n 非全量）+加节点只吸收近段——节点增减不再全量重路由（spec 2025） | [spec 2025](docs/spec/2025-consistent-hash-ring.md) |
| 执行治理 | 加权公平调度器 | WeightedFairScheduler——多流权重公平分享（网络 DRR deficit 轮询思想）：粘性轮内消费（当前流积分可负担连续出队不重入账，用尽让出入账下一流 quantum×weight）——长期服务比 ≈ 权重比（3:1 长跑收敛），高权重多得不独占；空流入环 deficit 从零（空闲不积累特权）+servedByStream 公平对账——严格优先级饿死与轮询无权的中间态（spec 2026） | [spec 2026](docs/spec/2026-weighted-fair-scheduler.md) |
| 观测计量 | EWMA 估计器 | EwmaEstimator——指标观测平滑层（Netflix/Finagle 口径）：首样本直接锚定+estimate=α×新+(1−α)×旧（α∈(0,1] 默认 0.2≈5 样本记忆，1 直通最新小 α 惯性大）——尖峰被稀释趋势仍跟随，告警调参 α 旋钮定灵敏度（spec 2027） | [spec 2027](docs/spec/2027-ewma-estimator.md) |
| 执行治理 | 并发组闸 | ConcurrencyGroupGate——同组任务互斥与取代（GitHub Actions concurrency group 思想）：tryEnter 三态（属主授予/重入幂等/SUPERSEDED cancelInProgress 新者接管旧者取消——只留最新防堆积、BUSY_REJECTED 在跑者优先）+complete 属主栅栏（仅现属主释放，被取代者迟到完成拦下 fencedCompletions 竞态显形）——与就绪门互补（同类互斥 vs 依赖就绪姿态）（spec 2028） | [spec 2028](docs/spec/2028-concurrency-group-gate.md) |
| 过程治理 | P 系 R30 周期对账 | Wave 5 五新类型快照补登（1021→1026，CONTEXT 920→925）+ 全仓 verify 三门绿（五波连续）+ 30/150=1/5 里程碑——调度原语族成谱系（spec 2029） | [spec 2029](docs/spec/2029-p-r30-reconciliation.md) |
| 工具治理 | 重定向预算 | RedirectBudget——HTTP 跟随重定向双防线（curl max-redirs × 环检测思想）：跳数预算硬界（超即停防恶意 302 拖死）+访问集环识破（A→B→A 配置错在预算空耗前识破，环跳不记账）+起点锚定（跳回 origin 即环）——与入参预算门同族（守出站跳数 vs 守入参规模）（spec 2030） | [spec 2030](docs/spec/2030-redirect-budget.md) |
| 门禁治理 | 必选检查聚合 | RequiredChecksRollup——多检查质量门单结论聚合（GitHub required checks rollup 思想）：任一必选 FAILURE 一票否决（优先于 PENDING——已失败不必等挂起者）/任一必选 PENDING 含未报 → 门未关/全绿放行/空集恒开；可选检查不阻断但 optionalFailures 显形（退化早期预警）——一票否决与挂起优先级一处定义不再逐处漂移（spec 2031） | [spec 2031](docs/spec/2031-required-checks-rollup.md) |
| 路由治理 | UCB1 选择器 | Ucb1Selector——选臂探索/利用平衡（多臂老虎机置信上界思想）：UCB = 均值 + c×√(2lnN/nᵢ)——尝试少的臂半径大自动探索，真值显形后收敛最优臂；未试臂优先每臂至少一试，不幸首抽不判死刑（半径护体）——均值贪心饿死病的根治原语，与延迟感知备模型排序互补（稳而全 vs 快而贪）（spec 2032） | [spec 2032](docs/spec/2032-ucb1-selector.md) |
| 恢复治理 | 维护触发器 | MaintenanceTrigger——死数据清理触发时机双口径（Postgres autovacuum 思想）：死/活 ≥ 阈值（默认 20%，live=0 全死必清）或距上次触发 ≥ 最大间隔（默认 24h 低流量兜底防陈化）——比例触发省写放大、间隔兜底防永不达标；触发记账频率即维护健康度——与归档 TTL 治理互补（何时清 vs 清什么）（spec 2033） | [spec 2033](docs/spec/2033-maintenance-trigger.md) |
| MCP 治理 | 工具溯源索引 | ToolProvenanceIndex——tool↔server 双向账与摘除后果显形（MCP 多 server 记账思想）：unregister 返回独供孤儿集合（共供不孤儿——其余 provider 仍在，摘 server 前影响面事前可见）+覆盖重注册（重连刷新先撤后铺）+conflictingTools 多源同名冲突面常驻显形（解析歧义源可治）——与目录差异报告互补（静态归属 vs 快照间变化）（spec 2034） | [spec 2034](docs/spec/2034-tool-provenance-index.md) |
| 过程治理 | P 系 R36 周期对账 | Wave 6 五新类型快照补登（1026→1031，CONTEXT 925→930；tools/mcp 两模块首入 P 系）+ 全仓 verify 三门绿（六波连续）（spec 2035） | [spec 2035](docs/spec/2035-p-r36-reconciliation.md) |
| 背压治理 | 双阈值迟滞水位门 | HysteresisWatermark——水位背压防抖（Netty write buffer watermark 思想）：超高水位停写、泄到低水位才恢复——两阈值间保持区状态延续不翻转（单阈值门抖动病根治）；toggleCount 翻转计数显形两阈值过近——与速率账户/准入门三形态互补（spec 2036） | [spec 2036](docs/spec/2036-hysteresis-watermark.md) |
| 恢复治理 | 同步副本追踪器 | InSyncTracker——成员同步性以追上时刻锚定（Kafka ISR 思想）：caughtUp 记最后追平时刻（不回拨）+距今超滞后阈值即剔出 ISR+追平自动回归（无需人工清单）+shrinkEvents 收缩计数（下游消费力紧张显形——扩张不计）——与后台选主互补（谁干 vs 谁跟得上）（spec 2037） | [spec 2037](docs/spec/2037-in-sync-tracker.md) |
| 观测计量 | SimHash 近重复指纹 | SimHashFingerprint——文本近重复 O(1) 位比较（Charikar SimHash 思想）：词元逐位散列每 bit 加权投票取符号 → 64 位指纹，相似文本汉明距离小（阈值 ≤3 默认判定）——万级条目近重复筛查不逐对比对；小词元集平局噪声诚实边界入档（≥16 词元较稳）——与数据集近重复读数正交互补（spec 2038） | [spec 2038](docs/spec/2038-simhash-fingerprint.md) |
| 技能治理 | 版本要求判定 | VersionRequirement——技能运行时要求兼容范围表达（npm semver range 思想）：^ 同主兼容含 0.x 锁定特例（^0.2.3 锁次/^0.0.3 锁补丁）、~ 次锁定、>=/> 含与不含界、精确、* 任意六算子；点分逐段短补 0（1.10>1.9 正确）+prerelease 低于同基段——错配显形在装载门口非运行深处（spec 2039） | [spec 2039](docs/spec/2039-version-requirement.md) |
| 溢出治理 | 可冻结分段缓冲 | FreezableBuffer——spill 缓冲并发快照安全与整段 flush 兼得（LSM memtable 不可变段思想）：可变段写满自动封冻为不可变段（冻结后只读——flush 期间并发快照不失效）+drainFrozen 整段取走（写放大的批量化，段容量即批大小旋钮）+快照追加序稳定+frozenCount 待持久化积压面——与组提交账面互补（机制件 vs 收益面）（spec 2040） | [spec 2040](docs/spec/2040-freezable-buffer.md) |
| 过程治理 | P 系 R42 周期对账 | Wave 7 五新类型快照补登（1031→1036，CONTEXT 930→935；skills/spill 两模块首入——P 系原语覆盖八模块）+ 全仓 verify 三门绿（七波连续）（spec 2041） | [spec 2041](docs/spec/2041-p-r42-reconciliation.md) |
| 执行治理 | 刻度轮定时器 | TickWheelTimer——海量定时任务 O(1) 调度（Netty hashed wheel timer 思想，纯逻辑无线程）：任务按延迟散进轮槽（W=64 默认）+跨轮挂圈数 rounds=(delay−1)/W+advance tick 推进只查当前槽（圈数尽到期/未尽 −1 留槽）+幂等重调度+cancel——调用方驱动 tick 确定性可回放（spec 2042） | [spec 2042](docs/spec/2042-tick-wheel-timer.md) |
| 技能治理 | Skill 管理操作读面 | create/update/publish/disable/delete 五操作独立计数显形（spec 1085） | [spec 1085](docs/spec/1085-skilladmin-stats.md) |
| 跑飞防护 | Runaway 预算 hook 判定读面 | 三硬顶终止/放行/禁用守恒显形（spec 1076） | [spec 1076](docs/spec/1076-runaway-stats.md) |
| 溢出治理 | read_range 回读判定读面 | 回读量与截断率分桶显形，五桶守恒（spec 1062） | [spec 1062](docs/spec/1062-readrange-stats.md) |
| 溢出治理 | SpillService 幂等复用读面 | 服务层五分支守恒显形，复用率可对账（spec 1101） | [spec 1101](docs/spec/1101-spillsvc-idem-stats.md) |
| 溢出治理 | offload→readBack 双轴闭环组合测试 | 溢出落盘与回读闭环计数一致性钉住（spec 1093） | [spec 1093](docs/spec/1093-offread-combo.md) |
| 溢出治理 | readRange×Spotlight 组合测试轮 | 溢出占位标记段与包裹回读幂等钉住（spec 1112） | [spec 1112](docs/spec/1112-readspot-combo.md) |
| 溢出治理 | 加密溢出×逐出组合测试轮 | 加密路径双读面独立性钉住（spec 1213） | [spec 1213](docs/spec/1213-cipherevict-combo.md) |
| 溢出治理 | cipher×readBack 组合测试轮 | 加密回读解密调用联动钉住（spec 1106） | [spec 1106](docs/spec/1106-cipherread-combo.md) |
| 溢出治理 | RangeReadEngine 引擎读面 | 切片引擎三模式调用分布显形（spec 1113） | [spec 1113](docs/spec/1113-enginereads-stats.md) |
| 溢出治理 | Spill 溢出 hook 判定读面 | 溢出触发率与降级动作五桶守恒显形（spec 1077） | [spec 1077](docs/spec/1077-spilloffload-stats.md) |
| 溢出治理 | Spill 加解密读面 | 加解密操作量与失败分布显形（spec 1079） | [spec 1079](docs/spec/1079-spillcipher-stats.md) |
| 溢出治理 | evict×readRange 逐出复活组合测试 | 逐出→回读→再逐出链双读面守恒钉住（spec 1114） | [spec 1114](docs/spec/1114-evictread-combo.md) |
| 溢出治理 | SemanticChunkIndex 操作读面 | 索引/查询/无效跳过操作分布显形（spec 1115） | [spec 1115](docs/spec/1115-chunkidx-op-stats.md) |
| 溢出治理 | offload×cipher 加密联动组合测试 | 加密配置真实生效联动钉住（spec 1097） | [spec 1097](docs/spec/1097-offcipher-combo.md) |
| 溢出治理 | spill 域 offload+evict 生命周期组合测试 | 溢出→逐出→回读链路双读面守恒钉住（spec 1089） | [spec 1089](docs/spec/1089-spillchain-readout.md) |
| 记忆治理 | 双时序事实台账操作读面 | 废止写入/两类查询/损坏蒸发四计数显形（spec 1063） | [spec 1063](docs/spec/1063-factledger-stats.md) |
| 注入防御 | 读侧 Spotlighting 包裹判定读面 | 包裹覆盖率与幂等跳过分桶显形，四桶守恒（spec 1064） | [spec 1064](docs/spec/1064-spotlight-stats.md) |
| 内容防御 | Spotlight×Moderation 顺序协作组合测试 | 包裹先于检查的双读面计数一致性钉住（spec 1092） | [spec 1092](docs/spec/1092-spotmod-combo.md) |
| 内容防御 | 双档读写四象限对照组合测试 | 读写对跨两档组装对称恒等钉住（spec 1103） | [spec 1103](docs/spec/1103-dualrw-symmetry.md) |
| 内容防御 | guard 三 hook 链顺序协作组合测试 | 脱敏→包裹→检查三读面计数一致钉住（spec 1094） | [spec 1094](docs/spec/1094-trihook-combo.md) |
| 工具计量 | todo×http 跨工具工作流组合测试 | 双读面同工作流独立性互不串账钉住（spec 1104） | [spec 1104](docs/spec/1104-todohttp-combo.md) |
| 沙箱治理 | Deno 沙箱探测读面 | 探测缓存命中/重探/成败双守恒显形（spec 1066） | [spec 1066](docs/spec/1066-denoprobe-stats.md) |
| 内容防御 | 内容安全词表双缝判定读面 | BLOCK/MASK 动作与跳过分桶直读显形，四桶守恒（spec 1067） | [spec 1067](docs/spec/1067-moderation-stats.md) |
| 配额治理 | 工具配额消耗读面 | 消耗/拒绝/未管辖三桶守恒显形（spec 1068） | [spec 1068](docs/spec/1068-toolquota-stats.md) |
| 授权治理 | 危险工具守卫判定读面 | HITL 五结局桶守恒显形，升级量可对账（spec 1069） | [spec 1069](docs/spec/1069-dangerous-tool-stats.md) |
| 记忆治理 | 完成轮检测器读面 | 检出率分母/分子显形，空检出即压缩失能信号（spec 1065） | [spec 1065](docs/spec/1065-completedturn-stats.md) |
| 记忆治理 | evidence_lookup 证据回查读面 | 回查命中率与切片率双守恒显形（spec 1073） | [spec 1073](docs/spec/1073-evidlookup-stats.md) |
| 记忆治理 | compact_now×归档 生命周期组合测试 | 归档后手动压缩行为双读面守恒钉住（spec 1119） | [spec 1119](docs/spec/1119-archivecompact-combo.md) |
| 记忆治理 | EpisodeLedger 双实例组合测试 | 跨实例计数累计与双守恒钉住（spec 1105） | [spec 1105](docs/spec/1105-dualinst-episodic.md) |
| 预算治理 | 预算钳位读面 | 负预算钳 0 发生频次显形（spec 1133） | [spec 1133](docs/spec/1133-budgetclamp-stats.md) |
| 预算治理 | 预算 needed 判定分布并入 | 压缩触发压力信号 neededTrue/False 显形（spec 1134） | [spec 1134](docs/spec/1134-budget-needed-ext.md) |
| 记忆治理 | evidence×episodic 独立性组合测试 | 回查与情景记忆互不串账钉住（spec 1111） | [spec 1111](docs/spec/1111-evidepi-combo.md) |
| 记忆治理 | memory 域双工具组合测试 | compact_now 与情景记忆读面互不串账钉住（spec 1087） | [spec 1087](docs/spec/1087-memorytools-readout.md) |
| 记忆治理 | memory 三读面大组合测试 | compact/episodic/fact 三读面交叉互不串账收口（spec 1107） | [spec 1107](docs/spec/1107-memtriple-readout.md) |
| 压缩治理 | 压缩检查点操作读面 | 安全网保存/回滚触发分布显形（spec 1135） | [spec 1135](docs/spec/1135-checkpoint-stats.md) |
| 记忆治理 | compact×evidence 交叉组合测试 | 压缩前后回查计数一致钉住（spec 1096） | [spec 1096](docs/spec/1096-compact-evid-combo.md) |
| 记忆治理 | memory 双台账组合测试 | fact/episodic 双台账读面互不串账钉住（spec 1088） | [spec 1088](docs/spec/1088-dualledger-readout.md) |
| 工具计量 | 沙箱版 run_command 执行分布读面 | 沙箱档送达率与五拒绝桶守恒显形（spec 1074） | [spec 1074](docs/spec/1074-sandboxrun-stats.md) |
| 工具计量 | 双守卫（黑名单+SSRF）组合测试 | 双守卫独立性与一致性互不串账钉住（spec 1086） | [spec 1086](docs/spec/1086-dualguard-readout.md) |
| 工具计量 | tools 五读面全矩阵组合测试 | 五读面交叉互不串账收口（spec 1108） | [spec 1108](docs/spec/1108-toolsmatrix-readout.md) |
| 租户隔离 | 租户沙箱×读写链路组合测试 | 租户面读写对称与越界拒绝计数钉住（spec 1099） | [spec 1099](docs/spec/1099-tenant-rw-chain.md) |
| 工具计量 | 读写对称守恒组合测试 | bytesWritten/bytesRead 字节口径恒等钉住（spec 1081） | [spec 1081](docs/spec/1081-rw-symmetry.md) |
| 工具计量 | fs 全链路四读面组合测试 | 沙箱/写/读/黑名单链路守恒与对称恒等（spec 1084） | [spec 1084](docs/spec/1084-fs-chain-readout.md) |
| 工具计量 | 双档 run_command 对账组合测试 | timeout 语义分叉钉住防修齐回归（spec 1082） | [spec 1082](docs/spec/1082-dualmode-contrast.md) |
| 生命周期治理 | 会话归档操作读面 | 归档成功/空跳/下限拒四桶显形（spec 1075） | [spec 1075](docs/spec/1075-archiver-stats.md) |
| 生命周期治理 | 归档×evidence 回查联动组合测试 | 归档后回查行为双读面守恒钉住（spec 1109） | [spec 1109](docs/spec/1109-archivevid-combo.md) |
| 记忆治理 | compact_now×归档 生命周期组合测试 | 归档后 compact_now skipped 桶行为双读面守恒钉住（spec 1119） | [spec 1119](docs/spec/1119-archivecompact-combo.md) |
| 生命周期治理 | 归档清理任务读面 | 清理轮次/累计产出/锁跳过三面显形（spec 1078） | [spec 1078](docs/spec/1078-purgejob-stats.md) |
| 提示词治理 | 提示词注册表解析分布读面 | InMemoryPromptRegistry 嵌套 PromptResolutionStats（attempts/hits/misses 守恒，公共解析核心不重复计）+ resolutionStats()——解析显形谱系（spec 1039） | [spec 1039](docs/spec/1039-prompt-resolution-stats.md) |
| 会话治理 | ExportManifest 子集校验 | verifySubset（增量搬运只核对提供的子集，规范化口径配对；空 contents fail-fast）——rsync --partial 思想（spec 941） | [spec 941](docs/spec/941-manifest-subset.md) |
| 评估闭环 | pass@k×防抖门组合补验 | 双口径并存语义固化（频率门 fail 与概率达标并存不矛盾）+ enforceStable×history 一致性——评估域三口径组合收口（spec 953） | [spec 953](docs/spec/953-passk-gate-combo.md) |
| 会话治理 | 摘要版本链缺口审计 | SummaryVersionAudit.gaps（version 升序扫描定位缺失号，重复/非正 fail-fast）——Kafka log gap 对账思想（spec 952） | [spec 952](docs/spec/952-summary-version-audit.md) |
| memory | 事实衰减预报读面 | FactDecayPolicy.turnsUntilFloor（逆函数解析，floor=0 永不衰出）——predict_linear 同思路（spec 926） | [spec 926](docs/spec/926-decay-forecast.md) |
| 评估闭环 | k 次防抖门 | EvalGate.enforceStable（k 次全过才过 + 早停 + 历史容量校验）——flaky 误报防护从严门（spec 943） | [spec 943](docs/spec/943-stable-gate.md) |
| 评估闭环 | 评估剪枝进程级兜底装配 | EvalPrunePolicyHolder（进程级兜底）+ autoconfig buzhou.eval.prune.* 装配 bean——RetryBudgetHolder 先例，901 装配收口（spec 958） | [spec 958](docs/spec/958-prune-holder.md) |
| 观测治理 | 工具调用结局分布读面 | ToolCallOutcomeStats.stats（四桶+other 收容桶，守恒不破枚举扩展）——spec 50 日志根因分诊聚合面（spec 944） | [spec 944](docs/spec/944-outcome-stats.md) |
| 持久化 | LeaderElector 契约校验套件 | 五项选主语义检查静态 verify（空位新纪元/重入幂等/跟随态/resign 重取/inspect 一致性）+ 内存接入——契约系列第六站，与 R40 读数面分轴（spec 954） | [spec 954](docs/spec/954-leader-contract.md) |
| 模型韧性 | outbox 重试次数分布读面 | WebhookOutbox.retryDistribution（attempts 分桶 TreeMap 升序 + appendRetry 包级退避落盘）——重试积压结构可见（spec 948） | [spec 948](docs/spec/948-retry-distribution.md) |
| 模型韧性 | ElasticBudgetPool 并发守恒压测 | 8 线程×500 借还 Σheld+surplus==capacity 守恒 + base 保底不吃 borrow——池级单锁语义并发正确性实证（spec 947） | [spec 947](docs/spec/947-budget-pool-stress.md) |
| 持久化 | SessionIndexStore 契约校验套件 | 五项语义检查静态 verify（往返/覆盖幂等/delete 幂等/DELETED 排除/purge 计数 limit 尊重 ACTIVE 保护）+ 内存接入——契约系列第五站（spec 945） | [spec 945](docs/spec/945-index-contract.md) |
| 持久化 | ObservabilityStore 契约校验套件 | 八项语义检查静态 verify（保序/快照写读/空读/隔离/deleteSession 幂等/eventsOfSpan 过滤）+ 内存接入——契约系列收口最后核心 SPI（spec 936） | [spec 936](docs/spec/936-obs-contract.md) |
| 持久化 | 租约契约接入 H2/JDBC | release DELETE 行致 fence 重置缺陷→软过期保 token 单调；九项契约全过（spec 929） | [spec 929](docs/spec/929-h2-lease-contract.md) |
| 持久化 | 租约契约接入 Redis | ACQUIRE_SCRIPT 缺幂等重入→同 owner 续期分支补齐；九项契约全过（spec 930） | [spec 930](docs/spec/930-redis-lease-contract.md) |
| 会话治理 | 剪枝边界深验 | minItems==total 不残缺/阈值极小首 fail 即剪/memo 共存不绕裁决——901 边界组合收口（spec 931） | [spec 931](docs/spec/931-prune-edge-deep.md) |
| tools 文件域 | write_file noclobber 防误覆盖 | WriteFileTool opt-in noclobber（写盘前存在守门零副作用）——csh set -C / cp -n 防误覆盖语义（spec 951） | [spec 951](docs/spec/951-noclobber.md) |
| 工程门禁 | 周期对账轮（spec 932 缺位补位） | 四账核对：spec/票/impl/快照；票号双占用三组改号、impl 680 补写、553-652 归属核清（spec 932） | [spec 932](docs/spec/932-periodic-audit.md) |
| 评估闭环 | 剪枝 run 有效通过率口径 | EvalRunResult.prunedCount() + effectivePassRate()（分母排除 pruned）——双口径显式并存，总量防刷分（spec 933） | [spec 933](docs/spec/933-effective-passrate.md) |
| 评估闭环 | GateResult 有效通过率透出 | GateResult 加 effectivePassRate 组件（11 参新构造 + 10 参兼容 NaN 委托）——剪枝 run 门结果双口径同屏（spec 934） | [spec 934](docs/spec/934-gate-effective-passrate.md) |
| 会话治理 | ExportManifest 规范化摘要 | addCanonical/verifyCanonical 配对（canonicalJson 单点提级）——911 JCS 向 manifest 扩散，键序漂移不误报（spec 935） | [spec 935](docs/spec/935-manifest-canonical.md) |
| 持久化 | spill 回读命中率读面 | SpillOnloadStats（attempts/loaded/failed 守恒，回读失败=侵蚀信号）OnloadHook 回灌点计数——PostgreSQL buffer hit-ratio 借鉴（spec 1008） | [spec 1008](docs/spec/1008-spill-onload-stats.md) |
| 观测治理 | 轮次时延分位数读面 | TurnLatencyPercentiles（R-7 插值 p50/p95 对既有 64 样本窗，percentiles() 读面）——补 spec 191 用户故事的 p95，numpy percentile 同口径（spec 1015） | [spec 1015](docs/spec/1010-turn-latency-percentiles.md) |
| 持久化 | spill 容量水位读面 | SpillUsage（totalBytes/entryCount）DiskSpillStore.usage() 与配额守卫同口径 walk——Redis INFO memory / pg_database_size 借鉴（spec 1011） | [spec 1011](docs/spec/1011-spill-usage.md) |
| 安全 | 加密封存操作生命周期计数读面 | EncryptedSessionExport sealed/opened/openRejected 三计数（open 三拒绝路径全覆盖）+ 嵌套 SealStats + stats()——age/OpenSSL ops 实践，开失败率=密钥失配第一信号（spec 1012） | [spec 1012](docs/spec/1012-seal-lifecycle-stats.md) |
| 观测治理 | Hook Replace 载荷应用/丢弃计数读面 | applyReplace boolean 化 + replaceApplied/replaceDropped 实例计数——类型不匹配幽灵载荷静默蒸发的显形，分发行为逐位不变（spec 1013） | [spec 1013](docs/spec/1013-hook-replace-stats.md) |
| 安全 | Spotlighting 应用与损坏计数读面 | SpotlightingStats（wrapped/unwrapped/malformed 守恒）+ stats()/resetForTest()——含头但结构不完整包裹原样放行的篡改显形，防御覆盖率可见（spec 1014） | [spec 1014](docs/spec/1014-spotlighting-stats.md) |
| 观测治理 | 超时覆盖命中读面 | ToolTimeoutOverrideStats（lookups/hits/misses 守恒 + hitsByPattern 播种全部模式，0 = 幽灵覆盖配置显形）——feature-flag 评估计数思想（spec 1015） | [spec 1015](docs/spec/1015-timeout-override-stats.md) |
| 技能治理 | 技能解析未命中计数读面 | SkillResolutionStats（loads/resolved/notFound 守恒，load-only 口径）——幻觉技能名探测，Berkeley function-calling leaderboard 借鉴（spec 1016） | [spec 1016](docs/spec/1016-skill-resolution-stats.md) |
| 安全 | 沙箱执行结果分桶读面 | LimitedCommandSandbox 嵌套 ExecStats（executions/timeouts/outputTruncations 两轴正交）+ stats()——Firejail/bubblewrap run stats 借鉴（spec 1017） | [spec 1017](docs/spec/1017-sandbox-exec-stats.md) |
| 安全 | taint 信息流控制生命周期计数读面 | TaintMarkStats + GateStats 四分桶（checked == trusted + approved + blocked 守恒）+ 双 stats()——FIDES 判定分布显形（spec 1018） | [spec 1018](docs/spec/1018-taint-lifecycle-stats.md) |
| 安全 | 金丝雀生命周期计数读面 | CanaryGuardHook 嵌套 CanaryStats（planted 播撒幂等不重复计 / leaked 泄漏 / variantBlocked 变体自硬化）+ stats()——Thinkst Canary 触发即铁证思想（spec 1019） | [spec 1019](docs/spec/1019-canary-lifecycle-stats.md) |
| 安全 | 事实采集隔离硬化与计数读面 | FactCollectorHook 逐定义 judge/save 隔离（单定义异常不再炸 afterTool 链）+ FactCollectionStats（saved/failures）+ stats()——监听器隔离惯例推广（spec 1020） | [spec 1020](docs/spec/1020-fact-collector-isolation.md) |
| 安全 | HITL 审批操作分布读面 | GuardAuthApi 嵌套 AuthOperationStats（approved/rejected/revoked 三计数）+ stats()——审批聚合视图，事件流之外的直读水位（spec 1021） | [spec 1021](docs/spec/1021-auth-operation-stats.md) |
| 安全 | 加密消息存储操作计数读面 | EncryptingMessageStore 嵌套 CryptoStoreStats（encrypted/decrypted/passthrough 双向透传分计）+ stats()——信封加密 ops 可视性（spec 1022） | [spec 1022](docs/spec/1022-crypto-store-stats.md) |
| 安全 | 密钥扫描计数读面 | SecretScanner 嵌套 SecretScanStats（scanCalls/findings/redactions，findings 水位=泄漏趋势）+ stats()——Gitleaks findings 借鉴（spec 1023） | [spec 1023](docs/spec/1023-secret-scan-stats.md) |
| 记忆治理 | facts 段导入导出行数读面 | FactsExporter 嵌套 FactsFlowStats（factsExported/factsImported/importFailures 照抛）+ stats()——rsync --stats 迁移完整性思想（spec 1024） | [spec 1024](docs/spec/1024-facts-flow-stats.md) |
| 安全 | 内嵌策略引擎判定分布读面 | EmbeddedPolicyEngine 嵌套 PolicyDecisionStats 四桶（allow/deny/escalate/escalateApproved 守恒）+ stats()——OPA 判定分布谱系，FIDES approver 通道压力显形（spec 1025） | [spec 1025](docs/spec/1025-policy-engine-stats.md) |
| 安全 | 事实注入覆盖读面 | FactAttachmentRenderer 嵌套 FactInjectStats（renders/factsInjected/factsOmitted，max-inject-chars 配置水位）+ stats()——两参 render 收敛委托输出恒等（spec 1026） | [spec 1026](docs/spec/1026-fact-inject-coverage.md) |
| 记忆治理 | 手动压缩操作分布读面 | ManualCompactor 嵌套 CompactOpStats 五计数（attempts/completed/skipped/failed/foldedMessages 守恒）+ opStats()——K8s 事件聚合思想（spec 1027） | [spec 1027](docs/spec/1027-compact-op-stats.md) |
| 观测治理 | 模型窗口解析分布读面 | TableContextWindowResolver 嵌套 WindowResolutionStats（override/内置/回退三路守恒 + resolvedWindows 快照）——LLM 模型目录覆盖思想，幽灵覆盖显形（spec 1028） | [spec 1028](docs/spec/1028-window-resolution-stats.md) |
| 成本预算 | 模型预算闸判定分布读面 | ModelBudgetGate 嵌套 BudgetGateStats（checks/allowed/blocked 守恒）+ stats()——SRE 预算耗尽告警思想，连续拦截水位直读（spec 1029） | [spec 1029](docs/spec/1029-budget-gate-stats.md) |
| 安全 | 审计收集器采集与持久化失败计数读面 | AuditTrailCollector 嵌套 AuditIngestStats（collected/persistFailures/openSessions）+ stats()——Splunk HEC ingestion stats 借鉴（spec 1031） | [spec 1031](docs/spec/1031-audit-ingest-stats.md) |
| 观测治理 | 打转检测触发聚合读面 | RepetitionDetectorHook 嵌套 RepetitionStats（fires/blocks/maxRunSeen 峰值）+ stats()——LLM 打转频率调参水位（spec 1032） | [spec 1032](docs/spec/1032-repetition-stats.md) |
| 会话治理 | 会话级联清理聚合计数读面 | SessionCleaner 嵌套 CleanupStats（deleteCalls/cleanedTargets/failedTargets + failuresByTarget 分桶）+ cleanupStats()——PostgreSQL autovacuum stats 思想（spec 1030） | [spec 1030](docs/spec/1030-cleanup-stats.md) |
| 观测与运维 | 归档 TTL 清扫（补登） | 归档记录按 TTL 过期清扫，容量治理闭环（spec 103） | [spec 103](docs/spec/103-archive-ttl-purge.md) |
| 观测治理 | 错误签名 JSONL 导出（补登） | 错误签名集 JSONL 批量导出，离线分析通道（spec 112） | [spec 112](docs/spec/112-signatures-jsonl-export.md) |
| 观测与运维 | 归档明细查询（补登） | 归档批次内逐会话明细查询，凭证可追溯（spec 120） | [spec 120](docs/spec/120-archive-detail-query.md) |
| 观测治理 | 错误签名重置（补登） | 签名计数进程态重置，测试隔离与运维清零（spec 121） | [spec 121](docs/spec/121-signatures-reset.md) |
| 工程门禁 | J 系周期预检（R10） | 隔离 worktree 全仓 verify 16 模块绿 + 双门复跑 + 三处主仓红收口（guard 保序/910–915 README 行/SessionExportDiff 快照行）（spec 1009） | [spec 1009](docs/spec/1009-periodic-audit-r10.md) |
| 观测治理 | 工具策略匹配决策读面 | ToolPolicyMatcher 判定单点分类 EXACT/GLOB/NONE + 有界最近决策环 + stats() 快照，Σ守恒 == match 调用数——OPA decision log 借鉴（spec 1000） | [spec 1000](docs/spec/1000-policy-match-decision.md) |
| 观测治理 | 工具慢调用榜读面 | ToolSlowLog（严格大于阈值入有界 FIFO 环 + entries() 新→旧现场）与 spec 108 timer 同点接线——Redis SLOWLOG 借鉴（spec 1001） | [spec 1001](docs/spec/1001-tool-slow-log.md) |
| 观测治理 | Hook 链解析顺序快照读面 | ChainComposition（派发序显形 + 幽灵禁用集——拼错 disabled 名静默蒸发的信号）composition() 只读快照——Kong plugin priority 借鉴（spec 1002） | [spec 1002](docs/spec/1002-hook-chain-composition.md) |
| 观测治理 | 策略层级归属读面 | PolicyLayerAttribution（DEFAULTS/YML/BINDING/ABSENT 归属 + getAttributed 同序同判，get() 薄封装零行为变化）——spring config insights 层归因借鉴（spec 1003） | [spec 1003](docs/spec/1003-policy-layer-attribution.md) |
| 会话治理 | 维护窗历史读面 | MaintenanceGate 闭窗历史环（HistoryEntry：何时/为何/多久 + 窗内拒绝按窗分账）history() 新→旧快照——K8s cordon 事件史借鉴（spec 1004） | [spec 1004](docs/spec/1004-maintenance-history.md) |
| 观测治理 | 工具在飞并发水位读面 | ToolInFlight（每工具 current/peak/total + 全局双水位 + AutoCloseable 租约恰一次）——Go NumGoroutine/Hystrix 借鉴（spec 1005） | [spec 1005](docs/spec/1005-tool-in-flight.md) |
| 观测治理 | 会话面包屑环形读面 | EventBreadcrumb 时间线尾部环（deliverEvent 双模式共同漏斗，只记 type 不记 payload）+ breadcrumbs() 新→旧快照——Sentry breadcrumbs 借鉴（spec 1006） | [spec 1006](docs/spec/1006-session-breadcrumbs.md) |
| 模型韧性 | 凭证租约生命周期计数读面 | SecretLeaseStats 五计数快照（补 renew 轴：续租成功/被拒——拒绝率高=TTL 过短信号）——Vault lease lifecycle 借鉴（spec 1007） | [spec 1007](docs/spec/1007-lease-lifecycle-stats.md) |
| 工程门禁 | 全模块测试补全覆盖（K 会话 R1） | JaCoCo 缺口驱动零覆盖清零：6 模块 20 靶点直测（core 16 含 SessionStateStore default 体死路径复活 + guard/spill/resilience/mcp 各 1），豁免入档不硬凑；测试显形三缺陷单列修复（spec 1200） | [spec 1200](docs/spec/1200-test-coverage-completion.md) |
| 工程门禁 | 低覆盖类批次 1（K 会话 R2） | 低覆盖档（<50% 且 miss≥10）清点：guard PolicyGateHook 三态裁决/taint 映射/事件/指标四合同面 + memory RecallSearchTool 四模输出/降级/截断十断言面（spec 1201） | [spec 1201](docs/spec/1201-low-coverage-batch1.md) |
| 工程门禁 | skills RedisSkillStore 契约接入（K 会话 R3） | SkillStore 契约范式补链：Redis 实现接同一契约基类（真实 redis:7-alpine 容器，无 Docker 跳过）+ 重启存活加验；修正 R1 审计漏扫 skills（spec 1202） | [spec 1202](docs/spec/1202-redis-skill-store-contract.md) |
| 工程门禁 | core 零覆盖尾巴清扫（K 会话 R4） | 判据收紧 miss≥5→miss≥1 后复扫：AttachmentRenderer default 截断合同四断言 + CommandOutcome success 谓词矩阵（超时优先于退出码）；core 证据改走隔离 worktree（spec 1203） | [spec 1203](docs/spec/1203-core-zero-tail-sweep.md) |
| 工程门禁 | SnapshotMessage 补测与跨模块复核（K 会话 R5） | 收紧判据残留归口：compact 构造 null 防御 + Map.copyOf 拷贝语义合同；六小模块 miss≥1 复扫清单化（spec 1204） | [spec 1204](docs/spec/1204-snapshot-message-tightened-sweep.md) |
| 工程门禁 | K 会话周期对账轮 R6 | 全仓 verify（隔离 worktree CI 等价门）+ K 线工件链五项对账（spec/README/票/impl/map）——SRE Production Readiness Review 思想，R7 起对账/雾区交替（spec 1205） | [spec 1205](docs/spec/1205-k-audit-r6.md) |
| 工程门禁 | R7 雾区裁决：T1819 治本 + BRANCH/report-aggregate | Webhook 测试 forwarder 收口（谁启动谁收尾）；BRANCH 13 模块实测分布入台账（硬门暂缓有据）；report-aggregate 不引入（模块工程档不动）——ADR 式裁决（spec 1206） | [spec 1206](docs/spec/1206-fog-adjudication-r7.md) |
| 工程门禁 | R8 分支缺口批次 1：observe-otel | 分支精确定向补测：OtelBridgeSink 61%→87%（重复开启/驱逐护栏/spanName 回退/sessionTrace 上界/类型适配器/故障隔离）+ OtelProperties 100%；显形 T1824 sessionTrace 驱逐 iterator.remove() 缺 next() 致超限后新会话 span 静默丢弃（spec 1207） | [spec 1207](docs/spec/1207-branch-uplift-otel.md) |
| 工程门禁 | R9 分支缺口批次 2：observability 两小类 | ObservabilitySessionState 46%→88%（会话 span 生命周期/usage 聚合/CANCELLED 终态）+ ObservableToolCallback 29%→86%（parent 三级解析/异常 error+close+rethrow）；批次 3 = Advisor/DualWriter（spec 1208） | [spec 1208](docs/spec/1208-branch-uplift-observability.md) |
| 工程门禁 | R10：MicrometerDualWriter 补测 | 双写适配器指标口径合同 11 用例（NOOP 哨兵/MODEL_CALL·TOOL_CALL 双路径/unknown 回退/bounded 32·64·16 截断/TTFT·TPOT 三态不记）；分支 67%→93%（spec 1209） | [spec 1209](docs/spec/1209-micrometer-dual-writer.md) |
| 工程门禁 | R11 分支批次 4：边缘分支清扫 | ThinkingChainExtractor 76%→90%（extraKeys 过滤/maxChars 钳制/omitted 字符串形态）+ DefaultSpanHandle 63%→88%（attributes 批量导入/双 close 幂等/显式终态优先）；Advisor 流式 harness 单列（spec 1210） | [spec 1210](docs/spec/1210-branch-uplift-batch4.md) |
| 工程门禁 | K 会话周期对账轮 R12 | 全仓 verify（隔离 worktree）+ 工件链五项对账（R8–R11 增量回归）；R13 议程 = Advisor 流式 harness（spec 1211） | [spec 1211](docs/spec/1211-k-audit-r12.md) |
| 工程门禁 | R14：ToolGraphAnalyzer 边缘分支 | cycles null fail-fast/零计数边不入邻接/同 count 边字典序 tie-break/kind=null 忽略/startedAt=null 计 0/负时长夹 0；分支 95%（spec 1213） | [spec 1213](docs/spec/1213-graph-analyzer-edge.md) |
| 工程门禁 | R15：BaseSpanRecorder sink 分发补测 | enqueue 时刻旁路分发合同 5 用例（span/event 到达/逐 sink 异常隔离落库不受污染/PendingSnapshot 跳过/顺序保持）；分支 85%（spec 1214） | [spec 1214](docs/spec/1214-sink-dispatch.md) |
| 工程门禁 | R16：ObservabilityAdvisor 非流式路径补测 | adviseCall 全链 13 用例（recordModelCallOutcome 22 missed 集中区：usage 三态/thinking 三态 PROVIDER_NOT_RETURNED 启发式与显式 provider/抑制分支/异常 ERROR）；分支 69%→75%（spec 1215） | [spec 1215](docs/spec/1215-advisor-call.md) |
| 工程门禁 | R17：Advisor 残余分支清扫 | 流式+非流式 harness 复用追加 5 用例（metadata=null·result=null 防御/blank finishReason 不记/空串思维链无事件/usage 0 归一口径）；ObservabilityAdvisor 75%→77%（spec 1216） | [spec 1216](docs/spec/1216-residual-sweep.md) |
| 工程门禁 | K 会话周期对账轮 R18 | 全仓 verify（隔离 worktree 强制重置基座）+ 工件链五项对账（R13–R17 五轮 52 用例增量回归）；沿 R6/R12 口径（spec 1217） | [spec 1217](docs/spec/1217-k-audit-r18.md) |
| 工程门禁 | R19：流式语义定向补测 | ToolResponseMessage 占位符快照提取（evidence/spill 首次直接断言）/TTFT CAS 幂等恰一次/omitted-only 流块/空 Flux 防御；spec 1218 | [spec 1218](docs/spec/1218-stream-detail.md) |
| 工程门禁 | R24：流式 usage 组合与 null 防御 | completion-only 不记 prompt/全 null Usage 经 builder 归一 0/0 捕获（打点实证）/流中 null chatResponse 元素防御跳过；spec 1223 | [spec 1223](docs/spec/1223-stream-null-usages.md) |
| 工程门禁 | R28：adviseCall 路径 usage null 保留两侧 | NullableUsage 对称补齐（R24 流式先例到 call 路径）：completion-only prompt 跳过/双 null 两属性均跳过；spec 1227 | [spec 1227](docs/spec/1227-call-usage-null-retain.md) |
| 工程门禁 | R30：Jcs 规范化全分支 | RFC 8785 自实现子集 10 用例（标量规范形/键字典序/转义全覆盖/整数约束/非法 JSON）；分支 97%（spec 1229） | [spec 1229](docs/spec/1229-jcs-canonicalization.md) |
| 工程门禁 | R34：金丝雀候选限流两态 | 放行态（RPM=10 额度内双轮金丝雀照常直达备模型，主模型零调用）与耗尽态（RPM=1 时次轮 acquireOrThrow 抛 ModelRateLimitExceededException——全局限流窗语义实证）；spec 1233 | [spec 1233](docs/spec/1233-canary-quota.md) |
| 工程门禁 | R32：金丝雀路径 e2e 直测 | canary 路由成功/终态失败链序回退主模型/canary-selected 事件 payload 钉 model+sessionId；金丝雀发布语义三件套（spec 1231） | [spec 1231](docs/spec/1231-canary-path-e2e.md) |
| 工程门禁 | R29：GuardAuditConfig 解析全分支 | fromGuardMap 子 Map 解析 8 用例（null/非 Map 回退/trim+lower/容量与 min-verify 容错/blank key-dir/非法 KeyFile 过滤）；分支 17%→92%（spec 1228） | [spec 1228](docs/spec/1228-audit-config-parse.md) |
| 工程门禁 | R36：GuardModule$Builder 开关组合矩阵 | feature toggle 装配合同 10 用例（全关最小面/逐一开→hook 注册/enabled=false→移除/dangerousTool entry/fromYml 等价/allOn⊇allOff 超集）；spec 1231 | [spec 1231](docs/spec/1231-builder-branch-matrix.md) |
| 工程门禁 | R31：ResilienceAdvisor 影子镜像 e2e | 主路成功后采样对照备模型 4 用例（镜像发生/失败全吞/候选空守卫/零采样守卫）；Istio mirror 思想容量预案信心面；spec 1230 | [spec 1230](docs/spec/1230-shadow-mirror-e2e.md) |
| 工程门禁 | R27：T1867 TRM getText 语义核验 | javap 反编译定论（textContent 恒空为设计现状）+ 生产捕获走 getResponses responseData 源码比对；错误合同断言删除；spec 1226 | [spec 1226](docs/spec/1226-trm-gettext-verification.md) |
| 工程门禁 | R26：Advisor 细粒度残余清扫 | 流式 content=null chunk 防御（无 NPE/后续照常）+ TRM 占位符提取正反例（快照 evidence/spill 首次负例断言）；可达性分级豁免入档；spec 1225 | [spec 1225](docs/spec/1225-advisor-fine.md) |
| 工程门禁 | K 会话周期对账轮 R23 | 全仓 verify（隔离 worktree 固定本地 HEAD）+ 工件链五项对账（R19–R22 四轮 25 用例增量回归）；沿 R6/R12/R18 口径（spec 1222） | [spec 1222](docs/spec/1222-k-audit-r23.md) |
| 工程门禁 | K 会话周期对账轮 R25 | 全仓 verify（隔离 worktree）+ 工件链五项对账；漂移防护提前对账（距 R18 六轮、O 系 R32+ 高速落库）（spec 1224） | [spec 1224](docs/spec/1224-k-audit-r25.md) |
| 工程门禁 | K 会话周期对账轮 R33 | 超期执行（距 R18 十四轮）：全仓 verify（隔离 worktree 固定本地 HEAD）+ 工件链五项对账 + 跳号 T1870 入档；spec 1232 | [spec 1232](docs/spec/1232-k-audit-r33.md) |
| 工程门禁 | R21：BuzhouMemoryAdvisor 分支直测 | 写路径 fence（先于落库/抛错阻断零写入/恢复照常）+ Identity 去重 + USER/ToolResponse 写入与 ASSISTANT 过滤 + 无 fence 行为不变；分支 77%→95%（spec 1220） | [spec 1220](docs/spec/1220-memory-advisor-branches.md) |
| 工程门禁 | R22：HookAdvisor 切面分发直测 | beforeModel Block 短路/afterModel replaceResponse 回填/onModelError 决策树（Block 文本兜底/Replace 经链契约 ctx 回填/Continue 放行）+ 流式同构对称；分支 70%→100%（spec 1221） | [spec 1221](docs/spec/1221-hook-advisor-dispatch.md) |
| 工程门禁 | R20：快照预算分解 + extraKeys 贯通 + 文本 null 防御 | budgetBreakdown 四键角色分桶首次断言（长度口径确定性）/custom_thinking 经 advisor 贯通 THINKING 事件/content=null 无 FINAL_REPLY 无 NPE；spec 1219 | [spec 1219](docs/spec/1219-snapshot-budget-extrakeys.md) |
| 工程门禁 | R11 分支批次 4：边缘分支清扫 | ThinkingChainExtractor 76%→90%（extraKeys 过滤/maxChars 钳制/omitted 字符串形态）+ DefaultSpanHandle 63%→88%（attributes 批量导入/双 close 幂等/显式终态优先）；Advisor 流式 harness 单列（spec 1210） | [spec 1210](docs/spec/1210-branch-uplift-batch4.md) |
| 工程门禁 | K 会话周期对账轮 R12 | 全仓 verify（隔离 worktree）+ 工件链五项对账（R8–R11 增量回归）；R13 议程 = Advisor 流式 harness（spec 1211） | [spec 1211](docs/spec/1211-k-audit-r12.md) |

## 生产级纵深 VIII（G 会话 700 系增量）

G 会话（effort #700+ 号段，借鉴 GitHub >10K star 项目）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|

## 生产级纵深 IX（H 会话 800 系增量）

H 会话（effort #800+ 号段，借鉴 GitHub >10K star 项目）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 护栏 | 工具自动封禁 | ToolAutoBanHook——受监视工具滑窗连续失败达阈值自动封禁+beforeTool 拦截报剩余秒+快照读数，失效工具止损从盯梢变自动（fail2ban maxretry/findtime/bantime） | [spec 800](docs/spec/800-tool-auto-ban.md) |
| 持久化 | Redis 大值审计 | RedisValueSizeAudit——采样字节九族归类+WARN/CRIT 定级+Top32+按族聚合+治理提示，哪个命名空间在吃内存结构化（redis-cli --bigkeys 思想，纯函数） | [spec 801](docs/spec/801-redis-value-size-audit.md) |
| 观测治理 | 指标新鲜度审计 | MetricFreshnessTracker——BuzhouMetrics 装饰器记名字级最后写入+audit(now, staleAfter) 陈旧清单，「指标为何没数了」从猜变查（Prometheus staleness；gauge 不追踪口径显式） | [spec 802](docs/spec/802-metric-freshness.md) |
| 记忆治理 | 检索多路改写融合 | MultiQueryRetriever——查询多变体展开+逐路检索+跨变体 RRF 融合，词形脆弱不再漏召（LangChain MultiQueryRetriever；与 605 双信号融合正交） | [spec 803](docs/spec/803-multi-query-retriever.md) |
| 缓存与前缀 | 嵌入 L2 归一化装饰器 | NormalizingEmbeddingModel——输出向量逐条单位范数，cosine 退化为点积、跨供应商尺度一致（sentence-transformers normalize_embeddings；721 同模式） | [spec 804](docs/spec/804-normalizing-embedding.md) |
| 模型路由 | 路由分布倾斜读数 | RouteDistributionReadout——实际调用分布 vs 声明权重偏差降序+gini 集中度+dominant，「50/50 说成 95/5 做」漂移可见（Spark skew；只读不纠偏） | [spec 805](docs/spec/805-route-distribution-skew.md) |
| 成本归因 | 预算用量分位推荐 | BudgetRecommendation——用量样本 P50/P95/P99+⌈P95×(1+headroom)⌉ 推荐档位，预算从拍脑袋变分位推导（k8s VPA；<5 样本不下结论哨兵） | [spec 806](docs/spec/806-budget-recommendation.md) |
| 护栏 | 签名密钥轮换到期审计 | KeyRotationAudit——密钥龄 OVERDUE/DUE_SOON/OK 三档+UNKNOWN_ACTIVE 账本异常面+最坏排序，SigningKeyRing 零侵入（cert-manager 证书到期监控；纯读数不轮换） | [spec 807](docs/spec/807-key-rotation-audit.md) |
| 导出治理 | 导出内容去重统计 | ExportDedupeStats——精确串值重复计数+节省字符+savingsRatio+Top16 preview 截 32 隐私，空块计 items 不计重复（restic dedupe stats；字符口径与 738 一致） | [spec 808](docs/spec/808-export-dedupe-stats.md) |
| 并发治理 | 作业死信台账 | JobDeadLetterLog——DelayedJobQueue 可选失败观察者(2 参构造 null=原行为)停尸明细环 64+按键聚合 64+totalFailed，message 截 200（sidekiq dead set；不重投） | [spec 809](docs/spec/809-job-dead-letter.md) |
| 存储治理 | 存储提交延迟环形读数 | StoreLatencyRing+TimedMessageStore——per-op FIFO 128 样本环+最近秩 P50/P95+操作名封顶 16，MessageStore 装饰器三方法 finally 计时异常照记（etcd backend commit latency；行为零变更） | [spec 810](docs/spec/810-store-latency-ring.md) |
| 模型韧性 | 断路器 crash-loop 检测 | CircuitCrashLoopDetector——滑窗 OPEN≥minOpens 转 looping 闩锁态(窗口滑过不自动解除、唯 recordRecovery 清除)，loopsDetected 边沿计数（k8s CrashLoopBackOff 语义对齐；旁路不改断路器） | [spec 811](docs/spec/811-circuit-crash-loop.md) |
| 观测治理 | 观测管道内存限流器 | PipelineMemoryLimiter——在途权重总量判定 tryAdmit(CAS 无锁拒收不记账)+release 归账防负，拒绝只发信号（OTel memory_limiter；8 线程守恒压测） | [spec 812](docs/spec/812-pipeline-memory-limiter.md) |
| 技能治理 | 技能发布通道解析 | SkillChannelResolver——(名,版本,通道) 注册表纯解析：显式通道→回退 latest→全表最高，点分数值段+prerelease 低于 release（pnpm/yarn dist-tag；只读零 store 侵入） | [spec 813](docs/spec/813-skill-channel-resolver.md) |
| MCP 治理 | MCP 断路器变迁台账 | McpBreakerTransitionJournal——702 同模式扩散 server 聚合熔断：三路径后 stateOf 差分采样入账+环形 64+trips/recovers 聚合 32（采样型差分口径显式） | [spec 814](docs/spec/814-mcp-breaker-journal.md) |
| Spill 治理 | Spill 写放大读数 | SpillWriteAmplifier——逻辑/物理字节累计+放大率+近窗 64 样本均值与最近秩 P95（RocksDB bytes_written/bytes_logical 口径；store 主路径零侵入） | [spec 815](docs/spec/815-spill-write-amplifier.md) |
| 记忆治理 | 记忆分层容量读数 | MemoryHierarchyCapacity——core-summary/archival-facts/recall-window 三层 items/chars+可选 cap 水位三级（MemGPT/Letta 分层借鉴；Snapshot 调用方采集） | [spec 816](docs/spec/816-memory-hierarchy-capacity.md) |
| 观测治理 | SLO 多窗燃烧率判定 | SloMultiWindowBurn——快慢双窗同超阈值且样本足才判 incident，独热带毛刺/渗漏诊断 reason（Google SRE Workbook multiwindow；组合式判定脑 ErrorBudget 零变更） | [spec 817](docs/spec/817-slo-multiwindow-burn.md) |
| 模型韧性 | 限流自适应收紧器 | AdaptiveRateTightener——429 乘性收缩(下限封底)+保持窗后乘性步进恢复纯时间推导（AWS adaptive mode 客户端节流；乘数接线归调用方） | [spec 818](docs/spec/818-adaptive-rate-tightener.md) |
| 预算治理 | Token 估算校准偏差审计 | EstimatorCalibrationAudit——估算 vs 模型真实 usage 成对入账：相对误差均值+偏高偏低占比+近窗 P95（预测校准思想；事后审计不改估算器） | [spec 819](docs/spec/819-estimator-calibration.md) |
| 护栏 | 护栏豁免登记面 | GuardExemptionRegistry——机制×主体显式有时限豁免：惰性过期计数+同键覆盖续期+封顶 64 truncated（ESLint suppressions 带过期；不自动接线 hook 零变化） | [spec 820](docs/spec/820-guard-exemption-registry.md) |
| 工具治理 | 目录 lint 严重度分级 | LintSeverityGrader——DENY/WARN/HINT 三档默认映射+withRule 不可变定制+严重序典序破平+未知规则保守 HINT（rust-clippy 分级；纯分级不阻断） | [spec 821](docs/spec/821-lint-severity-grader.md) |
| MCP 治理 | MCP 能力协商快照 | McpCapabilitySnapshot——连接 seam 三观察点单点快照：排序名册+hint 覆盖/只读/破坏计数+确定性指纹（LSP capabilities；835 diff 的基线输入形状） | [spec 822](docs/spec/822-mcp-capability-snapshot.md) |
| 启动治理 | 启动阶段耗时读数 | StartupPhaseTiming——装配阶段 start/end 句柄计时留痕(未结束 -1 哨兵+end 首末幂等+升序快照+封顶 64)，Clock 注入（Spring Boot ApplicationStartup；零 lifecycle 侵入） | [spec 823](docs/spec/823-startup-phase-timing.md) |
| 会话治理 | 取消原因分布读数 | CancelCauseDistribution——606 五类闭集计数/份额/lastSeen 降序面+dominant 平局声明序（Temporal 取消观测；闭集天然有界） | [spec 824](docs/spec/824-cancel-cause-distribution.md) |
| 会话治理 | 会话迁移对账 | MigrationReconciliation——源/目标导出四维对账：消息计数/轮次范围/首尾 id(仅 keepIds)/状态键缺失明细封顶 8（gh-ost 在线迁移对账；重映射跳过 id 比对） | [spec 825](docs/spec/825-migration-reconciliation.md) |
| 护栏 | 注入检测分级策略 | InjectionParanoiaPolicy——L1-L4 标准阈值表(0.95/0.85/0.70/0.50)+BLOCK/LOG/ALLOW 三态裁决(0.10 观察带)，分数越界截断（ModSecurity paranoia levels；classifier 零变更） | [spec 826](docs/spec/826-injection-paranoia-policy.md) |
| 成本归因 | 定价表覆盖审计 | PricingCoverageAudit——被调用模型 vs 价表键集三层匹配(精确/大小写/剥 provider 前缀)+覆盖率+unknown 典序封顶 32（LiteLLM model_prices；空调用 1.0/空表 0 空真） | [spec 827](docs/spec/827-pricing-coverage-audit.md) |
| 持久化 | 计时连接池 DataSource | TimedDataSource——两种 getConnection nanoTime finally 计时进 StoreLatencyRing(含池等待，与 810 正交两层)（HikariCP 池等待指标；其余方法纯委托） | [spec 828](docs/spec/828-timed-datasource.md) |
| 记忆治理 | 事实合并决策分布 | FactMergeDecisionDistribution——9 段×3 决策(CREATED/KEPT/SUPERSEDED) 闭集记账+supersededRatio+段行声明序（mem0 冲突解决统计扩散；reconcile 零变更） | [spec 829](docs/spec/829-fact-merge-decisions.md) |
| 护栏 | 审计树形健康读数 | AuditTreeHealthReadout——叶数→深度(32−lz 技巧)/nextPow2/补位叶/满树判定，细高树 vs 矮胖健康量化（CT 树语义扩散；纯形状不校验内容） | [spec 830](docs/spec/830-audit-tree-health.md) |
| 背压治理 | 会话准入拒绝分布 | SpawnRejectionDistribution——拒绝原因开集聚合：键封顶 16 truncated+count/lastSeen 降序+dominant 平局稳定（k8s admission 拒绝读数；gate 零变更） | [spec 831](docs/spec/831-spawn-rejection-distribution.md) |
| 技能治理 | 技能加载延迟读数 | SkillLoadLatency——per-skill 环 32 样本+nearest-rank P50/P95+全历史 max，超 1024 技能并入溢出桶+slowest() P95 降序（LangSmith 延迟分析扩散；loads=近窗语义） | [spec 832](docs/spec/832-skill-load-latency.md) |
| 护栏 | 危险工具命中分布 | DangerousToolHitStats——per-tool 命中热力排行：封顶 64+溢出桶/requiredState 最近非空/lastSeen max/top(n) 降序（WAF top-rules 观测；不改拦截行为） | [spec 833](docs/spec/833-dangerous-tool-hit-stats.md) |
| 上下文治理 | 上下文截断统计 | ContextTruncationStats——跨截断机制 chars 聚合：策略键封顶 8 超限并入溢出桶(量净计)+events/chars 双累计+chars 降序（HF truncation_strategy；喂点=机制装配侧） | [spec 834](docs/spec/834-context-truncation-stats.md) |
| 观测治理 | 尾采样决策台账 | TailSamplingDecisionLog——trace 采样决策环形明细 64+决策×原因聚合(键封顶 16+溢出桶带决策维)+keptRatio（OTel tail_sampling；与 eval 采样域正交） | [spec 835](docs/spec/835-tail-sampling-decision-log.md) |
| 模型韧性 | 半开探测成功率读数 | HalfOpenProbeStats——per-model 探测成败累计+连续失败 streak(成功清零)+近窗 20 成功率（Resilience4j probe 语义扩散，与 811 互补；模型封顶 32） | [spec 836](docs/spec/836-halfopen-probe-stats.md) |
| 模型韧性 | 限流键热点读数 | RateLimitKeyHotspot——限流键申请热力排行：键封顶 128+溢出桶/requests+amount 毫账累计/lastSeen+top 降序（Envoy 键域观测；补位轮 effort 连续性恢复） | [spec 837](docs/spec/837-ratelimit-key-hotspot.md) |
| 持久化 | 选举竞争读数 | LeaderElectionStats——选主四态(获选/续期/让位/失位)原子计数+contentionRatio 竞争烈度（Redisson RedLock 竞争统计；归类归调用方零变更） | [spec 838](docs/spec/838-leader-election-stats.md) |
| 泄漏治理 | 泄漏疑似对象聚合器 | LeakSuspectAggregator——实现 LeakListener 按描述稳键(截 64)聚合 count/maxAge/lastSeen，键封顶 32+溢出桶+count 降序排行（换题 S9；检测器零变更） | [spec 839](docs/spec/839-leak-suspect-aggregator.md) |
| MCP 治理 | MCP 建连遥测读数 | McpConnectTelemetry——per-server 建连成败+连续失败 streak+lastDuration+近窗 16 成功率+worstFirst 失败降序（gRPC channelz 思想；与 722/822 三层正交） | [spec 840](docs/spec/840-mcp-connect-telemetry.md) |
| 会话治理 | 会话空闲时长分桶直方 | IdleDurationHistogram——可配升序边界(默认 1m/5m/15m/60m 五桶，恰达归右桶)桶计数+total/longest+人话区间标签快照（S5 扩散；AtomicLongArray） | [spec 841](docs/spec/841-idle-duration-histogram.md) |
| 护栏 | HITL 认证决策分布 | AuthDecisionStats——五态闭集(批准/拒绝/过期/已消费/未知凭证)计数+占比降序快照（Keycloak 决策观测扩散；GuardAuthApi 零变更） | [spec 842](docs/spec/842-auth-decision-stats.md) |
| Spill 治理 | 证据引用失效率读数 | EvidenceRefValidity——被引用 URI vs 存在性谓词失效率对账：失效样本典序封顶 16+空集空真（S3 presigned 时限校验；谓词注入零触碰） | [spec 843](docs/spec/843-evidence-ref-validity.md) |
| 记忆治理 | 摘要降级原因分布 | SummaryDegradeReasons——五态闭集(超限/生成失败/空内容/策略强制/未知)计数+占比降序快照（Envoy degraded 扩散；SummaryDegrader 零变更） | [spec 844](docs/spec/844-summary-degrade-reasons.md) |
| 提示词治理 | Prompt 回滚使用读数 | RollbackUsageStats——prompt 回滚聚合：名封顶 64+溢出桶/rollbacks/最近版本对/lastSeen，次数降序典序破平（S6 Langfuse rollback 观测面） | [spec 845](docs/spec/845-rollback-usage-stats.md) |
| 观测治理 | 死信重投成功率读数 | DeadLetterRedeliveryStats——重投 attempts/successes/successRate+连续失败 streak(成功清零) 原子记账（sidekiq retry set 扩散；重投语义归调用方） | [spec 846](docs/spec/846-deadletter-redelivery-stats.md) |
| 评估治理 | 数据集近重复读数 | DatasetNearDuplicateStats——trigram Jaccard 两两对账+并查集成簇：duplicatePairs/largestCluster/uniqueRatio，条目封顶 200 截断（Cleanlab 数据质量；714 同源扩散） | [spec 847](docs/spec/847-dataset-near-duplicate.md) |
| 配置治理 | 配置默认偏离审计 | ConfigDeviationAudit——当前值 vs 出厂默认偏离对账(无基线不裁决)+偏离清单典序封顶+偏离率（Spring Boot configuration metadata 扩散；与 ConfigDiff 辨义） | [spec 848](docs/spec/848-config-deviation-audit.md) |
| 收口 | H 会话 50 轮收口终验 | 全反应堆串行回归绿+快照门/覆盖门全过+台账 50/50 归档——effort 800–849 连续无缺位（R39 补位轮制度化） | [spec 849](docs/spec/849-h-session-closing.md) |

### I 会话 900 系产出吸收登记

I 会话（effort #900+）部分 spec 未及在 README 登记（H 会话收口合并时发现）——按覆盖门纪律补登如下，详表归 I 会话台账（[progress 台账](.wayfinder/maps/progress-effort-800.md) 同目录）：

| 来源 | 吸收登记 | 详设 |
|------|----------|------|

## 生产级纵深 X（L 会话 1400 系增量）

L 会话（effort #1400+ 号段，借鉴 GitHub >10K star 项目）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 会话治理 | 跨会话轮次并发水位 | TurnConcurrencyTracker——同实例注册全会话聚合 started/okFinished/failed 三总量+active/peakActive 水位，守恒式 started=ok+failed+active；同轮实证修复 guard-block 轮观察者终结回调缺失（TURN span 泄漏）——HikariCP 池读面思想（spec 1400） | [spec 1400](docs/spec/1400-turn-concurrency-tracker.md) |

| 工具计量 | 工具结果字节直方 | ToolResultSizeHistogram——afterTool 单点记账五幂次边界桶(256B/1K/4K/16K/64K)+溢出桶+totalBytes 精确累计，守恒 successes=Σbuckets、executed=successes+failed，失败不入字节分布——Prometheus histogram 思想，opt-in 只读 Hook（spec 1401） | [spec 1401](docs/spec/1401-tool-result-size-histogram.md) |

| 工具计量 | 工具结果字节直方 | ToolResultSizeHistogram——afterTool 单点记账五幂次边界桶(256B/1K/4K/16K/64K)+溢出桶+totalBytes 精确累计，守恒 successes=Σbuckets、executed=successes+failed，失败不入字节分布——Prometheus histogram 思想，opt-in 只读 Hook（spec 1401） | [spec 1401](docs/spec/1401-tool-result-size-histogram.md) |

| MCP 治理 | 入参 schema 破坏性分级 | McpSchemaCompatGrader——前后两版 inputSchema 客户端守恒视角机判：removed/type_changed/newly_required/enum_narrowed 四破坏轴+解析失败 fail-closed，reasons 典序——buf breaking 思想，822 目录 diff 显式留白轴的延伸（spec 1402） | [spec 1402](docs/spec/1402-mcp-schema-compat-grader.md) |

| 模型韧性 | EWMA 自适应超时推荐 | AdaptiveTimeout——record(observed) 喂样本 recommended() 出 clamp(⌈EWMA×3⌉,floor,ceiling) 推荐，α=0.3 CAS 无锁+预热哨兵 <3 样本不下结论；纯推导器不接线执行路径——Envoy timeout budget/Finagle 自适应超时思想（spec 1403） | [spec 1403](docs/spec/1403-adaptive-timeout.md) |

| 会话治理 | 会话 id 熵审计 | SessionIdEntropyAudit——字母表下界估计（观测字符类保守求和）+bits=length×log2(alphabet) 下界+四档闭集（WEAK&lt;64 时间戳档/STRONG≥112 UUIDv4 档）+批量四桶，弱 id（可猜串/自增）从静默变显形——nanoid 熵计算器思想（spec 1404） | [spec 1404](docs/spec/1404-session-id-entropy-audit.md) |

| 观测治理 | 查询页守卫与游标可读化 | DashboardQueryService——实证修复 listSessions 零钳制缺陷（size=1000 万即无界读 store、size≤0 subList 异常）+两路径裸 NFE 游标解析：MAX_PAGE_SIZE=200 常量钳制+可读 IAE，翻页语义逐位不变——Grafana query limit 思想（spec 1405） | [spec 1405](docs/spec/1405-dashboard-query-page-guard.md) |

| 会话治理 | 租约续期健康读面 | SessionLeaseGuard.renewalStats()——续期 failures/成功 renewals 双计数+续期时剩余租期最小水位（调度饿死/存储抖动收窄信号）+末次续期时刻+lost 终态，语义逐位不变——Redisson watchdog 健康审计思想（spec 1406） | [spec 1406](docs/spec/1406-lease-renewal-readout.md) |

| 护栏 | PII 检测器合成探针自查 | PiiProbeSelfCheck——内建确定性合成池（正例 EMAIL/手机号/测试 PAN/IPV4+负例×4）穿测 PiiDetector：逐类召回+误报哨兵（恒 0），基线标定锚=全召回；身份证号不入池（撞真实号红线）——spaCy/Presidio 评测思想（spec 1407） | [spec 1407](docs/spec/1407-pii-probe-selfcheck.md) |

| 模型韧性 | 断路器状态时长分析 | CircuitStateDurationAnalyzer——变迁流按模型积段积分：逐状态 total/segments/max+OPEN 占比 openShare（crash-loop 量化画像），无序容忍+采样窗口径入档——Resilience4j state duration 思想，只读零接线（spec 1408） | [spec 1408](docs/spec/1408-circuit-state-duration-analyzer.md) |

| 预算治理 | 多租户配额公平指数 | FairnessIndex——Jain 指数 J=(Σx)²/(n·Σx²)∈(0,1]+dominantShare 单点吃满检测+逐租户份额降序行动面+isFair(0.9 电信惯例)+全零 -1 哨兵；noisy neighbor 从人肉聚合变一读显形——Kafka client quota 公平性思想（spec 1409） | [spec 1409](docs/spec/1409-fairness-index.md) |

| 工具计量 | 入参校验读数 | ToolArgsValidator.validationStats()——validations/accepted 守恒+七错误桶（缺必填/类型/enum/数值/长度/非 JSON/其他）标记单源分桶、桶非互斥如实入档；三调用点静态入口自动全覆盖——Pydantic ValidationError 思想（spec 1410） | [spec 1410](docs/spec/1410-toolargs-validation-stats.md) |

| 会话治理 | 取消延迟追踪 | CancelLatencyTracker——onCancel 仅在途轮记未决键，轮终结消费入环（环 64+P50/P95 recent-rank），「取消信号→实际停止」时延显形，无轮取消不入账；单会话实例构造期绑定——Temporal cancellation latency 思想（spec 1411） | [spec 1411](docs/spec/1411-cancel-latency-tracker.md) |

| 工具计量 | 工具入参字节直方 | ToolInputSizeHistogram——beforeTool 单点记账：arguments 序列化 UTF-8 字节落同款五幂次边界桶+溢出桶，守恒 executed=Σbuckets，测量点=链前原始入参；与 1401 结果侧对称——Datadog DogStatsD 对称计量思想，opt-in 只读 Hook（spec 1412） | [spec 1412](docs/spec/1412-tool-input-size-histogram.md) |

| 会话治理 | 结构化输出 REASK 读数 | StructuredOutputStats——chatForEntity 漏斗五计数（attempts/firstPassParsed/reasks/reaskParsed/failures）双守恒式闭合+firstPassRate 模型 JSON 首过合规率派生（全零 -1 哨兵）；REASK 语义逐位不变——Instructor max_retries 可观测面思想（spec 1413） | [spec 1413](docs/spec/1413-structured-output-stats.md) |

| 观测治理 | Span 树拓扑读面 | SpanTreeTopology——Span 集合树结构形状分析：深度（根=1）/单节点最大扇出/根数/孤儿计数/kind 直方（数量降序）+父引用环防护（visited 收敛不炸栈）；与 Timings 族时延维正交——Jaeger DAG 思想（spec 1414） | [spec 1414](docs/spec/1414-span-tree-topology.md) |

| 观测治理 | 事件积压水位读数 | EventBackpressureStats——queueDepth 历史峰值水位+BLOCK 策略限时等待推入累计（&gt;0=容量曾打满），进程级静态读面+分发器只增记账埋点，EventBusStats 公共 record 不改形——Kafka consumer lag 思想（spec 1415） | [spec 1415](docs/spec/1415-event-backpressure-stats.md) |

| 持久化 | 事务计量装饰器 | InstrumentedUnitOfWork——UnitOfWork opt-in 包装：begun/completed/failed/inFlight 守恒+失败异常类 Top 榜（有界 8 并 OTHERS），异常原样上抛、deleteSession 透传，全部 SPI 实现可包——pg_stat_database xact + Seata 事务度量思想（spec 1416） | [spec 1416](docs/spec/1416-instrumented-unit-of-work.md) |

| 并发治理 | 舱壁在飞峰值水位 | AgentBulkhead.peakInFlight/peakSaturation——per-agent 历史最大并发水位+饱和度=峰值/上限（1.0=曾打满、无限舱 -1 哨兵），acquire 成功路径采样拒绝不虚高，256 折叠纪律；容量调大/错峰治理有水位可依——HikariCP 池饱和度思想（spec 1417） | [spec 1417](docs/spec/1417-bulkhead-peak-watermark.md) |

| 持久化 | Redis 慢操作榜 | RedisSlowOpLog——RedisMessageStore append/load/findById 客户端往返耗时严格大于阈值（默认 100ms 动态可调）入有界 FIFO 榜 32（新→旧现场）+totalSlowOps 累计水位（挤出也计）；服务端 slowlog 看不见的网络抖动/大 key 客户端延迟显形——Redis SLOWLOG 思想（spec 1418） | [spec 1418](docs/spec/1418-redis-slow-op-log.md) |

| 预算治理 | 预算分档分类器 | BudgetTierClassifier——已用/上限→行动档位：GREEN/WARN(≥0.8 软限预警)/HARD(≥1.0 硬限已触)/UNKNOWN(limit≤0 畸形)四档闭集+四桶计数+饱和度降序+tightest(n)「先看谁快烧完」——k8s ResourceQuota + SRE headroom 思想（spec 1419） | [spec 1419](docs/spec/1419-budget-tier-classifier.md) |

| 评估治理 | 评估运行年龄台账 | EvalRunAgeLedger——Registry begin/close 双点埋点：active 在途数+oldestActiveAgeMillis 最老活跃年龄（卡死异味哨兵 -1）+maxCompletedDurationMillis 历史最长完成水位+closed 累计；挂死的评估 run 从「感觉慢」变年龄显形——tqdm + k8s 运行时长异味思想（spec 1420） | [spec 1420](docs/spec/1420-eval-run-age-ledger.md) |

| 实验治理 | 实验分桶均衡审计 | ExperimentBalanceAudit——两段式声明桶集合+喂观测计数：逐桶份额/与均匀份额最大偏移（≤5pp A/A 惯例含端点）+balanced 判定，零分配桶计入（权重键名笔误先于显著性显形）、无样本 -1 哨兵不冒充——A/A test 思想（spec 1421） | [spec 1421](docs/spec/1421-experiment-balance-audit.md) |

| 观测治理 | Hook 顺序碰撞审计 | HookOrderAudit——钩子清单同序碰撞组显形：组内名字典序（=ChainComposition 兜底序，重命名即变序的装配脆性）、组间 order 升序、唯一 order 不占报告；修复=显式错开 order——Spring ordered-bean 审计思想（spec 1422） | [spec 1422](docs/spec/1422-hook-order-audit.md) |

| 工具计量 | 命令黑名单拦截判定读面 | 黑名单命中/放行比显形，二桶守恒（spec 1051） | [spec 1051](docs/spec/1051-blacklist-stats.md) |
| 工具计量 | run_command 执行结果分布读面 | 执行结局九桶守恒，参数/运行时分轴（spec 1052） | [spec 1052](docs/spec/1052-runcommand-stats.md) |
| 溢出治理 | evict_handle 逐出判定读面 | 模型主动逐出采用率与拒绝分桶显形，三桶守恒（spec 1053） | [spec 1053](docs/spec/1053-evict-stats.md) |
| 溢出治理 | str_replace 编辑判定读面 | 编辑成功与 notFound/ambiguous 失败模式分桶显形，六桶守恒（spec 1054） | [spec 1054](docs/spec/1054-strreplace-stats.md) |
| 记忆治理 | 情景记忆读写双守恒读面 | 情景库写入量与召回命中率显形，双守恒（spec 1055） | [spec 1055](docs/spec/1055-episodic-stats.md) |
| 模型韧性 | 崩循环探测器类级水位读面 | OPEN 总量/封顶截断量/循环检出/恢复四计数显形（spec 1056） | [spec 1056](docs/spec/1056-crashloop-watch-stats.md) |
| 技能治理 | skill_search 搜索判定读面 | 搜索命中率与零结果率显形，四桶守恒（spec 1057） | [spec 1057](docs/spec/1057-skillsearch-stats.md) |
| MCP 治理 | 工具集轮询提供器读面 | 热更新轮询三桶守恒显形，失败率可对账（spec 1058） | [spec 1058](docs/spec/1058-toolsetpoll-stats.md) |
| 记忆治理 | compact_now 手动压缩判定读面 | 模型主动压缩采用率与四结局桶守恒显形（spec 1059） | [spec 1059](docs/spec/1059-compactnow-stats.md) |
| 工程门禁 | core 零覆盖尾巴清扫（K 会话 R4） | 判据收紧 miss≥5→miss≥1 后复扫：AttachmentRenderer default 截断合同四断言 + CommandOutcome success 谓词矩阵（超时优先于退出码）；core 证据改走隔离 worktree（spec 1203） | [spec 1203](docs/spec/1203-core-zero-tail-sweep.md) |
| 工程门禁 | SnapshotMessage 补测与跨模块复核（K 会话 R5） | 收紧判据残留归口：compact 构造 null 防御 + Map.copyOf 拷贝语义合同；六小模块 miss≥1 复扫清单化（spec 1204） | [spec 1204](docs/spec/1204-snapshot-message-tightened-sweep.md) |
| 工程门禁 | K 会话周期对账轮 R6 | 全仓 verify（隔离 worktree CI 等价门）+ K 线工件链五项对账（spec/README/票/impl/map）——SRE Production Readiness Review 思想，R7 起对账/雾区交替（spec 1205） | [spec 1205](docs/spec/1205-k-audit-r6.md) |
| 会话治理 | SessionObserver 通知面异常隔离 | DefaultAgentSession 12 处观察者裸 forEach 通知点统一改走 notifyObservers 隔离派发——单观察者异常记 ERROR 后继续其余观察者、不向上传播（onOpen 未隔离时观测组件缺陷可炸掉会话构造且半初始化泄漏）——Guava EventBus SubscriberExceptionHandler 思想（spec 1500） | [spec 1500](docs/spec/1500-observer-notify-isolation.md) |
| 会话治理 | HookChain 事件通知面逐 hook 隔离 | fireEvent（通知面，无裁决语义）链内逐 hook try/catch——单 hook onEvent 异常不再吞掉其余 hook 的事件消费，计时 try/finally 仍入账；run() 裁决面 fail-fast 治理语义不动——通知面/裁决面分离（spec 1501） | [spec 1501](docs/spec/1501-hook-event-notify-isolation.md) |
| 观测治理 | 计时聚合器双子实例清零面 | HookTimingAggregator / ToolTimingAggregator 各补公开 reset()——Holder.reset() 只关聚合不清实例账，stats()/windowedMax() 此前只增不减（测试基线污染、长生命周期进程无法重建观测基线）——Prometheus counter reset 语义（spec 1502） | [spec 1502](docs/spec/1502-aggregator-reset.md) |
| 文档门禁 | 核心 API 包类级 Javadoc 覆盖门 | 32 个内核公共类型（AgentSession/BuzhouHook/HookResult/HarnessToolCallingManager 等）补类级 Javadoc + CoreApiJavadocCoverageTest 纪律变测试——六包新公共类型无 Javadoc 即 CI 红，注解夹层感知（spec 1503） | [spec 1503](docs/spec/1503-core-api-javadoc-gate.md) |
| 工具治理 | BuzhouTool destructive 风险注解 | @BuzhouTool 加 destructive()（MCP tool annotations destructiveHint 思想——工具自描述风险），write_file/run_command/http_request 标注；ToolsModule 危险名单从三处手工登记改为注解扫描驱动（行为等价），新工具标注即自动进 HITL 清单（spec 1504） | [spec 1504](docs/spec/1504-destructive-tool-annotation.md) |
| 评估治理 | 评估 run 协作式取消 | EvalRunner.requestCancel()——宿主发现数据集配错/方向不对时立即止损（此前只能跑完全程或等自动止损）；项边界生效（K8s Job 删除传播语义：在飞项做完、未启动项标 cancelled），串行/并行统一，run 开始清零防残留污染（spec 1505） | [spec 1505](docs/spec/1505-eval-run-cancel.md) |
| 评估治理 | A/B 对比 run 宿主取消（spec 1505 扩散） | PairwiseEvalRunner.requestCancel()——A/B 双 runtime 成本翻倍时同样可主动叫停；未起项复用 skipped 桶（与 SPRT 达界停同位），summary.hostCancelled 布尔区分统计达界停与宿主叫停（9/7 参兼容构造器保留，序列化仅取消 run 落位）（spec 1506） | [spec 1506](docs/spec/1506-ab-run-cancel.md) |
| 安全治理 | MCP 危险工具默认动词模式（S1 硬偏差修复） | buzhou.mcp.dangerous-tool-patterns 缺省从空改为 spec 14 §F 承诺的七动词前缀 glob（delete/drop/write/update/remove/send/exec）——恶意 server 的写侧工具默认进登记面；显式空列表 = 关闭逃生门（design-incompleteness S1 闭环）（spec 1507） | [spec 1507](docs/spec/1507-mcp-default-dangerous-patterns.md) |
| 安全治理 | 危险工具默认 HITL 自动带入桥（S2 硬偏差修复） | core DangerousToolRegistry 进程级桥（模块解耦不破白名单）——tools 装配后灌注危险名单，guard afterName 保时序默认并入三参 HITL 条目（yml 显式优先去重）；opt-in 开 write_file 即得默认审批拦截（spec 1508） | [spec 1508](docs/spec/1508-dangerous-tool-bridge.md) |
| 韧性观测 | canary.selected 事件会话归属补齐（F7）+ spec 07 续跑名回写（F10） | payload 补 sessionId（多会话监听面可定位，常量 Javadoc 自钉「sessionId + model」此前未兑现）；spec 07 的 AgentSession.resume() 推演名回写指向 SessionInterrupts.resumeWith（功能等价）——design-incompleteness F 系清扫轮（spec 1509） | [spec 1509](docs/spec/1509-canary-payload-f7-f10.md) |
| 装配治理 | ConfigMaps indexed 属性数字键归一 | properties/命令行/env-var 源的 key[i].f=v 从 Binder mapOf 弱点绑出的 Map 形态归一为数值序 List——guard dangerous-tools 等一切 fromYml 列表键在这些源下此前静默失效（YAML 源正常）；混合键不误伤（spec 1510） | [spec 1510](docs/spec/1510-configmaps-indexed-coerce.md) |
| 韧性执行 | 幂等工具瞬断重试自动装配（F1 落地） | buzhou.core.tool-transient-retry.enabled=true——@BuzhouTool.idempotent 或白名单工具装配期自动包既有 RetryingToolCallback（spec 05 承诺的声明式收口）；RetryPolicy 新增 transientOnly 瞬断白名单档（IO/超时族+类名启发+cause 链，非瞬断零重试；默认 false 语义不变）（spec 1511） | [spec 1511](docs/spec/1511-tool-transient-retry.md) |
| 文档门禁 | 配置全键表 config-reference（F9 闭环） | docs/config-reference.md 三段式全键表——57 record/198 组件键 + fromYml 子键（4 模块）+ Environment 直读键（34 个），spec 21 承诺落地（spec 1512） | [spec 1512](docs/spec/1512-config-reference.md) |
| 工程卫生 | 中断恢复与异常上下文（五-4 部分/五-8） | mcp shutdown 排空等待不再吞 InterruptedException（收窄 catch + 恢复中断位提前停止——JCIP 纪律）；DiskSpillStore 9 处裸异常 message 补操作名+路径/uri/sessionId 上下文（spec 1513） | [spec 1513](docs/spec/1513-interrupt-context-hygiene.md) |
| 工程卫生 | SHA-256 裸异常迁移 CONFIG_INVALID + 审计死代码 | 11 处 IllegalStateException("SHA-256 不可用")（全量重扫较评审清单多 5 处）统一迁移结构化 BuzhouException(CONFIG_INVALID/FATAL)（可分类可观测）；AuditChain.verifySignature 零调用死方法删除（逻辑已迁 AuditChainVerifier）（spec 1514） | [spec 1514](docs/spec/1514-sha256-config-invalid.md) |
| 存储治理 | 降级存储契约对齐 + 机制计数口径（六-1/七-1） | redis 版 DegradingObservabilityStore 补 buzhou.store.write.failures{policy=degrade} 指标（jdbc 先例同款——同名策略两库观测一致）；README 九大机制升十大（模型韧性层入列，与 CLAUDE 口径统一）（spec 1515） | [spec 1515](docs/spec/1515-store-contract-align.md) |
| 工程卫生 | spec 07 七切面回写 + 序位常量化（四-5/五-6 部分） | spec 07 三处「六切面」追认七切面（onModelError spec 15 落地后）；四处 advisor/hook 序位魔法值（+450/+460/100/200）抽常量+链位注释——ResilienceAdvisor.CHAIN_ORDER_OFFSET 先例同款，同值零行为变化（spec 1516） | [spec 1516](docs/spec/1516-spec07-order-consts.md) |
| 工程卫生 | spill 默认值单一事实源（五-6 收口） | 2048/20/32000 散落四处收口 SpillProperties 三常量（threshold 引用 SpillOffloadHook 既有常量）——改默认值从散弹变单点；六-6 死参数（SPI 扩展位注记）/六-9 调度器工厂（delay queue 无无界风险）裁定入档（spec 1517） | [spec 1517](docs/spec/1517-spill-defaults-ssot.md) |
| 文档门禁 | 文档间残留矛盾三裁定（七-2/七-3/四-8） | perf 口径澄清（10ms 目标/20ms 哨兵红线的目标-红线关系）；promptfoo star 统一 24K★@2026-09 时点注记；spec 09 追认 test 边豁免（ModuleBoundaryGuard 只扫 src/main 同口径）——design-incompleteness 可做项全部闭环（spec 1518） | [spec 1518](docs/spec/1518-doc-adjudications.md) |
| 文档门禁 | 日志双门面追认 + spec 04 属性回写（五-3/四-7） | SLF4J/System.Logger 双门面追认（69 文件既成风格不迁移，占位符风格硬约束不变）；spec 04 补 mcp 装配属性增量（dangerous-tool-patterns 缺省七动词/shutdown-budget/config-reference 指针）（spec 1519） | [spec 1519](docs/spec/1519-logger-spec04-adjudication.md) |
| 并发治理 | 观测管线构造器 this 逃逸修复 + 三裁定（六-7/五-2/六-2） | AsyncObservabilityPipeline 构造器立即 start 改首事件 CAS 惰性启动（零事件零线程）；CLAUDE 追认三键内 getProperty 直读边界；指纹双轨不统一裁定（值稳定性优先）（spec 1520） | [spec 1520](docs/spec/1520-this-escape-adjudications.md) |
| 工程卫生 | MemoryModule yml 解析样板统一（六-5 部分） | 13 处嵌套 instanceof 提取样板 → memoryLeaf/memorySub 两 helper 统一 9 处（3 处反射/泛型复杂体保留）——等值重构（spec 1521） | [spec 1521](docs/spec/1521-memory-yml-dedup.md) |
| 评估治理 | 并行评估波间剪枝（spec 901 边界收口） | 并行路径分波执行（cooperative batching）：每波 invokeAll 后按串行同款观察窗检查失败率，达阈值剩余项 pruned 不再起波——配置剪枝的并行大 run 从「诚实不剪」变「波间止损」；未配策略单波全量零变化（spec 1522） | [spec 1522](docs/spec/1522-eval-parallel-prune.md) |
| 评估治理 | A/B 并行波间早停（spec 1522 扩散） | PairwiseEvalRunner 并行路径分波化——SPRT 达界/宿主取消在波间真生效（此前全量派发 task 首行检查近似无效），波内 scored 原子语义不变（spec 1523） | [spec 1523](docs/spec/1523-ab-wave-earlystop.md) |
| 会话治理 | 配置错误显形（hook 重名/observer 重复注册） | HookChain 构造期重复 hook 名 WARN（派发序不稳定/stats 对位歧义信号，Kong 重名诊断思想）；addObserver 同实例幂等去重（双份通知是装配错误，静默双计污染读面）（spec 1524） | [spec 1524](docs/spec/1524-dup-name-observer-dedupe.md) |
| 工具治理 | serial-groups yml 通道（F2 残留收口） | buzhou.tools.serial-groups map（名→组）——yml 显式覆盖 @BuzhouTool 注解通道（同名优先），无注解工具亦可纯 yml 指定串行组；F2 全档闭环（超时键 ToolTimeoutOverrides 先行）（spec 1525） | [spec 1525](docs/spec/1525-serial-groups-yml.md) |
| 工具执行 | 批级工具结果回喂预算 | buzhou.core.tool-batch-response-budget > 0 声明即启用——批总量超限按响应长度降序贪心截大者（小结果保留完整，比平均截断信息保留更多），单工具限幅之上的批维度护栏（spec 1526） | [spec 1526](docs/spec/1526-batch-response-budget.md) |
| 工具执行 | 批预算错误反馈豁免（spec 1526 补强） | 超限批内的结构化错误反馈（模型自纠关键输入且通常很短）豁免截断——截它省不了预算却毁纠错；大结果承担截断（spec 1527） | [spec 1527](docs/spec/1527-batch-budget-error-exempt.md) |
| 存储执行 | 内存消息存储 load 已序快路径 | O(n) isSorted 检查免 O(n log n) 排序（load 是每轮模型调用的热路径，正常追加天然有序——长会话退化点消除）；乱序（time-travel 恢复序）回退全排序语义零变化（spec 1528） | [spec 1528](docs/spec/1528-load-sorted-fastpath.md) |
| 文档门禁 | memory/spill 公开类型类级 Javadoc 补齐（五-1 扩散） | 40 个零类级 Javadoc 公开类型（memory 21+spill 19）全部补齐角色一句话——R4 core 门的地模块扩散（spec 1529） | [spec 1529](docs/spec/1529-memory-spill-javadoc.md) |
| 文档门禁 | 类级 Javadoc 全仓收口（五-1 终轮） | 8 个补齐（AutoConfig×3+JdbcStore 五 SPI 实现——契约消费方可见面）；core 六包外 30 个裁定不补（internal 实现域，门辖界=语义 API 承诺面）（spec 1530） | [spec 1530](docs/spec/1530-javadoc-final-sweep.md) |
| 测试门禁 | 瞬断重试装配链测试（spec 1511 补账） | enabled 声明即 Holder 生效四断言 + 缺省零装配；装配测试当场实证并修复 Duration 转换缺陷（DurationStyle 宽松解析 1s/250ms+ISO，非法 fail-fast）（spec 1531） | [spec 1531](docs/spec/1531-retry-assembly-test.md) |
| 测试门禁 | 批预算装配链测试（spec 1526 补账） | 三用例——值声明透传/显式 0 仍装配但语义关（条件命中与语义开关解耦）/缺省零装配；M 系三个 Holder 装配测试全覆盖（spec 1532） | [spec 1532](docs/spec/1532-batch-budget-assembly-test.md) |
| 运维文档 | M 系增量运维段（runbook 第 24 节） | 九行机制表——通知隔离日志定位/评估取消与波间止损指标/危险模式与 HITL 桥开关/瞬断重试与批预算配置族/serial-groups yml/ConfigMaps 归一（spec 1533） | [spec 1533](docs/spec/1533-runbook-m-series.md) |
| 评估治理 | 评估 run 进度读面 | EvalRunner.progress()——runId/done/total/cancelled 不可变快照（串行每项后/波间/占位分支三处 volatile 更新），同步 run 的跨线程轮询面（UI/日志/探活，进度条思想）（spec 1534） | [spec 1534](docs/spec/1534-eval-progress.md) |
| 评估治理 | A/B 对比进度读面（spec 1534 扩散） | CompareProgress(runId/done/total/hostCancelled)——过程快照（每项/波间）+ 聚合终态快照（skipped null 占位无对象，终态统一 done=total）（spec 1535） | [spec 1535](docs/spec/1535-ab-progress.md) |
| 领域文档 | CONTEXT.md M 系术语段 | 新节「评估与执行治理」五条术语：通知面/裁决面分离、评估取消/剪枝/进度三件套、批级回喂预算、幂等瞬断重试双门、危险工具双通道（spec 1536） | [spec 1536](docs/spec/1536-context-m-terms.md) |
| 测试治理 | 进程匹配谓词收窄（周期预检发现） | interruptKillsProcessTree 的裸 "sleep 30" 子串谓词匹配全机进程——多会话共享机器上并行 shell 轮询循环命中误红（R41 实证）；锚定 marker 唯一路径 + 垂死窗口轮询（spec 1537） | [spec 1537](docs/spec/1537-hardening-test-predicate.md) |
| 文档门禁 | spec 05 判定项批量回写（F3/F4/F6） | 注入通道实现定案 per-session 组装（Builder Bean 通道不采用）；buzhou.parallel.* 键族按实现重写（真键四行+config-reference 指针）；退避随机源实现口径回写（spec 1538） | [spec 1538](docs/spec/1538-spec05-adjudications.md) |
| 文档门禁 | F8/F11 判定收尾（F 系全清） | 会话索引业务标签编程面 only 定案（wiring() 公开构造可传）；Spill 单路径 Hook 化定案（双 Hook 覆盖进出两向，manager 终检不采用）——F1-F11 全档闭环（spec 1539） | [spec 1539](docs/spec/1539-f8-f11-adjudications.md) |
| 工具执行 | FAILED_ONLY 占位的批预算豁免（组合补强） | 组合测试实证：同伴失败占位文本（54 字符元信息）非错误反馈格式被批预算截到 0——模型丢失成功信号；前缀常量单源 + 豁免纳入（spec 1540） | [spec 1540](docs/spec/1540-failedonly-placeholder-exempt.md) |
| 会话治理 | M 系雾区池终态裁定 | MCP 动态危险桥裁定不做（观测面+静态拦截面已覆盖）；望远镜构造器/Runtime 拆分/双降级收敛=长期重构移交；其余已闭环不留（spec 1541） | [spec 1541](docs/spec/1541-fogpool-adjudication.md) |

## 生产级纵深 XII（N 会话 1600 系增量）

N 会话（effort #1600+ 号段，借鉴 GitHub >10K star 项目）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 韧性缓存 | 语义缓存 LFU 采样驱逐 | 驱逐从纯 eldest 升级为采样窗口内最低命中数先出（平局取老保 LRU 底线），热 FAQ 条目不被一次性扫描写入冲刷；hotPreservedCount 观测采样实效率——Redis allkeys-lfu + maxmemory-samples 思想（spec 1600） | [spec 1600](docs/spec/1600-semantic-cache-lfu-sampling.md) |
| MCP 治理 | 连接最大寿命 | 到寿 ACTIVE 连接退役重建（复用探活失败同款排水+原样重建口径），在飞调用推迟下轮（归还时退役语义）——防长连接状态腐化/漂移累积；HikariCP maxLifetime 思想（spec 1601） | [spec 1601](docs/spec/1601-mcp-connection-maxlifetime.md) |
| 韧性治理 | 熔断启动宽限期 | 进程冷启动期（circuit.warmup）跳闸判定豁免——建连/TLS/预热抖动不计开闸，窗口照记、成功照常冲淡；宽限结束已积累样本立即恢复完整判定（真故障仍跳），warmupSuppressedCount 观测启动抖动量——K8s startupProbe 思想（spec 1602） | [spec 1602](docs/spec/1602-circuit-warmup.md) |
| 工具治理 | http_request per-host 并发上限 | 同 host 在飞请求超上限快速失败（拒绝不排队——保护目标服务与本进程连接资源不被单 host 打满）；hostLimitRejects 第七拒绝桶进守恒式——Nginx limit_conn 思想（spec 1603） | [spec 1603](docs/spec/1603-http-perhost-limit.md) |
| 韧性缓存 | 响应缓存 stale-if-error | stale-window 内过期条目保留——模型调用失败（熔断/网络/供应商故障）时旧响应救场不抛（staleReads 可观测），无救场条目异常照抛；流式不救场（out-of-scope）——Varnish grace / RFC 5861 思想（spec 1604） | [spec 1604](docs/spec/1604-response-cache-stale-if-error.md) |
| 评估闭环 | A/B 评估 SPRT 序贯提前终止 | 显著优势早现即停——符号检验 LLR 越界（α=0.05/β=0.10 可配）即停止剩余项（skipped 桶诚实分离，sprtDecision 入 summary/落盘/事件）；平局不进检验分母；未启用零变化——Wald SPRT / GrowthBook sequential testing 思想（spec 1605） | [spec 1605](docs/spec/1605-pairwise-sprt-early-stop.md) |
| 并发健康 | 虚拟线程 pinning 审计 + 金丝雀热路径修复 | 全仓 synchronized-IO 审计（高危 6 组/中危 11 组入档 spec）+ Top1 修复：CanaryToolCallback 路由三段式——monitor 只护决策与计数、工具执行移锁外（锁内远程调用钉住载体线程且串行化并行工具调用）——Netty「不阻塞事件循环」铁律 / JDK21 虚拟线程 pinning（spec 1606） | [spec 1606](docs/spec/1606-vthread-pinning-audit.md) |
| 并发健康 | RollingJsonlWriter 锁迁移 | appendLine/close/bytesWritten 的 monitor → ReentrantLock（互斥语义零变；磁盘写+flush+轮转 gzip 在虚拟线程下 unmount 而非 pin）——spec 1606 审计排队项落地（spec 1607） | [spec 1607](docs/spec/1607-rolling-jsonl-reentrantlock.md) |
| 并发健康 | DiskSpillStore 锁迁移 | store/usage 的 monitor → ReentrantLock（「一次调用一次 spill」互斥不变；MB 级写盘+walk 在虚拟线程下 unmount 而非 pin）——spec 1606 审计高危 #3 落地（spec 1608） | [spec 1608](docs/spec/1608-disk-spill-reentrantlock.md) |
| 并发健康 | WebhookOutbox 锁迁移 | append/appendRetry/orphanIndexCount/requeueDead 的 monitor → ReentrantLock wrapper（锁内 store put/scan 在虚拟线程 dispatcher 下 unmount 而非 pin）——spec 1606 审计中危 #1 落地（spec 1609） | [spec 1609](docs/spec/1609-webhook-outbox-reentrantlock.md) |
| 韧性治理 | 离群驱逐生产接线 + 分类感知 | spec 149 原语自 R11 前为未接线孤类（生产零调用）——advisor 全路径喂入（主/金丝雀/降级候选成败）+ 备模型候选驱逐过滤 + outlier.enabled 进程级装配（opt-in）；分类感知：failureCategories 默认 NETWORK/SERVER/TIMEOUT（AUTH/CONTENT 驱赶端点无意义——熔断同口径）（spec 1610） | [spec 1610](docs/spec/1610-outlier-ejection-wiring.md) |
| 韧性治理 | 孤类普查 + 熔断旁路遥测接线 | 全仓普查「类存在、测试齐全、生产零调用」孤类 15 项（19 类）+ 疑似 6 项入档 spec（系统性流程风险显形）；本轮修复 CircuitCrashLoopDetector（spec 811）与 HalfOpenProbeStats（spec 836）：withTelemetry 注入 + 跳闸/半开恢复/探测成败喂点 + 装配恒挂（纯读数旁路）（spec 1611） | [spec 1611](docs/spec/1611-orphan-census-circuit-telemetry.md) |
| 护栏治理 | guard 孤类装配面 | ToolRoleGuardHook（角色工具权限，K8s RBAC 式 fail-closed）与 InputFloodGuardHook（同输入泛洪防护）自 GuardModule.Builder 声明即注册——spec 141/167 两孤类救活（此前零装配路径）；未声明零注册——spec 1611 普查修复第二弹（spec 1612） | [spec 1612](docs/spec/1612-guard-orphan-assembly.md) |
| 会话治理 | 工具目录漂移看门狗接线 | CatalogDriftHolder 进程级基线 + 会话构造节拍拍指纹——目录增删改即 WARN + buzhou.catalog.drifted 计数（MCP 差量刷新错配/装配漂移显形）；首拍建基线、包装层不入指纹——spec 201 孤类救活（spec 1613） | [spec 1613](docs/spec/1613-catalog-drift-wiring.md) |
| 观测治理 | 指标新鲜度追踪接线 | metrics 装配链恒包 MetricFreshnessTracker（有界 512 名纯旁路）+ MetricFreshnessHolder 静态 audit——「某机制为何不再有数」从猜变查（序列静默=写路径死亡/装配丢失信号）——spec 802 孤类救活（spec 1614） | [spec 1614](docs/spec/1614-metrics-freshness-wiring.md) |
| 观测治理 | 泄漏疑似聚合接线 | LeakSuspectHolder.compositeWith 把聚合器复合进检测器 listener 链（宿主 listener 与聚合器双收）——泄漏「同一处反复漏 vs 多处散漏」从日志流水变排行（LeakSuspectHolder.report() 读出）——spec 839 孤类救活（spec 1615） | [spec 1615](docs/spec/1615-leak-suspect-wiring.md) |
| 工具治理 | 工具失败负缓存 | NegativeCachingToolCallback——同 key 失败短 TTL（默认 30s）记忆，窗内复读直接回错误文本不再真调（防模型重试风暴撞同一失败）；恢复窗口即 TTL、成功不缓存（与成功 memo 正交）、异常路径同缓存——DNS negative caching / NXDOMAIN 思想（spec 1616） | [spec 1616](docs/spec/1616-tool-negative-cache.md) |
| 并发治理 | 梯度式自适应并发闸 | GradientAdaptiveLimiter——延迟梯度驱动动态上限（长窗慢 EMA 基线 / 短窗快 EMA 近期）：劣化乘性下调（过载前兆先于失败规避）、变快加性上调、容错带防抖、warmup 学习期；与失败驱动 AIMD（spec 145）正交——Netflix Gradient2 / Envoy adaptive_concurrency 思想（spec 1617） | [spec 1617](docs/spec/1617-gradient-adaptive-limiter.md) |
| 预算治理 | Token 校准审计接线 | TokenBudgetHook.afterModel 同点对账——CharHeuristic 估算 prompt vs 模型回报 usage.promptTokens 成对入账（均值相对误差/偏高偏低占比/近窗 P95），CalibrationAuditHolder 读出——「预算按估算设、账单按真实来」的系统性偏差从感觉变数字——spec 819 孤类救活（spec 1618） | [spec 1618](docs/spec/1618-calibration-wiring.md) |
| Spill 治理 | 写入字节率限速 | SpillWriteRateLimiter 令牌桶（bytes/s + burst 突发容忍）——溢出写盘高峰不再打满磁盘带宽（背压传导）；maxWait 软限速超时放行 + degraded 计数（限速器故障不放大成 spill 失败）——RocksDB rate limiter 思想（spec 1619） | [spec 1619](docs/spec/1619-spill-write-rate-limit.md) |
| 会话治理 | 空闲监控全链接线 | IdleMonitorHolder 进程级（特征仓→监控器→直方）+ SessionFeaturesHook afterTurn 每 32 轮节拍 sweep——空闲超阈清单（翻转才通知）+ 时长分布入直方；纯观测旁路只判定不动作——spec 161/179/841 三孤类一次救活（spec 1620） | [spec 1620](docs/spec/1620-idle-monitor-wiring.md) |
| 工程质量 | 校准对账 NPE 修复 | R19 接线对测试替身链路（只带 response 的 ctx）NPE——对账前置三重缺席防御（request/prompt/instructions 任一 null 跳过，观测旁路缺席不记即诚实）；跨会话记档承接闭环（spec 1622） | [spec 1622](docs/spec/1622-calibration-npe-fix.md) |
| 会话治理 | 会话隔离检疫装配 | 连续失败达阈值（可配 3 缺省）→ 隔离一个指数升级冷却（30s 起 10m 封顶），隔离期 beforeTurn block 可读理由（剩 Xs）；成功复位走公共 API（hook 面不谎装自动复位）——Erlang supervisor「let it crash + 退避」思想，opt-in 默认关——spec 143 双孤类救活（spec 1621） | [spec 1621](docs/spec/1621-quarantine-wiring.md) |
| 韧性治理 | 影子读探针接线 | 主路成功后确定性采样（sha256(key)%100）对照首个备模型——agreed/diverged 双计数 + 分歧样本环（「备模型若被启用结果是否一致」的容量预案信心面）；旁路异常全吞、会话关闭竞态 REE 防护——Istio mirror 思想，spec 189 孤类救活（spec 1623） | [spec 1623](docs/spec/1623-shadow-probe-wiring.md) |
| 护栏治理 | 危险工具 HITL 豁免征询 | GuardExemptionRegistry 首个消费者——危险工具授权检查后征询豁免（「这条告警我看过、豁免到 T1」ESLint suppressions 思想）：未过期即放行 + guard.exemption.applied 审计事件，过期/撤销/无豁免恢复确认流程；GuardModule.exemptions() 暴露 grant/revoke——spec 820 孤类救活（spec 1624） | [spec 1624](docs/spec/1624-dangerous-tool-exemption.md) |
| 护栏治理 | 跨会话泄漏金丝雀接线 | SessionCanaryHook——每会话种植专属确定性令牌（sha256(sessionId|salt)），afterModel 扫描模型输出：他会话令牌出现即跨会话污染信号（guard.session.leak-detected 事件），自会话回显不算；opt-in leakCanary(salt)——thinkst canarytokens 思想，spec 528 孤类救活（spec 1625） | [spec 1625](docs/spec/1625-session-canary-wiring.md) |
| 护栏治理 | PII 脱敏豁免双粒度 | 工具级（该工具输出经核验整体豁免）+ 类型级（type:CN_PHONE 等——该类型误报豁免、其余照脱）——「规则误报已核验」与「该数据源可信」两种生产痛点各得其所——820 豁免登记第二消费者（spec 1627） | [spec 1627](docs/spec/1627-pii-exemption.md) |
| 护栏治理 | 输入侧 PII 豁免 | PiiInputRedactionHook 双粒度（会话级「该会话输入可信——内部已合规通道」+ 类型级 type:TYPE）——mechanism 域分侧与输出侧独立豁免，820 第三消费者（spec 1632） | [spec 1632](docs/spec/1632-pii-input-exemption.md) |
| 韧性治理 | 熔断慢调用率维度 | withSlowCallPolicy(duration, rate)——未到超时但持续慢（duration ≥ 阈值的成功调用）也是可用性问题：慢样本环形窗与失败窗并行，慢率或失败率任一达界即开闸（零失败前提可跳）；链式注入零配置零行为，主路径时长自动喂入——resilience4j slow call rate 思想（spec 1628） | [spec 1628](docs/spec/1628-circuit-slow-call.md) |
| 工具治理 | http_request 输入边界四护栏 | body 64K（超长走 bodyPath 通道带修法指引）/URL 8K/头数量 64/单头值 8K——模型自报超长输入不进执行层，拒绝入桶可观测不计失败——Envoy HTTP/2 SETTINGS_MAX_* 思想（spec 1629） | [spec 1629](docs/spec/1629-http-input-bounds.md) |
| 工具治理 | 负缓存装配面 | NegativeCachingHolder 进程级开关（默认关零包装）+ 会话装配链全工具包装——失败短 TTL 记忆（DNS negative caching，spec 1616）从宿主手动 wrap 升级为开关装配，未启用原引用透传（spec 1633） | [spec 1633](docs/spec/1633-negative-cache-assembly.md) |
| 观测治理 | dashboard 响应 gzip | writeJson 客户端协商（Accept-Encoding 含 gzip 且响应 ≥512B 才压——阈值下压缩头倒挂）；面板 JSON 数十 KB 起带宽显著削减（spec 1634） | [spec 1634](docs/spec/1634-dashboard-gzip.md) |
| 评估闭环 | eval 失败项重跑 | run(..., onlyItemIds) 子集重载——上轮 fail/error 的 id 传入即 rerun-failed：CI 红了只重跑失败项省时 + flaky 区分（重跑过=flaky、仍败=真回归）；null 全量零变化（spec 1635） | [spec 1635](docs/spec/1635-eval-rerun-failed.md) |
| 预算治理 | 校准系数建议 | calibrationFactorSuggestion——持续偏差给出把估算拉回真值的乘法系数（高估→<1 调低，一阶换算 1/(1+e)）；样本不足/零偏差 empty（不基于噪声给建议）——spec 1618 读数的可操作化（spec 1636） | [spec 1636](docs/spec/1636-calibration-factor.md) |
| 韧性治理 | 慢调用维度 yml 装配 | circuit.slow-call-duration 声明即启用（rate 阈缺省 0.5 与失败率阈同档、可配 (0,1]）——spec 1628 链式注入补全配置面，语义归位 Circuit 组（spec 1637） | [spec 1637](docs/spec/1637-slow-call-yml.md) |
| 护栏治理 | 泄漏金丝雀 yml 装配 | buzhou.guard.leak-canary.salt 声明即启用（salt 是防离线推演的秘密——建议环境变量注入）——spec 1625 编程面补全配置面（spec 1638） | [spec 1638](docs/spec/1638-leak-canary-yml.md) |
| 并发治理 | 梯度限流器观测接线 | executeToolCalls 批耗时经 GradientLimiterHolder 喂入梯度限流器（观测先行——View 读数显形全局工具路径延迟梯度；tryAcquire 闸接入待数据积累独立裁决）——spec 1617 装配面（spec 1639） | [spec 1639](docs/spec/1639-gradient-wiring.md) |
| 护栏治理 | PII 豁免计数 | PiiHitStats.recordExemption——「豁免了多少 vs 命中了多少」对照面（豁免失控显形：exemptionsApplied 持续高于命中数=豁免面过宽），reset 同步归零（spec 1640） | [spec 1640](docs/spec/1640-pii-exemption-count.md) |
| 工具治理 | 负缓存 yml 装配 | buzhou.core.negative-cache.{enabled,ttl}（默认 30s——DNS 短窗纪律）声明即启用，DisposableBean 关闭钩子停用（已包装会话缓存自然过期）——spec 1633 配置面补全（spec 1641） | [spec 1641](docs/spec/1641-negative-cache-yml.md) |
| 文档治理 | N 系运维手册段 | ops-runbook 第 23 节四族（缓存限流/熔断降级/护栏豁免/工具观测）——spec 1600-1641 全部可运维机制的配置键、观测读数与失控信号集中入档（spec 1642） | [spec 1642](docs/spec/1642-n-runbook.md) |
| 护栏治理 | 流式 PII 类型级豁免 | replyStreamFilter 创建时生效集剔除（type:TYPE——每轮窗口过滤器生效视图）；StreamTextFilter SPI 无会话上下文故会话级豁免不适用流式面（诚实边界）——820 豁免族四消费者闭环（spec 1643） | [spec 1643](docs/spec/1643-pii-stream-exemption.md) |
| 工程质量 | 新公开类型 @since 补全 | N 系 15 个 API 快照入档类型的 Javadoc 补 @since——api-surface 面的文档一致性（spec 1644） | [spec 1644](docs/spec/1644-since-annotations.md) |
| 工程门禁 | API 快照增量再生 | R27 后新增类型（GradientLimiterHolder 等）worktree 再生入档；同文件并行冲突化解（GuardModule.dangerousTools 双方同时加——M 系保留）（spec 1645） | [spec 1645](docs/spec/1645-snapshot-incremental.md) |
| 工程门禁 | N 会话收口 | 50 轮自迭代终验——五族成果总账（孤类普查救活×10/pinning 治理×4/高价值思想特性×14/豁免族四消费者/质量运维×5）+ 隔离 worktree 全仓 verify（spec 1648） | [spec 1648](docs/spec/1648-n-session-closing.md) |
| 观测治理 | ObservabilityAdvisor 流式路径分支补测（K 系 R13） | 本仓首个 Spring AI 流式 advisor 测试基建（StreamAdvisorChain stub harness）——此前 86 covered 分支全来自非流式路径（spec 1212） | [spec 1212](docs/spec/1212-advisor-stream.md) |
| 评估闭环 | A/B 胜率 Wilson 置信区间 | ab.run.completed 事件加 winRateA 95% CI（decided 口径分母）——「0.7 胜率（CI [0.42,0.88]）」与「0.7 胜率」是两个结论强度；小样本/极端比例不越界不出负值（正态近似经典缺陷），与 SPRT 决策面互补（spec 1630） | [spec 1630](docs/spec/1630-wilson-interval.md) |
| 韧性治理 | 退避抖动模式可配 | jitter-mode（EQUAL=既有 ±j 对称/FULL=[0,cap] 全随机——防重试风暴同步最优/DECORRELATED=[base,min(cap,prev×3)] 与前次去相关）——AWS「Exponential Backoff and Jitter」思想，默认 EQUAL 零行为（spec 1631） | [spec 1631](docs/spec/1631-jitter-mode.md) |
| 工程门禁 | N 会话中期对账审计 | 26 轮跨 6 模块首跑隔离 worktree 全仓 verify——API 快照非破坏新增 10 类再生入档 + api-surface.md 同步；spec 1622 悬空补档；16xx 全工件双向实存（spec 1626） | [spec 1626](docs/spec/1626-n-session-mid-audit.md) |

| 记忆治理 | Embedding 质量自查探针 | EmbeddingSelfCheck——合成句对（相似×2+无关对照×2）穿测 EmbeddingProvider：相似对余弦序逐对判定（免绝对阈值）+minMargin+orderHolds 回归哨兵+维度显形；换模型/供应商检索劣化从倒查变一行自查——OpenAI cookbook / sentence-transformers 思想（spec 1423） | [spec 1423](docs/spec/1423-embedding-selfcheck.md) |

| 并发治理 | 轮次限速拒绝榜 | TurnRateLimitHook.blockedSnapshot()——per-key 累计被拦次数（次数降序同次数字典序），256 封顶折 __overflow__，reset 清榜不清桶；「哪个租户在反复触发限速」从 Block 文案逐条捞变一表显形——Cloudflare WAF top-rules 思想（spec 1424） | [spec 1424](docs/spec/1424-turn-ratelimit-blocked-snapshot.md) |

| 会话治理 | 会话历史形态审计 | ConversationShapeAudit——roleHistogram 角色直方（降序典序）+连续同角色非 TOOL 相邻对（history 写坏信号）+空内容计数（带 toolCalls 为正常形态）+maxTurnGap 跳变（回放/乱序嫌疑）；上下文污染前的形态信号——MLflow 数据画像思想（spec 1425） | [spec 1425](docs/spec/1425-conversation-shape-audit.md) |

| 会话治理 | 用户输入重复审计 | UserInputDuplicationAudit——USER 输入归一化（trim/小写/空白折叠/截断 64）后审计：连续复读对+最长复读游程+distinctInputs 多样性+topRepeated 榜（≥2 才入容量 8 降序典序）；复读=最强挫败信号——Rasa 对话分析思想（spec 1426） | [spec 1426](docs/spec/1426-user-input-duplication-audit.md) |

| 治理 | 保留清扫新鲜度追踪 | RetentionSweepFreshness——addSweepListener 零侵入挂载：sweepCount+lastSweepAt+staleMillis（now−末次，调用方时钟）+maxGapMillis 相邻间隔水位（调度抖动/停摆显形）+failureCount 未完全成功分桶；清扫停摆=保留承诺静默失效的先行显形——Airflow scheduler heartbeat 思想（spec 1427） | [spec 1427](docs/spec/1427-retention-sweep-freshness.md) |

| 工具治理 | 工具目录重名审计 | ToolCatalogDuplicateAudit——工具名清单重名组审计（≥2 同名组、名字典序、去重对比）：HarnessToolCallingManager HashMap 按名索引重名静默覆盖（后到者胜），本地与 MCP server 工具同名遮蔽不可解释——Spring bean 重名/Maven Enforcer 思想，只读不裁决（spec 1428） | [spec 1428](docs/spec/1428-tool-catalog-duplicate-audit.md) |

| 恢复治理 | 运行状态分布与滞后审计 | RunStatusDistribution——恢复快照状态直方（全枚举预置计 0）+turnLag 崩溃暴露窗口（currentTurn−lastCompletedTurn：崩溃将丢的未持久化轮数）+worst offenders 榜（滞后降序容量 3）；RUNNING 淤积与暴露窗口一读显形——Temporal workflow stats 思想（spec 1429） | [spec 1429](docs/spec/1429-run-status-distribution.md) |

| 会话治理 | 会话关闭耗时读数 | SessionCloseStats——closed/closeFailures 双计数+last/maxCloseDurationMillis 耗时水位（单调），close() 清理优先异常聚合语义逐位不变只增记账；排空慢（observer 慢/逆序关闭卡住）从停机窗口超限倒推变水位显形——k8s graceful shutdown 思想（spec 1430） | [spec 1430](docs/spec/1430-session-close-stats.md) |

| 观测治理 | 指标命名校验器 | MetricNameAudit——validate(name)→NameVerdict 违规闭集（EMPTY/WHITESPACE/PREFIX/SEGMENT_EMPTY/CASE/CHARS）首违不短路一次看全，段规则与 starter MetricNamingGuardTest 门同源；机制作者命名自查即时反馈——Prometheus metric naming 规范思想（spec 1431） | [spec 1431](docs/spec/1431-metric-name-audit.md) |

| 会话治理 | 会话 spawn 统计读面 | SessionSpawnStats——spawn 漏斗 attempts/successes/collisions/steals 守恒式（attempts=successes+collisions）+activePeak spawn 时点活跃峰值水位（口径显式仅 spawn 路径采样）；同 id 冲突高发与抢占频次从静默变漏斗显形——HikariCP 建连统计思想（spec 1432） | [spec 1432](docs/spec/1432-session-spawn-stats.md) |

| 治理 | 工具循环打断分布 | ToolLoopBreakerHook.brokenByToolSnapshot()——per-tool 累计打断次数（降序典序）+brokenTotal+maxRunObserved 打断时点最长 run 水位，reset 清分布不清会话 run 状态；「哪个工具在烧配额」定向治理信号显形——Temporal retry-loop detection 思想（spec 1433） | [spec 1433](docs/spec/1433-tool-loop-break-stats.md) |

| 并发治理 | 延迟作业调度漂移 | DelayedJobQueue.driftStats()——实际起跑 vs 计划 fireAt 的漂移读数（executed/last/max 水位），过期补跑漂移显形正值=补偿逻辑错过窗口量化，既有替换/取消语义不变——Sidekiq queue latency 思想（spec 1434） | [spec 1434](docs/spec/1434-delayed-job-drift.md) |

| 工具治理 | 工具 schema 健康审计 | ToolSchemaHealthAudit——analyze(List<ToolCallback>) 四态分桶（VALID/MISSING/UNPARSEABLE/NOT_OBJECT）与校验器跳过条件严格同口径+bypassRatio 裸奔率派生（-1 哨兵）+findings 封顶 16；「多少工具在裸奔无参数校验」从 permissive 盲区变审计显形——ajv/OpenAPI 思想（spec 1435） | [spec 1435](docs/spec/1435-tool-schema-health-audit.md) |

| 观测治理 | 事件时序单调性审计 | EventOrderAudit——同会话事件 occurredAt 时序单调性审计：逆序相邻对数+最大倒退量（时钟回拨/乱序写入显形）+首逆序位定位（-1 哨兵），等时刻不算逆序、null 时戳跳过——事件溯源不变量思想（spec 1436） | [spec 1436](docs/spec/1436-event-order-audit.md) |

| 评估治理 | 评估数据集质量审计 | DatasetQualityAudit——analyze(List<EvalItem>) 退化条目分桶：空 input/emptyExpecteds/短 input（&lt;8 字符信息量不足）+输入长度 P50/P95 秩插值+degenerateRatio 退化比派生（-1 空集哨兵）；「数据集还能信吗」从逐条翻看变一行审计——Cleanlab 数据质量思想（spec 1437） | [spec 1437](docs/spec/1437-dataset-quality-audit.md) |

| 会话治理 | 悬空轮检测器 | DanglingTurnDetector——按 turnSeq 分组检测悬空轮：有 USER 无 ASSISTANT 即悬空（取消/中断残留，TOOL 链不豁免、仅 TOOL/SYSTEM 轮不算）+danglingSamples 升序封顶 8+hasDangling 哨兵；续聊质量风险显形——Temporal activity 检测思想（spec 1438） | [spec 1439](docs/spec/1438-dangling-turn-detector.md) |

| 工程门禁 | L 会话阶段对账 R40 | LSessionLedgerAuditTest——工件链四面互证对账测试（范围自扩展）：票对公式/impl 切片窗/README 行/spec 号连续性；同批实证修复票号 +2 漂移与 impl 置换——J/K 对账轮先例（spec 1440） | [spec 1440](docs/spec/1440-l-audit-r40.md) |

| Spill 治理 | Spill 冷热分层访问审计 | SpillTieringAudit——读事件按 uri 聚合对照存量全集：never（从未回读冷数据）/single（一次性消费）/multi（≥2 热点）三桶+hotRatio/coldRatio 占比派生（空库 -1 哨兵）；TTL/清理策略与预热依据显形——MinIO tiering 思想（spec 1441） | [spec 1441](docs/spec/1441-spill-tiering-audit.md) |

| 评估治理 | 门阈值敏感性扫描 | GateThresholdSensitivity——analyze(scores, threshold, δ) 扫 δ 带 [threshold−δ, threshold+δ)：带内分数计数+tightenFlips 上调翻 FAIL/loosenFlips 下调翻 PASS 分向+sensitivityRatio 敏感率派生；门立在分数稀疏带还是密集带一读显形——scikit-learn validation_curve 思想（spec 1442） | [spec 1442](docs/spec/1442-gate-threshold-sensitivity.md) |

| 持久化 | Saga 运行静态读数 | CompensatingBatch.sagaStats()——补偿型事务运行漏斗：runs/successes/compensationRuns/compensationFailures 四计数守恒式（conserved 派生）+stepsExecuted+lastFailedStep 断点步名；补偿高发从静默变漏斗显形——Seata 事务度量思想（spec 1443） | [spec 1443](docs/spec/1443-saga-run-stats.md) |

| 评估治理 | 评估通过率趋势审计 | EvalPassRateTrend——跨 run 通过率序列的 Theil–Sen 稳健斜率审计：成对斜率中位数（离群 run 不扭曲方向）+死区 ε=0.005/run+INSUFFICIENT 哨兵；「这版在变好还是变坏」从人眼比对变方向判定——Theil–Sen 稳健回归思想（spec 1444） | [spec 1444](docs/spec/1444-eval-pass-rate-trend.md) |

| Spill 治理 | 媒体摄入统计读面 | MediaIntake.stats()——intakes/readBacks/bytesTotal 三计数+per-MIME 摄入直方（数量降序典序、封顶 16 基数纪律），intakeText 复用同一漏斗自动计量、拒绝路径不入账；多模态使用画像与字节配额治理显形——OpenAI usage by modality 思想（spec 1445） | [spec 1445](docs/spec/1445-media-intake-stats.md) |

| 导出治理 | 导出清单校验统计 | ExportManifestVerifyStats——三受踪包装（canonical/subset/full 委托+入账）：verifies=ok+failed 守恒+mismatched/missing/unexpected 明细桶累计+lastEntryKind 末次入口；「从不校验」与「校验全红」两种病灶显形——TUF 校验遥测思想（spec 1439 补位） | [spec 1439](docs/spec/1439-export-manifest-verify-stats.md) |

| Spill 治理 | 语义切片索引覆盖读面 | SemanticChunkIndex.coverageStats()——已索引 uri 数/切片总数/单 uri 最大切片数与最厚制品定位（切片失衡信号），空索引零哨兵、provider 不可用短路覆盖恒空——Elasticsearch index stats 思想（spec 1447） | [spec 1447](docs/spec/1447-semantic-chunk-coverage.md) |

| 观测治理 | 事件去重聚合读面 | EventDeduplicator.deduplicationStats()——passed/deduped 双计数（守恒 seen=passed+deduped）+去重率派生（0 总量 -1 哨兵）+ringSize/capacity 环占用；重复事件注入压力从静默 counter 变比率显形，reset 只清计数不清环——SendGrid/Mailgun webhook replay 思想（spec 1448） | [spec 1448](docs/spec/1448-event-dedup-stats.md) |

| 评估治理 | 轮次采样漏斗读面 | TurnSamplerHook.samplerStats()——采样漏斗六计数：turnsSeen/emptySkipped/shortSkipped/rateSkipped/written/writeFailures（桶口径互斥、写失败 fail-soft 分桶）；「为何没进数据集」按原因归因——Envoy access log sampling 思想（spec 1446） | [spec 1446](docs/spec/1446-turn-sampler-funnel.md) |

| 收口 | L 会话 50 轮收口终验 | 全反应堆串行回归绿+快照门/覆盖门/对账门全绿+台账 50/50 归档——effort 1400–1449 连续（1439 补位轮兑现） | [spec 1449](docs/spec/1449-l-closing.md) |

| 技能治理 | SkillAdmin 管理操作分布 | SkillAdminApi 五操作计数（create/update/publish/disable/delete 静态面）——管理面治理审计基座，操作分布显形（spec 1085，J 会话 M 段产出） | [spec 1085](docs/spec/1085-skilladmin-stats.md) |

## 生产级纵深 XI（L 会话 1700 系增量）

L 会话（effort #1700+ 号段，借鉴 GitHub >10K star 项目）增量（每项默认零行为变化或 opt-in）：

| 分组 | 能力 | 一句话 | 详设 |
|------|------|--------|------|
| 评测计量 | 评测分数 MAD 鲁棒离散度 | EvalScoreMad——median/MAD（中位数绝对偏差）+修正 z 值（0.6745·|x−med|/MAD>3.5 Iglewicz–Hoaglin）离群 run 定位+Dispersion 三档（INSUFFICIENT n<3 哨兵/TIGHT MAD=0 收紧档偏离直接判离群/SPREAD）——Prometheus/Thanos 鲁棒统计思想，门→趋势（1444）→离散三维补齐（spec 1700） | [spec 1700](docs/spec/1700-eval-score-mad.md) |
| 评测计量 | 评测项轮换消序 | EvalOrderRotator——runIndex 派生种子 Fisher–Yates 置换（java.util.Random LCG 跨 JVM 重现）+shuffled 多重集守恒+OrderPlan 审计复现——OpenAI Evals/HELM 种子化顺序思想，与 1700 MAD 配套分辨顺序效应 vs 能力波动（spec 1701） | [spec 1701](docs/spec/1701-eval-order-rotator.md) |
| 评测计量 | 评测集覆盖矩阵 | EvalCoverageMatrix——标签×用例计数矩阵+missingFrom 零覆盖漏测清单+归一化香农熵（ln k 归一，1=均衡 0=偏科）——JaCoCo/Stryker 覆盖思想+scikit-learn 信息熵（spec 1702） | [spec 1702](docs/spec/1702-eval-coverage-matrix.md) |
| 评测计量 | 裁判位置偏差读面 | JudgePositionBias——成对裁决 (A,B)×换位 (B,A) 镜像自洽四桶（一致/首位双赢/次位双赢/混合平）+biasRatio（空哨兵 −1）——MT-Bench/FastChat 位置偏差检验（spec 1703） | [spec 1703](docs/spec/1703-judge-position-bias.md) |
| 评测计量 | 评测集内容指纹 | EvalSetFingerprint——\n 规范形 SHA-256 指纹 sha256- 前缀+ORDERED/UNORDERED 双序口径——DVC/HuggingFace Datasets 数据集指纹思想，跨 run 分数对比先验同数据（spec 1704） | [spec 1704](docs/spec/1704-eval-set-fingerprint.md) |
| 评测计量 | 门限边际直方 | EvalGateMargin——|rate−threshold| 逐 run 边际+min/max+withinBand 危险带计数（空哨兵 −1）——Google SRE 告警边际/SPRT 边际思想，「门过多悬」直接读数（spec 1705） | [spec 1705](docs/spec/1705-eval-gate-margin.md) |
| 工程治理 | O 系对账门 | OSession1800LedgerAuditTest——150 轮工件链四面互证（spec 1800–1949 ↔ README 行 ↔ T 票对 ↔ impl，号段公式 R1 钉死：T2801+2(N−1800)/1401+(N−1800)）——LSession1700LedgerAuditTest 预防式对账同款，漂移落盘瞬间即红（spec 1800） | [spec 1800](docs/spec/1800-o-series-ledger-audit.md) |
| 溢出保护 | Spill 压力失速读面 | SpillPressureStall——PSI 双档失速账目 some（≥1 会话失速=吞吐损失）/full（活跃全失速=进度损失）+峰值窗失速面（空观测 -1 哨兵）——Linux 内核 Pressure Stall Information 思想，压力不看水位看谁在等（spec 1801） | [spec 1801](docs/spec/1801-spill-pressure-stall.md) |
| 工具执行 | 轮墙钟预算传播裁决 | TurnDeadlineBudget——轮总预算沿顺序调用链传播递减，获准超时=min(预估,剩余)截断、耗尽即拒（零耗也不例外，deadline 先于派发检查）+committedNanos/admissionRatio——gRPC deadline propagation/Temporal schedule-to-close 思想，轮墙钟不被 N 次单超时拖成 N 倍（spec 1802） | [spec 1802](docs/spec/1802-turn-deadline-budget.md) |
| 记忆压缩 | 层代晋升审计 | MemoryPromotionAudit——微压缩→摘要/归档层间流动率（promotionRate/directArchiveRate）+过早晋升周期计数（有产出零原地保留的轮）——JVM 分代 GC 晋升诊断思想，晋升率常高=年轻代没拦住短命内容、过早晋升堆积=缓冲失效（spec 1803） | [spec 1803](docs/spec/1803-memory-promotion-audit.md) |
| 模型韧性 | 前缀块命中读面 | PrefixBlockHitStats——等长块切分的跨请求复用账（尾块不入账/请求内去重/radix 一次插入语义）+blockHitRatio——vLLM block-level prefix cache/SGLang radix 思想，整请求命中率与块命中率分开读，前缀投资有依据（spec 1804） | [spec 1804](docs/spec/1804-prefix-block-hit-stats.md) |
| 工程治理 | O 系 R6 对账轮 | 全仓 clean verify 三门核账（覆盖/快照/对账）+ 快照补登四类型 + 并行吸收（J 系 skills 断链两连修复：11bc4d3a 改名滑手→005cc8f0/a8b7885d 并发对撞镜像断链→7fdb1611 和解）——kill 在途 verify 留部分编译态的教训入档（spec 1805） | [spec 1805](docs/spec/1805-o-r6-reconciliation.md) |
| 恢复韧性 | 检查点滞后读面 | CheckpointLagReadout——逐会话产出/检查点水位差账目：totalLag（全会话重放成本）/maxLag（最坏单会话，-1 哨兵）/sessionsBeyond 越限计数/caughtUpRatio 追平率——Kafka consumer-group lag/SQLite WAL checkpoint 思想，崩溃重放成本与检查点节奏健康度直接读数（spec 1806） | [spec 1806](docs/spec/1806-checkpoint-lag-readout.md) |
| 流量治理 | 追限事件会话化 | ViolationEpisodeMerger——追限时点按容忍窗切段（Episode start/end/hits/span）：episodes 段数/longest 最长段/hitsPerEpisode 平均密度——Prometheus 告警分组/GA session gap 会话化思想，散点抖动（加余量）与持续超载（扩容降级）分开读（spec 1807） | [spec 1807](docs/spec/1807-violation-episode-merger.md) |
| 溢出保护 | 驱逐信号阈值门 | EvictionThresholdGate——两级阈值三态裁决：signal≥hard 立即逐（不受宽限豁免）/signal≥soft 宽限满即逐、未满 GRACE_PENDING+census 多信号三态普查——K8s eviction manager 思想，软阈给宿主自救窗、硬阈保命，临界点不抖动驱逐（spec 1808） | [spec 1808](docs/spec/1808-eviction-threshold-gate.md) |
| 工具执行 | 扇出 pacing 计划 | FanoutPacingPlan——并行扇出起发排程：头部 headStart 名额立即发（TCP IW 语义）、尾部按间隔匀速放行（pacing）+totalSpanMillis/pacedRatio——TCP 拥塞控制思想，小扇出零延迟、大扇出不惊群，两极连续可调（spec 1809） | [spec 1809](docs/spec/1809-fanout-pacing-plan.md) |
| 背压治理 | Prefetch 信用窗口 | PrefetchCreditWindow——在飞上限口径背压闸：tryAcquire 满窗即拒（totalExhausted 流控压力读数）/release 确认归还信用/stats 快照+utilization——RabbitMQ basic.qos/AMQP credit-based flow control 思想，免速率估计、下游多快上游多快（spec 1810） | [spec 1810](docs/spec/1810-prefetch-credit-window.md) |
| 工程治理 | O 系 R12 对账轮 | 快照补登前置（R7–R11 五类型一次入账，R6 两遍 verify 教训固化）+ 全仓 clean verify 三门绿 + GitHub 中断期积压补推确认（fe350325..b69b9864）（spec 1811） | [spec 1811](docs/spec/1811-o-r12-reconciliation.md) |
| 事务语义 | 半消息审计 | HalfMessageAudit——三态意图账目（HALF 滞留/COMMITTED 放行/ROLLED_BACK 丢弃）+staleHalves 超回查阈候选+resolutionRatio/pendingRatio——RocketMQ 事务消息思想，「发消息」与「做事务」原子性靠半消息两阶段+回查兜底（spec 1812） | [spec 1812](docs/spec/1812-half-message-audit.md) |
| 健康治理 | TTL 探针状态机 | TtlProbeStateMachine——被动健康三态判（PASSING/STALE 过预警线/CRITICAL 到期，双边界含上）+freshness 剩余新鲜度（到期钳 0）+census 普查——Consul health check TTL 思想，过期靠时间自然到期零轮询、STALE 先兆可提前处置（spec 1813） | [spec 1813](docs/spec/1813-ttl-probe-state-machine.md) |
| 溢出保护 | 热点重平衡建议器 | HotspotRebalancer——分片/卷间负载贪心搬迁建议：极差≤容差停手、单步量三重 min 封顶不越衡反转、并列 id 字典序确定性+spreadBefore/After/improvementRatio——K8s descheduler 思想，只消越容差热点不过度均衡（spec 1814） | [spec 1814](docs/spec/1814-hotspot-rebalancer.md) |
| 恢复韧性 | 缺口回填计划器 | GapBackfillPlanner——期望区间×在位集合的缺口闭区间清单（含首尾、升序合并）+largestGapSpan 瓶颈段长+missingRatio/complete——Kafka offset 补填/Prometheus backfill 思想，重放不重不漏先知道缺哪段、区间化可分批可并行（spec 1815） | [spec 1815](docs/spec/1815-gap-backfill-planner.md) |
| 背压治理 | 负载脱落阶梯 | LoadShedLadder——过载分级甩负载：每级一脱落阈值（低阈值=低优先级先掉），负载因子越阈即该级拒新（含边界）+shedRatio/escalating——Envoy overload manager 思想，一刀切全拒变按优先级逐级甩（spec 1816） | [spec 1816](docs/spec/1816-load-shed-ladder.md) |
| 工程治理 | O 系 R18 对账轮 | 快照补登前置第三例行（R13–R17 五类型：HalfMessageAudit/TtlProbeStateMachine/HotspotRebalancer/GapBackfillPlanner/LoadShedLadder）+ 全仓 clean verify + Wave 4 排程落图——R6 教训→R12 固化→R18 例行的对账节奏（spec 1817） | [spec 1817](docs/spec/1817-o-r18-reconciliation.md) |
| 溢出保护 | 顺序读预读顾问 | ReadAheadAdvisor——尾链检测三态（SEQUENTIAL 相接链/RANDOM 跳读/COLD 样本不足）+预读指数放大封顶 8 倍（blockSize×2^min(链长−1,3)）、跳读零预读——Linux readahead 思想，预读窗随访问形状自适应（spec 1818） | [spec 1818](docs/spec/1818-read-ahead-advisor.md) |
| 成本治理 | 预算花费匀速曲线 | BudgetPacingCurve——匀速基线三态判（ON_PACE 带内/OVER_PACING 超前烧钱/UNDER_PACING 落后漏损）+deviation 偏离+runRate 运行率（期末烧几倍，-1 哨兵）——广告 spend pacing 思想，浮点噪声免疫边界（spec 1819） | [spec 1819](docs/spec/1819-budget-pacing-curve.md) |
| 模型韧性 | 重启错峰计划 | RestartSpreadPlan——同批实例稳定哈希分槽错峰（同 id 永远同槽确定性可回放，delay∈[0,window)）+cohort 槽碰撞账/collisionRatio——memberlist/consul 协同重启+AWS jitter 思想，重启风暴（同刻重试打爆下游）确定性摊开（spec 1820） | [spec 1820](docs/spec/1820-restart-spread-plan.md) |
| 并发治理 | 优先级反转暴露读面 | PriorityInversionExposure——持有关系反转账：holderRank>waiterRank 即反转（值小=关键）+worstRankGap 拖死强度+未知资源等待诚实账+inversionRatio——OS 优先级反转（Mars Pathfinder 教训）思想，关键路径被低优持有者拖死显形为可计数风险面（spec 1821） | [spec 1821](docs/spec/1821-priority-inversion-exposure.md) |
| 工具执行 | 混沌预算门 | ChaosBudgetGate——实验节律闸三态（MAY_RUN/OUT_OF_BUDGET/FORBIDDEN_WINDOW，窗口优先于预算安全第一）+usage 使用账（钳零/烧尽比/超支照实）——Netflix Chaos Monkey/Chaos Toolkit 思想，与 ChaosMonkeyHook 执行器构成闸+执行对（spec 1822） | [spec 1822](docs/spec/1822-chaos-budget-gate.md) |
| 工程治理 | O 系 R24 对账轮 | 快照补登前置第四例行（R19–R23 五类型：ReadAheadAdvisor/BudgetPacingCurve/RestartSpreadPlan/PriorityInversionExposure/ChaosBudgetGate）+ README 行先落再 verify（R18 漏行教训内化）+ GitHub 二次中断积压补推链（spec 1823） | [spec 1823](docs/spec/1823-o-r24-reconciliation.md) |
| 模型韧性 | Stale-While-Revalidate 策略 | StaleWhileRevalidatePolicy——缓存年龄三态判（FRESH 直接服务/STALE 回旧值+异步后台刷新/EXPIRED 同步回源）+staleness 陈旧度读数（新鲜钳 0/窗内进度/过期 ≥1）——HTTP Cache-Control SWR/CDN 思想，p99 不吃回源延迟、尾延迟换轻微陈旧性显式交易（spec 1824） | [spec 1824](docs/spec/1824-stale-while-revalidate.md) |
| 会话治理 | 会话休眠分级 | SessionHibernationPolicy——闲置三档（ACTIVE 全热/DROWSY 预降级半足迹轻税/HIBERNATED 降冷一成足迹重税，边界含上）+档位画像（唤醒税 0/50/2000ms 与足迹比常量）+census 足迹节省率——k8s scale-to-zero/duty-cycling 思想，「省多少 vs 醒多慢」可算（spec 1825） | [spec 1825](docs/spec/1825-session-hibernation-policy.md) |
| 会话治理 | 会话布隆粗筛 | SessionBloomFilter——「从未见过」确定性快判（false 即一定没见过，零假阴性契约；true 可能见过小概率误报）+确定性哈希可回放+fillRatio 饱和度重建建议——Bloom filter 思想，位图级内存换新会话零索引快路径（spec 1826） | [spec 1826](docs/spec/1826-session-bloom-filter.md) |
| 会话治理 | 优雅停机排空预测 | DrainForecast——排空 makespan 预测：max(最长单会话剩余, ceil(总剩余÷并行度))×单位耗时+bottleneckSession 瓶颈直读+parallelismBound 主导方（workBound 催单点/parallelismBound 加并行延窗）——k8s drain/Envoy shutdown drain 思想，停机超时=预测×安全余量不拍常数（spec 1827） | [spec 1827](docs/spec/1827-drain-forecast.md) |
| 工具执行 | 平滑加权轮询序列 | SmoothWeightedSequence——NGINX smooth WRR 逐字算法（current 累加-峰值派出-回收）生成派发序：权重比例保持且交错平滑（5:1:2 派 aabacaad 非 aaaaabc）+counts 直方+零权重不参与+确定性可回放——朴素 WRR 负载锯齿变平滑曲线（spec 1828） | [spec 1828](docs/spec/1828-smooth-weighted-sequence.md) |
| 工程治理 | O 系 R30 对账轮 | 快照补登前置第五例行（R25–R29 五类型：SWR 策略/休眠分级/布隆粗筛/排空预测/平滑加权）+ 全仓 clean verify + 30/150 里程碑（1/5）+ GitHub 三次中断积压补推链（spec 1829） | [spec 1829](docs/spec/1829-o-r30-reconciliation.md) |
| 模型韧性 | 多级缓存命中读面 | MultiLevelCacheStats——两级（L1 进程内/L2 共享 store）逐层命中率+联合命中率+L1 失职率（L2 命中占非回源比——本可 L1 拦下的份额）——Caffeine multi-level/CPU L1-L2 思想，升容量/查预热/查键口径三分诊（spec 1830） | [spec 1830](docs/spec/1830-multilevel-cache-stats.md) |
| 模型韧性 | 对冲延迟策略 | HedgeDelayPolicy——对冲阈值从延迟分布推导（最近秩 P95，样本不足退守地板不冒进）+decide 边界含上（elapsed≥阈即 SEND_HEDGE）——Google Tail at Scale hedged requests 思想，尾部 5% 才付双倍钱、中位数零对冲成本（spec 1831） | [spec 1831](docs/spec/1831-hedge-delay-policy.md) |
| 并发治理 | 覆写环形缓冲 | OverwritingRingBuffer——固定容量环形满则覆写最老（永不阻塞写入方）+overwrites 覆写计数可审计+items 最老到最新防御拷贝快照——LMAX Disruptor 思想，最近窗采样「不挡主路+丢得起有数」基建（spec 1832） | [spec 1832](docs/spec/1832-overwriting-ring-buffer.md) |
| 会话治理 | 续读令牌编解码裁决 | ResumeTokenCodec——游标绑定数据指纹（encode/decode 回路）+check 三态 VALID/STALE_DATA（换代，重拉首页）/OUT_OF_RANGE（越界，查保留）——分页 continuation token/ETag 思想，指纹先行：底层数据换代后旧游标不再被当有效（spec 1833） | [spec 1833](docs/spec/1833-resume-token-codec.md) |
| 评测计量 | 完成度 ETA 投影 | EtaProjection——长任务剩余时长线性外推（rate=done/elapsed，ETA=remaining/rate，除不尽向上取整保守）+projectedTotalMillis 总时长投影，无速率基准（done=0/elapsed=0）-1 诚实哨兵——CI 进度条/带宽估计思想，「还要多久」不再人肉心算、多时点对比发现尾段漂移（spec 1834） | [spec 1834](docs/spec/1834-eta-projection.md) |
| 工程治理 | O 系 R36 对账轮 | 快照补登前置第六例行（R31–R35 五类型：多级缓存/对冲延迟/环形缓冲/续读令牌/ETA 投影）+ 全仓 clean verify 一次过绿 + 六波节奏稳定入档（spec 1835） | [spec 1835](docs/spec/1835-o-r36-reconciliation.md) |
| 溢出保护 | 库存周转读面 | TurnoverReadout——库存活性两读数：周转次数 turns（消费/库存，无库存 -1 哨兵、零消费 0=死库存）+耗尽视界 depletionHorizonMillis（库存/速率向上取整，零速率 -1）——供应链库存周转思想，周转贴地=TTL 激进、视界短=预热启动（spec 1836） | [spec 1836](docs/spec/1836-turnover-readout.md) |
| 成本治理 | 失败域配额 | FailureDomainQuota——预算按失败域分桶+全局保留兜底：admit 三态 FROM_BUCKET/BORROW_RESERVE/DENY+census 域普查（atCap/borrowing/tightest 最紧域/保留利用率）——k8s failure-domain/供应链分域备货思想，单域失败风暴不连坐健康域、备货总量封顶（spec 1837） | [spec 1837](docs/spec/1837-failure-domain-quota.md) |
| 安全治理 | 密钥轮换重叠窗 | RotationOverlapWindow——凭据代际三态判（CURRENT 当前/GRACE 重叠窗内旧代仍有效——在飞数据兼容，含边界/EXPIRED 窗尽失效）+未来代 fail-fast+census 清扫进度——TLS 证书轮换/Vault grace 思想，无重叠窗的切换=旧钥数据瞬间全废（spec 1838） | [spec 1838](docs/spec/1838-rotation-overlap-window.md) |
| 恢复韧性 | 反熵分歧账 | AntiEntropyDivergence——主副本键×版本比对四桶（onlyInPrimary 副本丢写/onlyInReplica 主被清/versionMismatch 冲突解候选/matched）+repairWorkload 三型合计+matchedRatio——Cassandra/Dynamo anti-entropy repair 思想，三型分歧修复动作不同分开数才排得了优先级（spec 1839） | [spec 1839](docs/spec/1839-anti-entropy-divergence.md) |
| 并发治理 | 向量时钟偏序比较 | VectorClockOrder——无中心时钟因果三态判（BEFORE/AFTER 各分量≤且至少一严格小/CONCURRENT 互相各领——冲突解判定前提），缺席分量按 0 稀疏合法、相等退化「不后于」——Dynamo 向量时钟/Lamport happens-before 思想，墙钟偏移下真冲突与因果先后可分（spec 1840） | [spec 1840](docs/spec/1840-vector-clock-order.md) |
| 工程治理 | O 系 R42 对账轮 | 快照补登前置第七例行（R37–R41 五类型：库存周转/失败域配额/轮换重叠窗/反熵分歧/向量时钟）+ 全仓 clean verify + 七波节奏稳定 + 共享检出特性入档（并行 push 携带本地提交）（spec 1841） | [spec 1841](docs/spec/1841-o-r42-reconciliation.md) |
| 护栏治理 | 隔离区普查 | QuarantineCensus——「放/杀」两极间的暂存待审积压读数：pendingReview/reviewedCount 分账+oldestPendingAge 待审最老旧（-1 哨兵）+pendingRatio 拥堵度——邮件隔离区/恶意样本沙箱思想，疑似即杀误报无申诉、只放真阳逃逸（spec 1842） | [spec 1842](docs/spec/1842-quarantine-census.md) |
| 技能体系 | 技能依赖图审计 | SkillDependencyAudit——依赖图三病分诊：环（显式栈 DFS 首环路径入档——该断哪条边直接读出）/缺失依赖（悬空引用——补装或降级）/孤儿（无边关联——目录噪音该清）——npm/pip 依赖解析思想，装得齐≠依赖健康（spec 1843） | [spec 1843](docs/spec/1843-skill-dependency-audit.md) |
| MCP 体系 | 重连退避阶梯 | ReconnectBackoffLadder——断线重连三段式策略面：指数爬升（100×2→100/200/400/800）+封顶钳制（溢出安全触顶先判后乘）+verdict RETRY/GIVE_UP（边界含——僵尸重连不永生）——Resilience4j retry/libpq 重连惯例思想（spec 1844） | [spec 1844](docs/spec/1844-reconnect-backoff-ladder.md) |
| 工具执行 | argv 预算门 | ArgvBudgetGate——命令参数体量双闸：总量闸（全部参数连 NUL 字节账 vs ARG_MAX 量级预算）+单参闸（vs MAX_ARG_STRLEN 量级上限，先于总量判定诊断价值高）——Linux execve E2BIG 前移到参数校验层，巨型 argv 注入面封顶（spec 1845） | [spec 1845](docs/spec/1845-argv-budget-gate.md) |
| 文件系统 | 组提交账面 | GroupCommitAccounting——合并刷盘收益账：amortizationRatio 摊薄倍数+savedFlushes 省刷数（×单价=SSD 寿命节省）+savingsNanos 净省（可为负——不划算面诚实）+savingsRatio——MySQL group commit/PostgreSQL commit_delay 思想，组提交开不开由数说话（spec 1846） | [spec 1846](docs/spec/1846-group-commit-accounting.md) |
| 工程治理 | O 系 R48 对账轮 | 快照补登前置第八例行（R43–R47 五类型：隔离区普查/依赖图审计/重连阶梯/argv 预算门/组提交账面——guard/skills/mcp/tools/fs 五模块新拓）+ 全仓 clean verify + 八波节奏稳定（spec 1847） | [spec 1847](docs/spec/1847-o-r48-reconciliation.md) |
| 认知可观测 | Misra-Gries 频项素描 | MisraGriesSketch——流式 heavy hitters：k−1 计数器计满抵消归零淘汰，O(k) 内存单遍找一切 >N/k 频繁项，计数为下界（真实−估计 ≤ N/k，热点不漏报），确定性可回放——经典流式算法思想，对照 Count-Min 概率口径（spec 1848） | [spec 1848](docs/spec/1848-misra-gries-sketch.md) |
| 认知可观测 | 水库采样 | ReservoirSample——流长未知均匀无放回采样（Knuth 算法 R：前 k 入池、k/i 概率替换——终选概率恰 k/n 与到达序无关）+种子化 LCG 跨 JVM 重现可回放——诊断采样从「攒全量再随机/取前 N 到达序偏差」二选一变第三条路（spec 1849） | [spec 1849](docs/spec/1849-reservoir-sample.md) |
| 清理治理 | 墓碑占比读面 | TombstoneRatioReadout——软删除积累双账：占比（墓碑/全量）+readAmplification 读放大（1/(1−ratio)，占比 0.5 即 2 倍每读跳一墓碑）+shouldCompact 阈判定（边界含上）——LSM-Tree tombstone/Cassandra compaction 思想，「删了但没真删」账面化（spec 1850） | [spec 1850](docs/spec/1850-tombstone-ratio-readout.md) |
| 缓存基建 | 桶表容量阶梯 | BucketTableSizing——自建桶表容量三件套：suggestCapacity（⌈n/lf⌉ 向上取 2 的幂，位与取模基数）+verdict 扩容判定（装填度≥负载因子边界含——碰撞链超线性拐点前）+load 装填前瞻——HashMap 负载因子/2 的幂容量惯例（spec 1851） | [spec 1851](docs/spec/1851-bucket-table-sizing.md) |
| 评测计量 | 截尾均值 | TrimmedMean——排序双侧各截 ⌊n×f⌋ 再均的稳健中枢（f∈[0,0.5)，零截退化算术均，空表 -1 哨兵）——统计学 trimmed mean/体育评审惯例（去最高最低再平均）思想，30s 卡顿不再把 100ms 中枢拉到 5s、不似中位数丢序信息（spec 1852） | [spec 1852](docs/spec/1852-trimmed-mean.md) |
| 工程治理 | O 系 R54 对账轮 | 快照补登前置第九例行（R49–R53 五类型：Misra-Gries 素描/水库采样/墓碑占比/桶表容量/截尾均值）+ 全仓 clean verify + 九波节奏稳定 + 测试数据病理三连入档（期望值用代码算）（spec 1853） | [spec 1853](docs/spec/1853-o-r54-reconciliation.md) |
| 指标治理 | 双窗口漂移检测 | WindowShiftDetector——近期窗 vs 基线窗均值漂移双闸判定：绝对闸（量级重要吗）×相对闸（比例重要吗，零基线退化绝对口径）同过才漂，SHIFTED_UP 变差/SHIFTED_DOWN 变好方向分开——Netflix/SRE 双窗口异常检测惯例，单闸二难（小基数刷屏 vs 缓变漏报）（spec 1854） | [spec 1854](docs/spec/1854-window-shift-detector.md) |
| 评测计量 | 收藏家覆盖期望 | CouponCollectorProjection——均匀抽样见全 k 类期望轮数 k×H(k)（调和级数长尾：收齐 10 类期望 ~29 轮非 10 轮）+expectedRemaining 随进度余轮递减——概率论 coupon collector 思想，覆盖测试轮数预算从拍到有期望依据（spec 1855） | [spec 1855](docs/spec/1855-coupon-collector-projection.md) |
| 指标治理 | 利特尔法则一致性审计 | LittlesLawAudit——跨指标互证：impliedConcurrency(λ×W 毫秒换算内置)+consistency 容差判定（|L−λW|≤tol×max(|λW|,1) 零基线退化）——排队论 Little's Law 思想，稳态下并发/到达率/逗留必然互证，偏差即仪表失真或稳态破（spec 1856） | [spec 1856](docs/spec/1856-littles-law-audit.md) |
| 工具执行 | 保工作性审计 | WorkConservationAudit——多队列调度浪费审计：逐时隙判违例（存在积压队列且有配额无积压的闲置队列）+violationRatio 违例率+wasteCoverageRatio 浪费覆盖比（闲置/积压，≥1 即重分配可救纯浪费）——调度理论 work conservation（WFQ/DRR 核心性质）思想，不公平可谈、不保工作不可恕（spec 1857） | [spec 1857](docs/spec/1857-work-conservation-audit.md) |
| 缓存基建 | 频次衰减竞速 | FrequencyDecay——访问计数周期减半衰减（老热点自然退烧）+overtakePeriod 新热点顶替周期（命中×t>衰减值最小 t——缓存自适应性读数：过长=老赖着、过短=抖动）——Redis LFU counter decay 思想，确定性纯数学（spec 1858） | [spec 1858](docs/spec/1858-frequency-decay.md) |
| 工程治理 | O 系 R60 对账轮 | 快照补登前置第十例行（R55–R59 五类型：双窗漂移/收藏家期望/利特尔互证/保工作性/频次衰减）+ 全仓 clean verify + 十波节奏稳定 + 40% 里程碑（spec 1859） | [spec 1859](docs/spec/1859-o-r60-reconciliation.md) |
| 指标治理 | 对数分桶直方图 | LogBucketHistogram——对数指数桶（桶 k 覆盖 [γ^k,γ^(k+1))，桶内相对差 ≤ γ−1）+quantile 桶序累计取几何中点，误差界随返回值声明（γ=1.25 即 ±12.5%）——HdrHistogram/DDSketch 思想，O(n) 一遍分位数免全排序、高段不失真（spec 1860） | [spec 1860](docs/spec/1860-log-bucket-histogram.md) |
| 事务语义 | 写偏斜检测 | WriteSkewDetector——快照隔离组合不变量风险：skewRisk（双方读写非空 且 读集相交 且 写集不相交——写冲突检测拦不住）+scan 全对扫描风险对清单——数据库 write skew 异象（经典「值班医生」反例）思想，风险对直接映射冲突桌（spec 1861） | [spec 1861](docs/spec/1861-write-skew-detector.md) |
| 缓存基建 | Rendezvous 哈希 | RendezvousHashing——键×节点确定性评分取最高的分片归属（HRW）：节点增删只影响原属它的 1/n 键（最小迁移数学保证），免一致性哈希环虚节点与环管理，无随机数可回放——Highest Random Weight 思想（spec 1862） | [spec 1862](docs/spec/1862-rendezvous-hashing.md) |
| 护栏治理 | 凭据强度计 | CredentialStrengthMeter——已知凭据弱度评估：字符四类计数（小写/大写/数字/符号）×长度双条件阶梯（3 类×16 长 STRONG/3 类×12 长 FAIR/否则 WEAK）——NIST SP 800-63 长度优先+强度计惯例思想，与 SecretScanner（文本检测）互补成防泄漏+防弱钥对（spec 1863） | [spec 1863](docs/spec/1863-credential-strength-meter.md) |
| 事件投递 | 可见性超时账 | VisibilityTimeoutAccounting——「至少一次」投递三段语义：取走即隐藏（防重复消费）+超时未确认重回队列（消费方死消息不丢，边界含上）+重投穷尽进死信（毒消息不死循环）+census 三段普查与最老在飞龄——AWS SQS visibility timeout 思想（spec 1864） | [spec 1864](docs/spec/1864-visibility-timeout-accounting.md) |
| 工程治理 | O 系 R66 对账轮 | 快照补登前置第十一例行（R61–R65 五类型：对数分桶/写偏斜/Rendezvous 哈希/凭据强度/可见性超时）+ 全仓 clean verify + R63 号漂移被对账门当场拦截入档（设计生效第二次活证）+ 显式退出码流程修正首次全程应用（spec 1865） | [spec 1865](docs/spec/1865-o-r66-reconciliation.md) |
| 模型韧性 | 缓存牺牲率 | CacheSacrificeRatio——插入驱逐比（牺牲率：全联理想 0、容量不足攀升）×命中率双条件颠簸分诊（牺牲高且命中低=白忙该扩容，任一哨兵无据不定罪）——体系结构缓存分析惯例（sacrifice ratio/thrashing）思想，满缓存换血与真颠簸不再同形（spec 1866） | [spec 1866](docs/spec/1866-cache-sacrifice-ratio.md) |
| 工具执行 | 关键路径长度 | CriticalPathLength——并行 DAG 最长加权路径（Kahn 拓扑+EF DP：EF[v]=dur[v]+max(EF[pred])）=总时长下界+terminalTask 终点直读——项目管理 CPM 思想，缩非关键任务白花力气、关键任务延一秒总长延一秒（spec 1867） | [spec 1867](docs/spec/1867-critical-path-length.md) |
| 工具执行 | 区间合并与空闲缝隙 | IntervalSchedule——占用段合并（重叠/相邻/嵌套归一取 max end，乱序容忍）+窗口内空闲缝（首前/区间间/尾后，越界裁剪）——日历调度 busy/free 惯例思想，总忙时直读、候选档期不再人肉拼（spec 1868） | [spec 1868](docs/spec/1868-interval-schedule.md) |
| 指标治理 | 流式中位数保持器 | MedianKeeper——双堆对半结构（大顶+小顶，不变量：小半顶≤大半顶、差≤1）：add O(log n) 平衡、median O(1)（奇小半顶/偶双顶均值，空 -1 哨兵）——双堆经典结构思想，观测流当前中枢不再全量重排、长尾不拉走（spec 1869） | [spec 1869](docs/spec/1869-median-keeper.md) |
| 工具执行 | 固定间隔下次触发 | NextFireSchedule——触发点钉在 epoch+k×interval 网格（停机不漂移：重启后 now=250 间隔 100 下次 300 非 350）+missedFires (lastAcked,now] 格点数补账显式——crontab/systemd timer 网格语义思想（spec 1870） | [spec 1870](docs/spec/1870-next-fire-schedule.md) |
| 工程治理 | O 系 R72 对账轮 | 快照补登前置第十二例行（R67–R71 五类型：牺牲率/关键路径/区间调度/流式中位/网格触发）+ 全仓 clean verify + 十二波节奏稳定 + 逼近半程（spec 1871） | [spec 1871](docs/spec/1871-o-r72-reconciliation.md) |

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
