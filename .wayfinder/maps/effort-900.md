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

## 100 轮台账

| # | 主题 | 借鉴源 | 票 | impl | spec | 状态 |
|---|------|--------|----|------|------|------|
| 1 | 事件丢弃按原因分类读面（原列「outbox 积压深度健康面」ruled-out——spec 135 已覆盖） | Sentry discarded events | T1251–T1252 | 653 | 900 | ✅ |
| 2 | （开工时按缺口核查选题，候选见下） | — | T1253–T1254 | 654 | 901 |  |

（2–100 号段开工时逐轮选题：从候选池选取 + 缺口核查通过后填入本表；候选池已预筛一轮——下列主题经预核查 **ruled-out** 不再入池：outbox 积压深度（spec 135）、重试预算（spec 348 RetryBudgetHealth）、webhook HMAC 签名（WebhookSignatures）、技能目录指纹（SkillCatalogFingerprint）、审计链 Merkle 根（spec 404）、健康段属性截断（BuzhouHealth 有界详情纪律已覆盖）、响应缓存统计水位（ResponseCacheStore hit/miss/evicted 已覆盖）、事件丢弃总量计数（EventBusStats.dropped，spec 13）、fail2ban 累进封禁（H 会话 R1 已认领——回避）。）

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
