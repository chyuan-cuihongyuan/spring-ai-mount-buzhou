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

（每轮解决后在决策票记录 Resolution，并在 150 轮台账转 ✅。）

## 150 轮台账

| # | 主题 | 借鉴源 | 票 | impl | spec | 状态 |
|---|------|--------|----|------|------|------|
| 1 | 工具策略匹配决策读面（EXACT/GLOB/未命中分类 + 有界最近决策环） | OPA decision log | T1451–T1452 | 753 | 1000 |  |
| 2 | （开工时按缺口核查选题，候选见下） | — | T1453–T1454 | 754 | 1001 |  |

（2–150 号段开工时逐轮选题：从候选池选取 + 缺口核查通过后填入本表；候选池仅预筛一轮，池尽时续筛。）

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
