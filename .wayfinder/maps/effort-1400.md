# Wayfinder Map — L 会话 1400 系：借鉴高价值开源项目的 50 轮自迭代（effort #1400 总图）

> **L 会话**（2026-09-14 启动）：继 C（300）/ D（400）/ E（500）/ F（600）/ G（700）/ H（800）/ I（900）/ J（1000）/ K（1200）之后的第十条自迭代线。
> **号段裁决（号段声明先行）**：L 会话占用 spec **1400–1449**、票 **T2101–T2200**（每轮 2 张：shape + verify）、impl **1053–1102**（每轮 1 片）、efforts **#1400–#1449**。本文件 + progress-effort-1400.md 即占坑声明，先于 R1 动工提交入 main。
> **开工前双查**：fetch 后实查 origin/main @ 0d1de35c——I 会话 900 系进行中（spec 至 952+）、J 会话 1000 系进行中（spec 至 1047，声明 1000–1149）、K 会话 1200 系进行中（测试补全覆盖线，声明 1200–1349）；**1400 系完全空闲**，无任何 spec/map/ticket 占用。
> **并存声明**：I/J/K 会话 map 文件与本线**互不触碰**；每轮开工先 `git fetch` 双查 main，push 被拒即 `pull --rebase` 后重推。
> 用户常设授权（沿 F/G/H/I/J/K 会话）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。

## Destination

**50 个完整自迭代 loop 全部完成**——每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或 opt-in 的机制改进；每轮 Conventional Commits 提交并推送 GitHub；周期性（每 10 轮）+ 收口轮跑全仓 `mvn -B -ntp clean verify` 绿（16 模块 + 快照门 + SpecCoverage 覆盖门）。

## Notes

- 每轮固定四步产物：决策票（shape 票同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<14NN>-<slug>.md`（+README「生产级纵深 X（L 会话 1400 系增量）」表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级定向测试必须绿；新公共 api 面类型入轮再生 API 快照（`-Dbuzhou.api-snapshot.regenerate=true`，优先嵌套 record 不进快照面）；每轮 commit 后 push 本分支，周期性合并 origin/main 防漂移。
- 选题纪律：每轮开工先缺口核查（grep spec+代码，**含 H 池 R1–R50+S1–S10 与 I/J/K 已落地主题一并回避**——回避清单见 progress-effort-1400.md 头注），已实现则台账记 `ruled-out` 顺延备选池；代码库 300+ effort 高度饱和，排重 grep 必须 `-i` 且按类名后缀查。
- 代码规范：无魔法数字（static final 常量或配置）、SLF4J 占位符、record/sealed 优先；进程级静态读面须配 reset 注入点与注释说明；测试无 Mockito——手写 fake/匿名类/lambda stub。
- 模块依赖边界不变：feature 模块互不直接依赖，跨机制协作走 core 事件总线或 core SPI。

## Decisions so far

（每轮 shape 票 Resolution 的 gist 逐轮补登于此）

- [跨会话轮次并发水位观察者的形状裁决](../tickets/T2101-turn-concurrency-shape.md) — TurnConcurrencyTracker implements SessionObserver（once-per-turn seam，Hook 的流式 afterModel 逐 chunk 会漏账故不用）；started/ok/failed 三总量 + active/peakActive 水位 + 守恒式 started=ok+failed+active；同轮实证修复 DefaultAgentSession 两处 guard-block 路径 onTurnStart 后无终结回调的 TURN span 泄漏（非流式补 onTurnEnd/流式补 onTurnError）——HikariCP 池读面思想。
- [工具结果字节直方分桶的形状裁决](../tickets/T2103-result-size-histogram-shape.md) — ToolResultSizeHistogram implements BuzhouHook（opt-in afterTool 单点，零裁决零侵入）：五幂次边界桶（256/1K/4K/16K/64K）+溢出桶 + executed/failed/totalBytes，守恒式 successes=Σbuckets、executed=successes+failed；UTF-8 口径与 J R46–R47 对齐——Prometheus histogram 思想。
- [MCP 工具入参 schema 破坏性变更分级的形状裁决](../tickets/T2105-schema-compat-grader-shape.md) — McpSchemaCompatGrader 纯函数（McpDirectoryDiff 显式留白的 schema 轴）：客户端守恒视角四破坏轴（removed/type_changed/newly_required/enum_narrowed）+fail-closed，嵌套 SchemaCompatVerdict 典序 reasons——buf breaking 思想。
- [EWMA 自适应超时推荐器的形状裁决](../tickets/T2107-adaptive-timeout-shape.md) — **换题轮**（原题 retry budget 与 spec 178 RetryBudget 全撞）：AdaptiveTimeout 纯推导器——EWMA(α=0.3,CAS 无锁)+clamp(⌈EWMA×3⌉,floor,ceiling)+预热哨兵(<3 样本 empty)+stats() 无副作用；不接线执行路径——Envoy timeout budget/Finagle 自适应超时思想。
- [会话 id 熵审计的形状裁决](../tickets/T2109-session-id-entropy-shape.md) — **换题轮**（原题基数守卫与 spec 160 全撞）：SessionIdEntropyAudit 纯函数——字母表下界估计（观测字符类保守求和）+bits=length×log2(alphabet) nanoid 同款+四档闭集（WEAK<64/STRONG≥112≈UUIDv4）+批量四桶；只读不裁决。
- [Dashboard 查询页守卫的形状裁决](../tickets/T2111-query-page-guard-shape.md) — **实证缺陷修复**：listSessions 零钳制（size=1000 万即无界读 store）与 filtered 路径 200 钳制不对称 + 两路径裸 NFE 游标解析——MAX_PAGE_SIZE=200 常量+normalizePageSize/parseCursor 共享辅助两路径同源，翻页语义逐位不变——Grafana query limit 思想。
- [租约续期健康读面的形状裁决](../tickets/T2113-lease-renewal-readout-shape.md) — **换题轮**（序列缺口无序号不伪实现/TTFT 已有）：SessionLeaseGuard 增量 failures+minRemainingAtRenewal 水位+lastRenewalAt+renewalStats()（-1/0 哨兵），recordRenewalSuccess 提取公共记账，语义逐位不变——Redisson watchdog 健康审计思想。
