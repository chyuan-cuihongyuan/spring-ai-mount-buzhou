# Wayfinder Map — J 会话 1000 系：借鉴高价值开源项目的 150 轮自迭代（effort #1000 总图）

> **J 会话**（2026-09-14 启动）：继 C（300）/ D（400）/ E（500）/ F（600）/ G（700）/ H（800）/ I（900）之后的第八条自迭代线。
> **号段裁决（号段声明先行）**：J 会话占用 spec **1000–1149**、票 **T1451–T1750**（每轮 2 张：shape + verify）、impl **753–902**（每轮 1 片）、efforts **#1000–#1149**。本文件即占坑声明，先于 R1 动工提交入 main。
> **并存声明**：I 会话（effort-900.md 正序段）与本线**互不触碰对方 map 文件**；每轮开工先 `git fetch` 双查 main，push 被拒即 `pull --rebase` 后重推。
> 用户常设授权（沿 F/G/H/I 会话）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。

## Destination

**150 个完整自迭代 loop 全部完成**——每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或 opt-in 的机制改进；每轮 Conventional Commits 提交并推送 GitHub；周期性（每 10 轮）+ 收口轮跑全仓 `mvn -B -ntp clean verify` 绿。

## Notes

- 每轮固定四步产物：决策票（同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<1NNN>-<slug>.md`（+README 生产级纵深表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级定向测试必须绿；新公共 api 面类型入轮再生 API 快照（`-Dbuzhou.api-snapshot.regenerate=true`）；每轮 commit 后 push origin main。
- 选题纪律：每轮开工先缺口核查（grep spec+代码，含 H 会话 800 系池 R1–R50+S1–S10 与 **I 会话 900 系候选池一并回避**），已实现则台账记 `ruled-out` 顺延；代码库 250+ effort 高度饱和，排重 grep 必须 `-i` 且按类名后缀查。
- 代码规范：无魔法数字（static final 常量或配置）、SLF4J 占位符（core 内可用 System.Logger 先例）、record/sealed 优先；进程级静态读面须配 reset 注入点与注释说明（BuzhouMetricsHolder 先例）。
- 模块依赖边界不变：feature 模块互不直接依赖，跨机制协作走 core 事件总线或 core SPI。

## Decisions so far

- [工具策略匹配决策读面的形态裁决](../tickets/T1451-policy-match-decision-shape.md) — ToolPolicyMatchDecision（EXACT/GLOB/NONE + matchedKey）+ ToolPolicyMatchStats 快照（Σ守恒 == match 调用数，recent 有界环 32）；match 返回值逐位不变，stats()/resetStats() 读面（OPA decision log）。
- [慢调用榜读面的形态裁决](../tickets/T1453-slow-log-shape.md) — ToolSlowLog（Redis SLOWLOG：严格大于阈值入有界 FIFO 环 32、entries() 新→旧现场、configureThreshold/reset）；HookedToolCallback 与 timer 同点接线，只记名不记参（红线）；聚合面之外的单次现场。
- [Hook 链解析顺序快照读面的形态裁决](../tickets/T1455-hook-composition-shape.md) — ChainComposition（resolvedHookNames 派发序 + ghostDisabledNames 幽灵禁用集）composition() 构造期快照；拼错 disabled 名静默蒸发的显形，零行为变化（Kong plugin priority）。
- [策略层级归属读面的形态裁决](../tickets/T1457-policy-layer-attribution-shape.md) — PolicyLayerAttribution（DEFAULTS/YML/BINDING/ABSENT + value）+ LayeredPolicy.getAttributed 同序同判；get() 薄封装重构零行为变化；无计数器（无生产调用方不设假面）（spring config insights 层归因）。
- [维护窗历史读面的形态裁决](../tickets/T1459-maintenance-history-shape.md) — MaintenanceGate.HistoryEntry 闭窗历史环 16 新→旧（何时/为何/多久/refusals 按窗分账）+ history() 快照 + noteRefused 包内计数（Hook 同点补一行）；begin/end 对外语义不变（K8s cordon 事件史）。
- [插叙轮：SpecCoverage 门四位数扩容] — \d{1,3}→\d{1,4}（1000 系起四位数 spec 曾成门盲区）+ 补 I 会话 906–909 README 行；隔离 worktree reactor 联编双门验证绿——号段制跨入四位数后的门维护责任。
- [会话面包屑环形读面的形态裁决](../tickets/T1463-breadcrumbs-shape.md) — EventBreadcrumb（type+时刻，不记 payload 红线）+ 内部 BreadcrumbRing（32 新→旧）；deliverEvent 双模式共同漏斗一行记录；AgentSession.breadcrumbs() default 空表 + DefaultAgentSession 覆写（Sentry breadcrumbs）。
- [凭证租约生命周期计数读面的形态裁决](../tickets/T1465-lease-stats-shape.md) — SecretLeases 补 renew 轴（renewed/renewRejected：缺失拒与过期拒两路计数）+ SecretLeaseStats 五字段统一快照；既有三 getter 兼容保留——续租拒绝率=TTL 过短信号（Vault lease lifecycle）。
- [spill 回读命中率读面的形态裁决](../tickets/T1467-spill-onload-stats-shape.md) — SpillOnloadStats（attempts/loaded/failed 守恒）OnloadHook 回灌点计数；回读失败=spill 侵蚀信号，命中率消费方自算（PostgreSQL buffer hit-ratio）。首入 spill 模块。
- [J 系周期预检轮（R10）的范围裁决](../tickets/T1469-periodic-audit-shape.md) — 隔离 worktree 全仓 verify + 双门复跑 + 台账对账；三处主仓红收口（guard ToolDenialLog 保序、910–915 README 行、SessionExportDiff 快照行）；单模块跑两假红陷阱再证入档。
- [轮次时延分位数读面的形态裁决](../tickets/T1471-turn-percentiles-shape.md) — TurnLatencyPercentiles（count/p50/p95/max）+ TurnTimingHook.percentiles 对既有 64 样本窗 R-7 插值（纯函数直测）；不改正史 record TurnStats——补 spec 191 自己的 p95 用户故事（numpy percentile 同口径）。
- [spill 容量水位读面的形态裁决](../tickets/T1473-spill-usage-shape.md) — SpillUsage（totalBytes/entryCount）+ DiskSpillStore.usage() 与配额守卫同口径 walk（synchronized 同锁）；配额上限不入快照——调用方自持配置（Redis INFO memory / pg_database_size）。
- [加密封存操作生命周期计数读面的形态裁决](../tickets/T1475-seal-stats-shape.md) — EncryptedSessionExport sealed/opened/openRejected 三计数（open 三条 DATA_CORRUPTION 拒绝路径全覆盖，异常照抛）+ 嵌套 SealStats + stats()；开失败率=密钥轮换错配第一信号（age/OpenSSL ops 实践）。
- [Hook Replace 载荷应用/丢弃计数读面的形态裁决](../tickets/T1477-replace-stats-shape.md) — applyReplace boolean 化 + replaceApplied/replaceDropped 实例计数与双 getter；类型不匹配载荷静默跳过的显形（分发行为逐位不变）；与 R3 幽灵禁用同族。
- [Spotlighting 应用与损坏计数读面的形态裁决](../tickets/T1479-spotlighting-stats-shape.md) — SpotlightingStats（wrapped/unwrapped/malformed 守恒）+ stats()/resetForTest()；含头但结构不完整的包裹原样放行的篡改/截断显形（安全控制覆盖率）；行为逐位不变。
- [超时覆盖命中读面的形态裁决](../tickets/T1481-timeout-override-stats-shape.md) — ToolTimeoutOverrideStats（lookups/hits/misses + hitsByPattern 播种全部配置模式，0 = 幽灵覆盖配置显形）；timeoutMillisFor 返回值逐位不变；顺带入档存量口径：Map.copyOf 不保「首中即胜」调用方顺序（feature-flag 评估计数思想）。
- [技能解析未命中计数读面的形态裁决](../tickets/T1483-skill-resolution-stats-shape.md) — SkillResolutionStats（loads/resolved/notFound 守恒）+ resolutionStats()；load-only 口径（清单枚举不计——防渲染流量污染幻觉信号）；模型幻觉技能名探测，与 I 池使用统计分轴（Berkeley function-calling leaderboard）。首入 skills 模块。
- [沙箱执行结果分桶读面的形态裁决](../tickets/T1485-sandbox-exec-stats-shape.md) — LimitedCommandSandbox 嵌套 ExecStats（executions/timeouts/outputTruncations，TIMEOUT/OUTPUT 两轴正交无加法守恒）+ stats()；击杀与截断从单次字段升为累计水位（Firejail/bubblewrap run stats）。首入 guard 模块。
- [taint 信息流控制生命周期计数读面的形态裁决](../tickets/T1487-taint-lifecycle-stats-shape.md) — TaintMarkStats（marksApplied/firstMarks）+ GateStats 四分桶（checked == trusted + approved + blocked 守恒）+ 双 stats()；打标/放行/拦截行为逐位不变（FIDES 判定分布显形）。首入 guard taint 包。
- [金丝雀生命周期计数读面的形态裁决](../tickets/T1489-canary-stats-shape.md) — CanaryGuardHook 嵌套 CanaryStats（planted 播撒幂等不重复计 / leaked 泄漏 / variantBlocked 变体拦截）+ stats()；泄漏与变体触发即间接注入在场的铁证（Thinkst Canary）。
- [密钥扫描计数读面的形态裁决](../tickets/T1497-secret-scan-stats-shape.md) — SecretScanner 嵌套 SecretScanStats（scanCalls/findings/redactions：findings 水位是泄漏趋势第一信号；空文本早返与幂等早返均不计）+ stats()；AWS 文档示例键测试样本分段拼接避免源码级自命中（Gitleaks findings）。
- [事实采集隔离硬化与计数读面的形态裁决](../tickets/T1491-fact-collector-isolation-shape.md) — FactCollectorHook 逐定义 try/catch 隔离（judge/save 异常不再炸 afterTool 链——监听器隔离惯例对齐）+ FactCollectionStats（saved/failures）+ stats()；本轮含行为改进（隔离语义）。
- [HITL 审批操作分布读面的形态裁决](../tickets/T1493-auth-operation-stats-shape.md) — GuardAuthApi 嵌套 AuthOperationStats（approved/rejected/revoked 三计数）+ stats()；事件流之外的进程内聚合水位（与 R19 门判定分轴——台账操作轴）。
- [加密消息存储操作计数读面的形态裁决](../tickets/T1495-crypto-store-stats-shape.md) — EncryptingMessageStore 嵌套 CryptoStoreStats（encrypted/decrypted/passthrough 双向透传分计）+ stats()；解密失败照抛不计（完整性优先语义不变）——信封加密 ops 可视性。首入 crypto 包。

## 150 轮台账

| # | 主题 | 借鉴源 | 票 | impl | spec | 状态 |
|---|------|--------|----|------|------|------|
| 1 | 工具策略匹配决策读面（EXACT/GLOB/未命中分类 + 有界最近决策环） | OPA decision log | T1451–T1452 | 753 | 1000 | ✅ |
| 2 | 工具慢调用榜读面（严格阈值 + 有界 FIFO 环 + 单次现场） | Redis SLOWLOG | T1453–T1454 | 754 | 1001 | ✅ |
| 3 | Hook 链解析顺序快照读面（派发序显形 + 幽灵禁用检测） | Kong plugin priority | T1455–T1456 | 755 | 1002 | ✅ |
| 4 | 策略层级归属读面（DEFAULTS/YML/BINDING/ABSENT 归因） | spring config insights | T1457–T1458 | 756 | 1003 | ✅ |
| 5 | 维护窗历史读面（闭窗环 + 窗内拒绝分账） | K8s cordon 事件史 | T1459–T1460 | 757 | 1004 | ✅ |
| 6 | 工具在飞并发水位读面（current/peak 双水位 + 恰一次租约） | Go NumGoroutine/Hystrix | T1461–T1462 | 758 | 1005 | ✅ |
| 7 | 会话面包屑环形读面（时间线尾部环 + 双模式漏斗） | Sentry breadcrumbs | T1463–T1464 | 759 | 1006 | ✅ |
| 8 | 凭证租约生命周期计数读面（补 renew 轴 + 统一快照） | Vault lease lifecycle | T1465–T1466 | 760 | 1007 | ✅ |
| 9 | spill 回读命中率读面（attempts/loaded/failed 守恒） | PostgreSQL buffer hit-ratio | T1467–T1468 | 761 | 1008 | ✅ |
| 10 | 周期预检（全仓 verify + 双门 + 三处主仓红收口） | G/H 系收口预检惯例 | T1469–T1470 | 762 | 1009 | ✅ |
| 11 | 轮次时延分位数读面（R-7 插值 p50/p95） | numpy percentile | T1471–T1472 | 763 | 1010 | ✅ |
| 12 | spill 容量水位读面（totalBytes/entryCount 快照） | Redis INFO memory | T1473–T1474 | 764 | 1011 | ✅ |
| 13 | 加密封存操作生命周期计数读面（sealed/opened/openRejected） | age/OpenSSL ops | T1475–T1476 | 765 | 1012 | ✅ |
| 14 | Hook Replace 载荷应用/丢弃计数读面（幽灵载荷显形） | 静默蒸发显形谱系 | T1477–T1478 | 766 | 1013 | ✅ |
| 15 | Spotlighting 应用与损坏计数读面（防御覆盖 + 篡改显形） | 静默蒸发显形谱系 | T1479–T1480 | 767 | 1014 | ✅ |
| 16 | 超时覆盖命中读面（幽灵覆盖配置显形） | feature-flag 评估计数 | T1481–T1482 | 768 | 1015 | ✅ |
| 17 | 技能解析未命中计数读面（幻觉技能名探测） | Berkeley function-calling | T1483–T1484 | 769 | 1016 | ✅ |
| 18 | 沙箱执行结果分桶读面（TIMEOUT/OUTPUT 两轴正交） | Firejail run stats | T1485–T1486 | 770 | 1017 | ✅ |
| 19 | taint 信息流控制生命周期计数读面（打标/写门四分桶） | FIDES 判定分布显形 | T1487–T1488 | 771 | 1018 | ✅ |
| 20 | 金丝雀生命周期计数读面（播撒/泄漏/变体拦截） | Thinkst Canary | T1489–T1490 | 772 | 1019 | ✅ |
| 21 | 事实采集隔离硬化与计数读面（judge/save 隔离 + saved/failures） | 监听器隔离惯例推广 | T1491–T1492 | 773 | 1020 | ✅ |
| 22 | HITL 审批操作分布读面（approved/rejected/revoked） | 审批聚合视图 | T1493–T1494 | 774 | 1021 | ✅ |
| 23 | 加密消息存储操作计数读面（encrypted/decrypted/passthrough） | 信封加密 ops 可视性 | T1495–T1496 | 775 | 1022 | ✅ |
| 24 | 密钥扫描计数读面（scanCalls/findings/redactions） | Gitleaks findings | T1497–T1498 | 776 | 1023 | ✅ |
| 25 | （开工时按缺口核查选题，候选见下） | — | T1499–T1500 | 777 | 1024 |  |

（5–150 号段开工时逐轮选题：从候选池选取 + 缺口核查通过后填入本表；候选池仅预筛一轮，池尽时续筛。开工核查即 ruled-out：span 状态分布（spec 543 已收）、加密版本分布（v1 一统无分布价值）、fork 谱系深度（spec 711 已带深度/环读面）、SecretLeases 计数（issued/expired/revoked 已存在）、排空耗时（动同步闭锁路径风险收益比差）。）

## 候选池（开工选题用；每轮缺口核查通过后转入台账；撞 H/I 池或已落地能力即弃）

- Hook 链解析顺序快照（Kong plugin priority）／慢调用 Top-K 榜读面（Redis SLOWLOG，基于 spec 108 timer 之上的有界环）／会话面包屑环形读面（Sentry breadcrumbs）／虚拟线程在飞水位读面（Go runtime NumGoroutine）→ exec/hook/session
- Little's Law 在飞守恒一致性读面（queueing theory）／span 状态分布读面（OTel status codes）／排空耗时分布读面（SIGTERM drain，SessionDrainCoordinator）／会话 fork 谱系深度读面（git fork depth，ForkLineageWalker）
- 审计 vs 强制双模式计数（Kyverno audit mode）／会话恢复完整性统计读面（CRIU restore stats）／SecretLeases 续期批量读面（Vault lease renew）／事件幂等序号重置计数（Kafka idempotent producer）
- 正则回溯超时守卫读面（RE2 linear-time，StreamTextFilter）／数据集版本演进 diff 读面（dvc diff）／spill 回读命中率读面（cache hit ratio）／事件 TTL 过期计数读面（RabbitMQ message-expired）
- 并行工具扇出/回收对称读面（fan-out symmetry）／四层策略覆盖层级命中分布读面（spring config insights layer attribution）／原子工具补偿执行读面（saga compensation）／熔断器 time-in-state 分布读面（Resilience4j state duration）
- 导出加密版本分布读面（encryption-at-rest mix，EncryptedSessionExport）／判校分歧矩阵读面（judge disagreement matrix，开工先核 JudgeAgreement 缺口）／压缩前后 token 比读面（compaction ratio，开工先核 H R17 边界）

## 预排除（开工核查即 ruled-out 的近似撞题）

- 429 Retry-After 尊重回退：resilience `ProviderRateLimitSignals` 已解析限流头（含 flexible 解析），邻域已被占。
- 工具时长 timer／P95：spec 108 已收。策略豁免过期：H 池 R23 已占。事件载荷大小：observability `EventPayloadSizeAudit` 已存在。
- I 系候选池全量（bootstrap CI、MEMORY USAGE、LRU-K、prompt 前缀缓存、AIMD、watermark、EWMA 等）与 H 系池 R1–R50+S1–S10 全量回避。

## Out of scope

- 不引入任何新的第三方运行时依赖（BOM/enforcer 收口优先；测试域新增依赖亦回避）。
- 不重构模块依赖星形拓扑、不改公共 API 破坏性签名（0.x 语义内也尽量避免）。
- 不做 FPE/FF1 等需密码学依赖的主题（G 会话判定维持）；不触碰 I 会话 effort-900.md 正序号段产物（specs 90x / T125x+ / impl 65x）。
