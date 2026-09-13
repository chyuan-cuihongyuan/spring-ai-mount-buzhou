# Wayfinder Map — I 会话 900 系：借鉴高价值开源项目的 100 轮自迭代（effort #900 总图）

> **I 会话**（2026-09-13 启动）：继 C（300 系）/ D（400 系）/ E（500 系）/ F（600 系）/ G（700 系）/ H（800 系）之后的第七条自迭代线。
> **号段裁决**：I 会话占用 spec **900–999**、票 **T1251–T1450**（每轮 2 张：shape + verify）、impl **653–752**（每轮 1 片）。
> **让号记录**：I 会话最初按 800 系开工（T1051–T1052 / spec 800 / impl 553），首个提交后 fetch 发现 **H 会话已立桩占坑 800 系**（progress-effort-800.md：specs 800–849 / T1101–T1200 / impl 553–602，PR #22 已入 main）且对方 G 双线已用至 T1099——遵「号段声明先行」制度让出 800 系，产物全部改号为 900 系（本 map 即改号后总图）。
> 用户常设授权（沿 F/G/H 会话）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。
> 起点：origin/main @ 46f6b4a6（H 会话立桩后）。

## Destination

**100 个完整自迭代 loop 全部完成**——每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或 opt-in 的机制改进；100 轮全部 Conventional Commits 提交并推送 GitHub，终验全仓 `mvn -B -ntp clean verify` 绿（16 模块 + JaCoCo ≥70% + enforcer + 快照门 + SpecCoverage）。

## Notes

- 每轮固定四步产物：决策票（同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<9NN>-<slug>.md`（+README 生产级纵深表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级定向测试必须绿；周期性（每 10 轮）+ 收口轮跑全仓 `mvn -B -ntp clean verify`；每轮 commit 后 push origin main。
- 选题纪律：每轮开工先缺口核查（grep spec+代码，**含 H 会话 800 系已开工主题**——对方 progress-effort-800.md 主题池 R1–R50 与备选池 S1–S10 一并回避），已实现则台账记 `ruled-out` 顺延。
- 代码规范：无魔法数字（static final 常量或配置）、SLF4J 占位符、record/sealed 优先；新公共类入 `api` 包需 Javadoc + API 快照随轮再生（`-Dbuzhou.api-snapshot.regenerate=true`）。
- 模块依赖边界不变：feature 模块互不直接依赖，跨机制协作走 core 事件总线或 core SPI。

## Decisions so far

- [事件丢弃按原因分类读面](../tickets/T1251-event-drop-breakdown-shape.md) — EventDropBreakdown（drop-oldest/block-timeout/closed-undelivered 等分桶）+ eventDropBreakdown() 读面（SYNC empty 与 eventBusStats 同构）；ΣbyReason 守恒 == dropped；EventBusStats 原样不动（Sentry discarded events）。
- [评估失败率中途剪枝](../tickets/T1253-eval-prune-shape.md) — EvalPrunePolicy（minItems 观察窗 + failRateThreshold）opt-in；仅串行路径生效恰停剩余项 pruned；指标 buzhou.eval.run.pruned + WARN（Optuna pruner；并行路径 invokeAll 无低成本中途取消——诚实入档不伪实现）。
- [pass@k 无偏估计器](../tickets/T1255-pass-at-k-shape.md) — EvalPassAtK 连乘无偏公式（HumanEval §2.1，无组合数溢出）+ aggregate 逐项平均；纯函数不触 store，k 次采样留宿主（spec 513 边界一致）。
- [bootstrap 均值置信区间](../tickets/T1257-bootstrap-ci-shape.md) — EvalScoreAnalytics.bootstrapMeanInterval（Efron percentile + SplittableRandom 固定 seed 可复现）+ MeanInterval record；扩同类不炸类，嵌套 record 不进快照面（spec 903）。
- [导入审计与严格模式](../tickets/T1259-import-strict-shape.md) — SessionExportAudit.audit（未知顶层字段/缺失推荐字段只读报告，空消息数组合法不算缺失）+ fromJsonStrict opt-in 拒绝（pg_restore --exit-on-error / protobuf unknown fields）。
- [健康聚合评分读面](../tickets/T1261-health-score-shape.md) — BuzhouHealthScore（UP=100/UNKNOWN=50/DOWN=0 算术平均 + 分档常量 HEALTHY_FLOOR=90/DEGRADED_FLOOR=70 + ScoreReport 含 DOWN 清单）；纯函数端点接线留装配轮（K8s probe aggregate）。
- [工具耗时火焰图数据面](../tickets/T1263-flame-timing-shape.md) — ToolGraphAnalyzer.timings（TOOL 子集 parentSpanId 树 + self/cumulative 分解 + 环防护 + RUNNING 计 0 + 稳定排序）+ ToolTimingProfile（flamegraph；与 core ToolTimingAggregator spec 700 热路径互补的离线全量面）。
- [outbox 投递批量 AIMD 自适应](../tickets/T1265-aimd-batch-shape.md) — WebhookEventForwarder.opt-in 自适应批量（adjustedBatch 纯函数：全成功 +1 / 失败 ÷2 / 夹取 [1,64] / defer 中性）+ currentBatchSize 读数（TCP AIMD；默认关逐位不变）。
- [k 次 run 稳定性矩阵](../tickets/T1267-k-stability-shape.md) — EvalFlakinessDetector.analyzeK（跨 run 逐项对齐 + 红绿一致率 + 漂移不入分母 + runId 重复 fail-fast）+ KStabilityReport/KItemVerdict（spec 513 k 泛化留位；与 pass@k 902 概率口径互补）。
- [评估分数分位数读面](../tickets/T1269-percentiles-shape.md) — EvalScoreAnalytics.percentiles（R-7 线性插值 + LinkedHashMap 保序 + NaN fail-fast）；与 bootstrap 903 / passesAtThresholds 747 三面互补。
- [AIMD 自适应批量 yml 装配](../tickets/T1271-aimd-yml-shape.md) — webhookEventForwarder bean 点 Binder 直读 buzhou.webhook.adaptive-batch（缺省 false 逐字节不变）→ setter——spec 105 include-types 同法（D/G 装配轮模式）。
- [JCS 规范化内容指纹](../tickets/T1273-jcs-fingerprint-shape.md) — canonicalContentFingerprint（递归 Map 键排序规范化 + sha256-j: 新前缀防混用；数字文本原样诚实入档）——RFC 8785 JCS 思想，指纹绑定内容而非键序。

## 100 轮台账

| # | 主题 | 借鉴源 | 票 | impl | spec | 状态 |
|---|------|--------|----|------|------|------|
| 1 | 事件丢弃按原因分类读面（原列「outbox 积压深度健康面」ruled-out——spec 135 已覆盖） | Sentry discarded events | T1251–T1252 | 653 | 900 | ✅ |
| 2 | 评估失败率中途剪枝 | Optuna pruner | T1253–T1254 | 654 | 901 | ✅ |
| 3 | pass@k 无偏估计器 | HumanEval/Codex §2.1 | T1255–T1256 | 655 | 902 | ✅ |
| 4 | bootstrap 均值置信区间 | Efron bootstrap percentile | T1257–T1258 | 656 | 903 | ✅ |
| 5 | 导入审计与严格模式 | pg_restore --exit-on-error / protobuf unknown fields | T1259–T1260 | 657 | 904 | ✅ |
| 6 | 健康聚合评分读面（原列候选 prompt 前缀缓存统计 ruled-out——PromptPrefixCache.Stats 已覆盖；技能使用统计 ruled-out——SkillUsageStats 已存在） | K8s probe aggregate | T1261–T1262 | 658 | 905 | ✅ |
| 7 | 工具耗时火焰图数据面（reask 上限 ruled-out——BoundedToolCallingAdvisor×ToolRetryPolicy 已覆盖；EWMA ruled-out——FallbackLatencyTracker 已覆盖；归档回读 ruled-out——SessionArchiver.verify 已覆盖；配额预测 ruled-out——CostForecast 已覆盖） | brendangregg/FlameGraph | T1263–T1264 | 659 | 906 | ✅ README 行欠账 |
| 8 | outbox 投递批量 AIMD 自适应（指标标签值集守卫 ruled-out——TagCardinalityGuard spec 132 运行时守卫已覆盖） | TCP AIMD（RFC 5681） | T1265–T1266 | 660 | 907 | ✅ README 行欠账 |
| 9 | k 次 run 稳定性矩阵（LRU-K ruled-out——InMemoryMessageStore noeviction 语义无驱逐落点；游标稳定性 ruled-out——spec 631 keyset 已覆盖；影子分叉报告 ruled-out——ShadowProbe.Snapshot 聚合面已覆盖） | Google flaky-tests / k 次 A/A | T1267–T1268 | 661 | 908 | ✅ README 行欠账（906/907/908 三行，README 竞争解除后一并补） |
| 10 | 评估分数分位数读面（R-7 口径）+ 周期全仓 verify（中断：同工作区他方未跟踪半成品 SpotlightingTest 红灯污染 core——非本会话回归，全量留他方静止窗口复跑） | numpy percentile R-7 | T1269–T1270 | 662 | 909 | ✅ README 行欠账（906–909 四行） |
| 11 | AIMD 自适应批量 yml 装配（装配轮；PriorityLane aging ruled-out——超时让位语义已替代且 aging 与不剥夺模型叠加复杂） | D/G 装配轮模式（spec 105 同法） | T1271–T1272 | 663 | 910 | ✅ README 行欠账（906–910 五行） |
| 12 | JCS 规范化内容指纹 | RFC 8785 JCS | T1273–T1274 | 664 | 911 | ✅ README 行欠账（906–911 六行） |
| 13 | （开工时按缺口核查选题，候选见下） | — | T1275–T1276 | 665 | 912 |  |

（2–100 号段开工时逐轮选题：从候选池选取 + 缺口核查通过后填入本表。**工作区并发警示**：同机另有会话共享工作区（1000 系 / T1451+ / impl 753+）——每轮提交必须精确路径 add，勿 git add -A；README 行受其未提交 spec 引用阻塞时欠账下轮补。候选池已预筛一轮——下列主题经预核查 **ruled-out** 不再入池：outbox 积压深度（spec 135）、重试预算（spec 348 RetryBudgetHealth）、webhook HMAC 签名（WebhookSignatures）、技能目录指纹（SkillCatalogFingerprint）、审计链 Merkle 根（spec 404）、健康段属性截断（BuzhouHealth 有界详情纪律已覆盖）、响应缓存统计水位（ResponseCacheStore hit/miss/evicted 已覆盖）、事件丢弃总量计数（EventBusStats.dropped，spec 13）、fail2ban 累进封禁（H 会话 R1 已认领——回避）。）

## 候选池（开工选题用；每轮缺口核查通过后转入台账）

- 评估中途剪枝（Optuna pruner）／pass@k 无偏指标（HumanEval）／bootstrap 评估置信区间（Efron）／评估分组汇总（lm-eval-harness）
- 会话内存占用估算（Redis MEMORY USAGE）／压缩驱逐 LRU-K 候选序（PostgreSQL buffer）／租户窗口用量读面（Stripe usage records）
- prompt 前缀缓存命中观测（Anthropic prompt caching）／模型抢占语义观测（vLLM preemption）／校验失败重问上限（guardrails-ai reask）／断言反馈回路（DSPy assertions）
- 工具结果过期引用读面（HTTP Cache-Control）／会话导出滚动校验和增量（rsync rolling checksum）／store 扫描游标稳定性（Redis SCAN 语义）
- outbox 批量 AIMD 自适应（TCP 拥塞控制）／事件重放序号缺口检测（Kafka log gap）／健康加权评分读面（K8s probe aggregate）／影子分叉对比报告（shadow testing）
- 租约续约活性观测（client-go leaderelection）／配额预测读面（Prometheus predict_linear）／压缩策略建议读面（pg advisor）／技能使用统计读面（npm downloads）
- 拒绝趋势分桶读面（fail2ban 时序化——注意与 H 会话 R1 自动封禁错位：本仓已有拒绝日志 spec 709，此候选仅做趋势读面，开工再核）／导入严格校验模式（pg_restore）／store fsck 修复建议面（git fsck）／事件水位线读面（Flink watermark）／软截止分层提醒（K8s graceful period）／归档验证回读面（backup verify-restore）／路由健康 EWMA 平滑（Envoy outlier EWMA）／指标标签值集守卫（prometheus label 约束）

## Out of scope

- 不引入任何新的第三方运行时依赖（BOM/enforcer 收口优先；测试域新增依赖亦回避）。
- 不重构模块依赖星形拓扑、不改公共 API 破坏性签名（0.x 语义内也尽量避免）。
- 不与已收口/进行中会话主题撞车：每轮缺口核查含 D/E/F/G/H 已落地内容（H 会话主题池与备选池一并回避）。
- 不做 FPE/FF1 等需密码学依赖的主题（G 会话已判定 out of scope，维持）。
