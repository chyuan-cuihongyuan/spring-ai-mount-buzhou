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

- [SkillAdmin×Search 可见性联动组合测试轮的形状裁决](../tickets/T1645-adminsearch-shape.md) — 纯测试轮第十弹：SkillAdminSearchComboTest 钉住发布/下架→搜索命中联动 + 双读面各自守恒保持。
- [guard 三 hook 链顺序协作组合测试轮的形状裁决](../tickets/T1643-trihook-shape.md) — 纯测试轮第九弹：TriHookChainReadoutTest 钉住 SecretScan(40)→Spotlight(80)→Moderation(210) 三读面链中计数一致与各自守恒保持。
- [offload→readBack 双轴闭环组合测试轮的形状裁决](../tickets/T1641-offread-shape.md) — 纯测试轮第八弹：OffloadReadBackComboTest 钉住溢出落盘→分段回读闭环计数一致性（offloaded/reads 闭环对应）+ 双读面各自守恒。
- [Spotlight×Moderation 顺序协作组合测试轮的形状裁决](../tickets/T1639-spotmod-shape.md) — 纯测试轮第七弹：SpotlightModerationComboTest 钉住同 ctx 连续两 hook（先包裹 ORDER 80 后检查 ORDER 210）双读面计数一致与各自守恒保持。
- [冒烟清单扩展轮的形状裁决](../tickets/T1637-readout-ext-shape.md) — 纯测试轮：ReadoutContractSmokeTest 清单纳入 ArchivePurgeJob/SpillCipher（13→15 成员），清单随读面产出同步扩展纪律闭环。
- [R90 周期预检轮的形状裁决](../tickets/T1635-r90-audit-shape.md) — R81–R89 对账 + 三型欠账中两型实证再现并即时修复（1085 吞噬 + 1090 自漏）；复跑 BUILD SUCCESS 验收。
- [memory 双台账组合测试轮的形状裁决](../tickets/T1631-dualledger-shape.md) — 纯测试轮第六弹：DualLedgerReadoutTest 钉住 fact/episodic 双台账读面互不串账与 reset 独立隔离。
- [memory 域双工具组合测试轮的形状裁决](../tickets/T1629-memorytools-shape.md) — 纯测试轮第五弹：MemoryToolsReadoutTest 钉住 compact_now 与 EpisodeLedger 双读面交叉调用下各自守恒保持、互不串账、reset 独立隔离。
- [双守卫（黑名单+SSRF）组合测试轮的形状裁决](../tickets/T1627-dualguard-shape.md) — 纯测试轮第四弹：DualGuardReadoutTest 钉住双守卫读面同会话独立性与一致性（互不串账、reset 独立隔离）。
- [Skill 管理操作读面的形态裁决](../tickets/T1625-skilladmin-stats-shape.md) — SkillAdminApi 静态五计数（creates/updates/publishes/disables/deletes 独立口径同 R63）+ 嵌套 SkillAdminStats + stats()/resetForTest()；校验异常不入桶（GitHub repo admin API statistics）。
- [fs 全链路四读面组合测试轮的形状裁决](../tickets/T1623-fs-chain-shape.md) — 纯测试轮第三弹：FsChainReadoutTest 钉住沙箱/写/读/黑名单四读面在真实工作流链路下的各自守恒保持与跨面字节对称恒等（R45/R46/R47/R51 组合）。
- [J 系读面统一契约冒烟轮的形状裁决](../tickets/T1621-readout-smoke-shape.md) — starter ReadoutContractSmokeTest 反射驱动 15 读面清单三性质冒烟（stats 组件非负/reset 幂等/重复稳定）；清单显式维护=新读面登记纪律落点（meta-readout）。
- [双档 run_command 对账组合测试轮的形状裁决](../tickets/T1619-dualmode-shape.md) — 纯测试轮：DualModeRunContrastTest 钉住 timeout=0 双档语义分叉（直执行显式拒绝入桶 vs 沙箱补默认送达）+ 各自守恒保持；防语义分叉被误修齐或回归。
- [读写对称守恒组合测试轮的形状裁决](../tickets/T1617-rwsymmetry-shape.md) — 纯测试轮（I 系 R52 先例）：ReadWriteSymmetryTest 钉住 bytesWritten==bytesRead 字节口径恒等（ASCII/中文/混合三内容）+ 双侧守恒保持；读面谱系可信度组合语义（spec 1540 先例）。
- [R80 周期预检轮的形状裁决](../tickets/T1615-r80-audit-shape.md) — R71–R79 对账零缺陷（九域读面布局）+ 跨会话孤儿 spec 补登（三型欠账范式齐备：快照死链/README 死链/孤儿文件）；复跑 BUILD SUCCESS 验收。
- [Spill 加解密读面的形态裁决](../tickets/T1613-spillcipher-stats-shape.md) — SpillCipher 静态四计数（encryptCalls/encryptFailures/decryptCalls/decryptFailures）+ 嵌套 SpillCipherStats + stats()/resetForTest()；加解密独立双组，失败在异常外溢前落桶（KMS 操作审计）。
- [归档清理任务读面的形态裁决](../tickets/T1611-purgejob-stats-shape.md) — ArchivePurgeJob 静态三计数（purgeRounds/purgedTotal/skippedLocked）+ 嵌套 PurgeJobStats + stats()/resetForTest()；purgedTotal 跨轮累计无入口守恒（每轮产出可变）；锁跳过显形（Quartz/Chron job statistics）。
- [Spill 溢出 hook 判定读面的形态裁决](../tickets/T1609-spilloffload-stats-shape.md) — SpillOffloadHook 静态六计数（invocations/durableSkips/errorSkips/cleanInline/offloaded/refrains）+ 嵌套 SpillOffloadStats + stats()/resetForTest()；守恒 invocations = 五结局桶；溢出触发率即管线容量规划信号（logrotate 轮转率）。
- [Runaway 预算 hook 判定读面的形态裁决](../tickets/T1607-runaway-stats-shape.md) — RunawayHook 静态四计数（invocations/blocked 三硬顶合桶/allowed/disabledSkips）+ 嵌套 RunawayStats + stats()/resetForTest()；守恒 invocations = 三结局桶；预算过紧/过松量化信号（上游闸门空结果率同族）。
- [会话归档操作读面的形态裁决](../tickets/T1605-archiver-stats-shape.md) — SessionArchiver 静态四计数（archiveCalls/archived/emptySkipped/pdbRejected）+ 嵌套 ArchiveStats + stats()/resetForTest()；异常外溢入口不入桶口径诚实（S3 lifecycle 归档统计）。
- [沙箱版 run_command 执行分布读面的形态裁决](../tickets/T1603-sandboxrun-stats-shape.md) — SandboxRunCommandTool 静态七计数（calls/runs + blank/blacklist/workdir/timeoutParam/failures 五拒绝桶）+ 嵌套 SandboxRunStats + stats()/resetForTest()；守恒 calls = runs + 五拒绝桶；R52 Out of Scope 误判（装饰同族）就地撤销——实为独立 call 分支实现。
- [evidence_lookup 证据回查读面的形态裁决](../tickets/T1601-evidlookup-stats-shape.md) — EvidenceLookupTool 静态五计数（calls/misses/hits/completeReads/slicedReads）+ 嵌套 EvidenceLookupStats + stats()/resetForTest()；双守恒 calls = hits + misses、hits = complete + sliced；回查 miss 率即证据链引用失配信号（Redis cache hit-rate）。
- [PII 检测引擎读面的形态裁决](../tickets/T1599-piidetector-stats-shape.md) — PiiDetector 静态四计数（scanCalls/scansWithHits/matchesFound dedupe 前原生口径/pseudonymizeCalls）+ 嵌套 PiiDetectorStats + stats()/resetForTest()；弱校验口径（引擎 vs 业务 PiiHitStats 双层对账）（Yara 规则引擎统计）。
- [http_request 受控头丢弃显形的形态裁决](../tickets/T1597-headerdrop-stats-shape.md) — HttpToolStats 追加 headerDrops 第 10 计数（impl-49 黑名单头静默 return 处单点）；旁路修正量不占入口桶（一次请求可丢多头）；适配并行 spec 1603 hostLimitRejects 扩展（OWASP header injection 试探率）。
- [R70 周期预检轮的形状裁决](../tickets/T1595-r70-audit-shape.md) — R61–R69 对账零缺陷（八域读面布局）+ API 快照跨会话欠账 4 类型两次补账（「加类型不随轮再生」系统性欠账，倡议随轮自带 regenerate）；复跑 BUILD SUCCESS 验收。
- [危险工具守卫判定读面的形态裁决](../tickets/T1593-dangerous-tool-stats-shape.md) — DangerousToolGuardHook 静态六计数（invocations/disabledSkips/unmatchedSkips/authorizedSkips/exemptedSkips/escalations）+ 嵌套 DangerousToolStats + stats()/resetForTest()；守恒 invocations = 五结局桶；HITL 等待确认量量化显形（授权面对账）。
- [工具配额消耗读面的形态裁决](../tickets/T1591-toolquota-stats-shape.md) — ToolQuotaHook 静态五计数（calls/allowed/quotaBlocks/unmanagedSkips + excludedTokens 旁路）+ 嵌套 ToolQuotaStats + stats()/resetForTest()；守恒 calls = 三结局桶；配额清单覆盖率对账（per-API quota 消耗对账）。
- [内容安全词表双缝判定读面的形态裁决](../tickets/T1589-moderation-stats-shape.md) — ContentModerationHook 静态五计数（invocations/blocked/masked/cleanSkips/nullSkips）+ 嵌套 ModerationStats + stats()/resetForTest()；守恒 invocations = 四结局桶（双缝共用桶集）；与 micrometer 后端面互补（R57 先例）。
- [Deno 沙箱探测读面的形态裁决](../tickets/T1587-denoprobe-stats-shape.md) — DenoSandbox 静态五计数（availableCalls/probeCacheHits/probes/probeSuccesses/probeUnavailables）+ 嵌套 DenoProbeStats + stats()/resetForTest()；双守恒 availableCalls = 缓存命中 + 重探、probes = 成功 + 不可用；probeTtl 误配 0（每调用重探）量化可见（Envoy health check statistics）。
- [完成轮检测器读面的形态裁决](../tickets/T1585-completedturn-stats-shape.md) — DefaultCompletedTurnDetector 静态三计数（detectCalls/spansDetected/toolCallTurnsSeen）+ 嵌套 CompletedTurnStats + stats()/resetForTest()；弱校验口径（检出率 = 分子/分母，同轮可重复计分母）；悬挂轮全量时检出 0 即压缩管线失能信号（OTel span 完成判定）。
- [读侧 Spotlighting 包裹判定读面的形态裁决](../tickets/T1583-spotlight-stats-shape.md) — SpotlightHook 静态五计数（invocations/wrapped/alreadyWrappedSkips/noticeSkips/errorSkips）+ 嵌套 SpotlightStats + stats()/resetForTest()；守恒 invocations = wrapped + 三跳过桶；包裹覆盖率即注入面收敛度信号（OWASP LLM01 spotlighting 采用率）。
- [双时序事实台账操作读面的形态裁决](../tickets/T1581-factledger-stats-shape.md) — BiTemporalFactLedger 静态四计数（supersededWrites/historyLookups/validAtLookups/corruptRecordLoads）+ 嵌套 FactLedgerStats + stats()/resetForTest()；写/读两类操作独立计数不设人为守恒（口径诚实）；损坏段装载蒸发显形（bitemporal query/mutation 对账）。
- [read_range 回读判定读面的形态裁决](../tickets/T1579-readrange-stats-shape.md) — ReadRangeTool 静态七计数（calls/reads/truncatedReads/skillReads/parseRejects/skillRejects/failures）+ 嵌套 ReadRangeStats + stats()/resetForTest()；守恒 calls = 六结局桶；与 store 层 ReadAuditTrail 审计流水不同轴共存（S3 TransferManager 分页回读统计）。
- [Dashboard HTTP 状态分布读面的形态裁决](../tickets/T1577-dashhttp-stats-shape.md) — DashboardHttpServer（internal 包无公共 API 负担）静态八计数（requests/ok/auth/bad/notFound/tooLarge/unimplemented/serverErrors）+ 嵌套 DashboardHttpStats + stats()/resetForTest()；守恒 requests = ok + 六结局桶；400 两源合桶；行为逐位不变（nginx status zone）。
- [R60 周期预检轮的形状裁决](../tickets/T1575-r60-audit-shape.md) — R51–R59 对账零缺陷（七域读面布局盘点）+ API 快照跨会话欠账就近补账（门 regenerate 指引流程照走，`-am` 解析路径陷阱入档）；复跑 BUILD SUCCESS 验收。
- [compact_now 手动压缩判定读面的形态裁决](../tickets/T1573-compactnow-stats-shape.md) — CompactNowTool 静态五计数（calls/successes/skippeds/failures/unboundRejects）+ 嵌套 CompactNowStats + stats()/resetForTest()；守恒 calls = 四结局桶；与 R53 evict_handle 同谱系（模型主动维护行为采用率，Anthropic /compact）。
- [MCP 工具集轮询提供器读面的形态裁决](../tickets/T1571-toolsetpoll-stats-shape.md) — DbToolSetProvider 静态四计数（polls/changesDetected/unchangedPolls/pollFailures）+ 嵌套 ToolSetPollStats + stats()/resetForTest()；守恒 polls = 三桶和每轮恰落一桶；热更新失效三因（轮询失败/未检出/未轮到）可对账（etcd watch statistics）。
- [skill_search 搜索判定读面的形态裁决](../tickets/T1569-skillsearch-stats-shape.md) — SkillSearchTool 静态五计数（calls/hits/misses/parseRejects/blankQueryRejects）+ 嵌套 SkillSearchStats + stats()/resetForTest()；守恒 calls = 四桶和；与 spec 116 micrometer 遥测互补（后端面 vs 进程内直读），parse/blank 两路径原遥测缺口一并补齐（Algolia zero-result-rate）。
- [崩循环探测器类级水位读面的形态裁决](../tickets/T1567-crashloop-stats-shape.md) — CircuitCrashLoopDetector 静态四计数（opensRecorded/opensTruncated/loopsDetected/recoveriesRecorded）+ 嵌套 CrashLoopWatchStats + stats()/resetForTest()；MAX_MODELS 截断从布尔升格为量化对账信号，null/空白不入桶口径诚实（kube-state-metrics）。
- [情景记忆读写双守恒读面的形态裁决](../tickets/T1565-episodic-stats-shape.md) — EpisodeLedger 静态九计数双守恒（写侧 recordCalls=recorded+recordDropped+recordFailures / 读侧 recallCalls=recallHits+recallEmpties+recallDropped）+ 嵌套 EpisodicMemoryStats + stats()/resetForTest()；fewShotBlock 经 recallExamples 同点计数；J 系首个 memory 域轮（mem0 episodic 命中率）。
- [str_replace 编辑判定读面的形态裁决](../tickets/T1563-strreplace-stats-shape.md) — StrReplaceTool 静态七计数（attempts/successes + param/missingFile/notFound/ambiguous/failures 五拒绝桶）+ 嵌套 StrReplaceStats（totalRejects 派生）+ stats()/resetForTest()；守恒 attempts = successes + totalRejects；notFound/ambiguous 分布即提示词引导有效性信号（Anthropic text editor）。
- [evict_handle 逐出判定读面的形态裁决](../tickets/T1561-evict-stats-shape.md) — EvictHandleTool 静态四计数（attempts/evictions/badPathRejects/parseRejects）+ 嵌套 EvictStats + stats()/resetForTest()；守恒 attempts = evictions + 两拒绝桶；J 系首个 spill 域轮（Anthropic tool_result 清除采用率）。
- [run_command 执行结果分布读面的形态裁决](../tickets/T1559-runcommand-stats-shape.md) — RunCommandTool 静态九计数（attempts/exits 含非零送达/canceled/timeouts + blank/blacklist/workdir/timeoutParam/failures 五拒绝桶）+ 嵌套 RunCommandStats（totalRejects 派生）+ stats()/resetForTest()；守恒 attempts = 四结局桶 + totalRejects（Kubernetes Job status）。
- [命令黑名单拦截判定读面的形态裁决](../tickets/T1557-blacklist-stats-shape.md) — CommandBlacklist 静态三计数（checks/matched/allowed，空白短路归 allowed 诚实口径）+ 嵌套 CommandBlacklistStats + stats()/resetForTest()；守恒 checks = matched + allowed；matches() 返回值逐位不变（Fail2ban 规则命中计数）。
- [R50 周期预检轮的形状裁决](../tickets/T1555-r50-audit-shape.md) — R46–R49 工件对账全绿（幂等脚本纪律生效零吞噬复发）+ 两轮 verify 同位挂死推翻 flake 误判 → max 追踪 CAS 活锁三处同源实锤（重读留循环外，对照 RollingMaxCounter 原版）统一修复（jstack 定位 + 竞争度依赖复现实证法）。
- [http_request 请求量水位与结果分布读面的形态裁决](../tickets/T1553-httptool-stats-shape.md) — HttpRequestTool 静态八计数（attempts/successes + method/url/ssrf/timeoutParam/oversize/failures 六拒绝桶）+ 嵌套 HttpToolStats（totalRejects 派生）+ stats()/resetForTest()；守恒 attempts = successes + totalRejects；参数桶指向模型行为、环境桶指向环境守卫（Envoy upstream 统计分桶）。
- [SSRF 守卫判定分布读面的形态裁决](../tickets/T1551-ssrf-stats-shape.md) — SsrfGuard 静态六计数（checks/allowlisted/dnsAllowed + emptyHost/dns/blocked 三拒绝桶）+ 嵌套 SsrfGuardStats（totalAllowed/totalRejects 派生）+ stats()/resetForTest()；守恒 checks = 放行 + 拒绝（每入口恰落一桶）；check() 返回语义逐位不变（Fail2ban 判定链显形 + OPA decision log）。
- [read_file 读量水位与拒绝分桶读面的形态裁决](../tickets/T1549-readfile-stats-shape.md) — ReadFileTool 静态六计数（attempts/reads/bytesRead/notFileRejects/oversizeRejects/failures）+ 嵌套 ReadFileStats（totalRejects 派生）+ stats()/resetForTest()；守恒 attempts = reads + totalRejects；与 R46 写侧轴间同口径可比（Datadog DogStatsD read/write 对称计量）。
- [write_file 写入量水位与拒绝分桶读面的形态裁决](../tickets/T1547-writefile-stats-shape.md) — WriteFileTool 静态七计数（attempts/writes/bytesWritten/paramRejects/oversizeRejects/noclobberRejects/failures）+ 嵌套 WriteFileStats（totalRejects 派生）+ stats()/resetForTest()；守恒 attempts = writes + totalRejects（每入口恰落一桶）；call() 返回语义逐位不变（Sentry discarded events + Dropwizard Meter）。
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
- [facts 段导入导出行数读面的形态裁决](../tickets/T1499-facts-flow-stats-shape.md) — FactsExporter 嵌套 FactsFlowStats（factsExported/factsImported/importFailures 照抛）+ stats()；导出/导入对账迁移完整性（rsync --stats）。首入 memory 模块。
- [内嵌策略引擎判定分布读面的形态裁决](../tickets/T1501-policy-engine-stats-shape.md) — EmbeddedPolicyEngine 嵌套 PolicyDecisionStats 四桶（allow/deny/escalate/escalateApproved 守恒 == decide 调用数）+ stats()；FIDES approver 通道压力显形（OPA 判定分布谱系）。首入 guard policy 包。
- [审计收集器采集与持久化失败计数读面的形态裁决](../tickets/T1513-audit-ingest-stats-shape.md) — AuditTrailCollector 嵌套 AuditIngestStats（collected/persistFailures/openSessions）+ stats()；持久化失败连续=审计断链风险水位（Splunk HEC ingestion stats）。首入 guard audit 包。
- [提示词注册表解析分布读面的形态裁决](../tickets/T1531-prompt-resolution-stats-shape.md) — InMemoryPromptRegistry 嵌套 PromptResolutionStats（attempts/hits/misses 守恒）+ resolutionStats()；计数收敛到公共解析核心（resolve 双入口与 resolveVersion 不重复计）；解析返回值与异常语义逐位不变——提示词名拼错/标签缺失=配置错误信号，R17 技能轴的 prompt 域分轴。
- [token 估算调用量与总量读面的形态裁决](../tickets/T1519-token-estimate-stats-shape.md) — CharHeuristicTokenEstimator 静态三计数（estimateCalls/batchCalls/totalEstimatedTokens）+ stats()/resetForTest()；预算面估算总量显形（进程级静态先例）。
- [MCP properties 装配解析统计读面的形态裁决](../tickets/T1523-mcp-parse-stats-shape.md) — PropertiesToolSetProvider 静态三计数（servers/bindings/bindingsSkipped）+ parseStats()/resetForTest()；非 Map binding 项静默跳过显形（幽灵配置族）。首入 buzhou-mcp 模块。
- [time-travel fork 操作计数读面的形态裁决](../tickets/T1535-fork-stats-shape.md) — SessionForks 嵌套 ForkStats（forksCreated/messagesCopied）+ stats()；fork 使用水位与复制量（LangGraph get_state_history/fork 思想）。首入 buzhou-memory 模块读面。
- [工具权限判定分布读面的形态裁决](../tickets/T1527-permission-stats-shape.md) — ToolPermissions 嵌套 PermissionStats 四桶（checks/allowed/deniedUndefinedRole/deniedByRules 守恒）+ stats()；fail-closed 拼错角色显形（K8s RBAC audit 思想）。
- [词法排序生效计数读面的形态裁决](../tickets/T1521-lexical-rank-stats-shape.md) — LexicalSkillRanker 嵌套 RankStats（runs/reordered——排序器空转显形）+ stats()；reordered/runs 长期近零=词法路配置错位信号。首入 buzhou-skill 模块。
- [打转检测触发聚合读面的形态裁决](../tickets/T1517-repetition-stats-shape.md) — RepetitionDetectorHook 嵌套 RepetitionStats（fires/blocks/maxRunSeen 峰值）+ stats()；LLM 打转频率调参水位（spec 1032）。首入 core/runaway 包。
- [会话级联清理聚合计数读面的形态裁决](../tickets/T1511-cleanup-stats-shape.md) — SessionCleaner 嵌套 CleanupStats（deleteCalls/cleanedTargets/failedTargets + failuresByTarget 分桶）+ cleanupStats()；目标持续故障显形（PostgreSQL autovacuum stats 思想）。首入 core/cleanup 包。
- [手动压缩操作分布读面的形态裁决](../tickets/T1505-compact-op-stats-shape.md) — ManualCompactor 嵌套 CompactOpStats 五计数（attempts/completed/skipped/failed/foldedMessages，守恒前三和 == attempts）+ opStats()；逐次 CompactResult 之外的跨调用聚合水位（K8s 事件聚合思想）。首入 memory compact 包。
- [模型窗口解析分布读面的形态裁决](../tickets/T1507-window-resolution-stats-shape.md) — TableContextWindowResolver 嵌套 WindowResolutionStats（override/builtIn/fallback 三路守恒 + resolvedWindows 有界快照）；yml 覆盖拼错=幽灵覆盖显形、未知模型回退规模可见。首入 core/token 包。
- [模型预算闸判定分布读面的形态裁决](../tickets/T1509-budget-gate-stats-shape.md) — ModelBudgetGate 嵌套 BudgetGateStats 三桶（checks/allowed/blocked 守恒 == beforeModel 调用数）+ stats()；连续拦截水位=预算配置合理性第一信号（SRE 预算耗尽告警思想）。
- [事实注入覆盖读面的形态裁决](../tickets/T1503-fact-inject-coverage-shape.md) — FactAttachmentRenderer 嵌套 FactInjectStats（renders/factsInjected/factsOmitted）+ stats()；两参 render 收敛为 MAX_VALUE 委托（输出恒等）；max-inject-chars 配置水位（spec 07 渲染端）。
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
| 25 | facts 段导入导出行数读面（factsExported/factsImported/importFailures） | rsync --stats | T1499–T1500 | 777 | 1024 | ✅ |
| 26 | 内嵌策略引擎判定分布读面（四桶含 approver 通道） | OPA 判定分布 | T1501–T1502 | 778 | 1025 | ✅ |
| 27 | 事实注入覆盖读面（renders/injected/omitted） | spec 07 渲染端水位 | T1503–T1504 | 779 | 1026 | ✅ |
| 28 | 手动压缩操作分布读面（五计数守恒） | K8s 事件聚合 | T1505–T1506 | 780 | 1027 | ✅ |
| 29 | 模型窗口解析分布读面（override/内置/回退三路 + 已解析模型窗） | LLM 模型目录覆盖 | T1507–T1508 | 781 | 1028 | ✅ |
| 30 | 模型预算闸判定分布读面（checks/allowed/blocked 守恒） | SRE 预算耗尽告警 | T1509–T1510 | 782 | 1029 | ✅ |
| 31 | 会话级联清理聚合计数读面（目标失败分布显形） | autovacuum stats | T1511–T1512 | 783 | 1030 | ✅ |
| 32 | 审计收集器采集与持久化失败计数读面 | Splunk HEC ingestion stats | T1513–T1514 | 784 | 1031 | ✅ |
| 33 | 打转检测触发聚合读面（fires/blocks/maxRunSeen） | LLM 打转检测 | T1517–T1518 | 785 | 1032 | ✅ |
| 34 | token 估算调用量与总量读面（静态三计数） | 预算面可观测性 | T1519–T1520 | 786 | 1033 | ✅ |
| 35 | 词法排序生效计数读面（runs/reordered——排序器空转显形） | 混合排序生效水位 | T1521–T1522 | 787 | 1034 | ✅ |
| 36 | MCP properties 装配解析统计读面（servers/bindings/bindingsSkipped） | 清单解析统计 | T1523–T1524 | 788 | 1036 | ✅ |
| 37 | 工具权限判定分布读面（allowed/两拒分桶守恒） | K8s RBAC audit | T1527–T1528 | 789 | 1037 | ✅ |
| 38 | 摘要桥操作与代数回退读面（generation 单调回退探测） | 单调水位思想 | T1529–T1530 | 790 | 1038 | ✅ |
| 39 | 提示词注册表解析分布读面（attempts/hits/misses 守恒） | 判定显形谱系 | T1531–T1532 | 791 | 1039 | ✅ |
| 40 | todo 动作分布读面（白名单五桶有界） | 操作分布显形 | T1533–T1534 | 792 | 1040 | ✅ |
| 41 | time-travel fork 操作计数读面（forksCreated/messagesCopied） | LangGraph fork | T1535–T1536 | 793 | 1041 | ✅ |
| 42 | 签名验钥分布读面（verifyAttempts/verifyKeyMisses/rotations） | cert-manager/keyring ops | T1537–T1538 | 794 | 1042 | ✅ |
| 43 | 围栏裁决分布读面（五态判定分桶守恒） | Kafka epoch/consumer-lag | T1539–T1540 | 795 | 1043 | ✅ |
| 44 | J 系阶段对账审计轮（R44 五类对账+修复） | G/H 收口预检先例 | T1541–T1542 | 796 | 1044 | ✅ |
| 45 | fs 沙箱判定计数读面（resolutions/violations 守恒） | chroot escape detection | T1545–T1546 | 797 | 1045 | ✅ |
| 46 | write_file 写入量水位与拒绝分桶读面（attempts/writes/bytesWritten + 四拒绝桶守恒） | Sentry discarded events + Dropwizard Meter | T1547–T1548 | 798 | 1046 | ✅ |
| 47 | read_file 读量水位与拒绝分桶读面（attempts/reads/bytesRead + 三拒绝桶守恒） | Datadog DogStatsD read/write 对称计量 | T1549–T1550 | 799 | 1047 | ✅ |
| 48 | SSRF 守卫判定分布读面（checks/两放行桶 + 三拒绝桶守恒） | Fail2ban 判定链显形 + OPA decision log | T1551–T1552 | 800 | 1048 | ✅ |
| 49 | http_request 请求量水位与结果分布读面（successes + 六拒绝桶守恒） | Envoy upstream statistics | T1553–T1554 | 801 | 1049 | ✅ |
| 50 | 周期预检轮：R46–R49 对账 + max 追踪 CAS 活锁三处实锤修复 + 全仓 verify | jstack 定位 + 竞争度依赖复现实证 | T1555–T1556 | 802 | 1050 | ✅ |
| 51 | 命令黑名单拦截判定读面（matched/allowed 二桶守恒） | Fail2ban 规则命中计数 | T1557–T1558 | 803 | 1051 | ✅ |
| 52 | run_command 执行结果分布读面（exits/canceled/timeouts + 五拒绝桶守恒） | Kubernetes Job status | T1559–T1560 | 804 | 1052 | ✅ |
| 53 | evict_handle 逐出判定读面（evictions + 两拒绝桶守恒） | Anthropic tool_result 清除采用率 | T1561–T1562 | 805 | 1053 | ✅ |
| 54 | str_replace 编辑判定读面（successes + notFound/ambiguous 等五拒绝桶守恒） | Anthropic text editor str_replace | T1563–T1564 | 806 | 1054 | ✅ |
| 55 | 情景记忆读写双守恒读面（record 三桶 + recall 三桶双恒等式） | mem0 episodic memory 命中率 | T1565–T1566 | 807 | 1055 | ✅ |
| 56 | 崩循环探测器类级水位读面（OPEN 量/封顶截断量/循环检出/恢复四计数） | kube-state-metrics crashloop 总账 | T1567–T1568 | 808 | 1056 | ✅ |
| 57 | skill_search 搜索判定读面（hits/misses + 两拒绝桶守恒） | Algolia zero-result-rate | T1569–T1570 | 809 | 1057 | ✅ |
| 58 | MCP 工具集轮询提供器读面（polls 三桶守恒） | etcd watch statistics | T1571–T1572 | 810 | 1058 | ✅ |
| 59 | compact_now 手动压缩判定读面（successes/skippeds/failures/unbound 四桶守恒） | Anthropic /compact 采用率 | T1573–T1574 | 811 | 1059 | ✅ |
| 60 | 周期预检轮：R51–R59 对账全绿 + API 快照跨会话欠账补账 + 复跑 verify BUILD SUCCESS | 门自带 regenerate 指引就近处置 | T1575–T1576 | 812 | 1060 | ✅ |
| 61 | Dashboard HTTP 状态分布读面（ok + 六结局桶守恒） | nginx status zone | T1577–T1578 | 813 | 1061 | ✅ |
| 62 | read_range 回读判定读面（reads/truncated/skill 三组七桶守恒） | S3 TransferManager 分页回读统计 | T1579–T1580 | 814 | 1062 | ✅ |
| 63 | 双时序事实台账操作读面（写入/两类查询/损坏蒸发四计数） | bitemporal query/mutation 对账 | T1581–T1582 | 815 | 1063 | ✅ |
| 64 | 读侧 Spotlighting 包裹判定读面（wrapped + 三跳过桶守恒） | OWASP LLM01 spotlighting 采用率 | T1583–T1584 | 816 | 1064 | ✅ |
| 65 | 完成轮检测器读面（检出率分子/分母显形） | OTel span 完成判定空结果率 | T1585–T1586 | 817 | 1065 | ✅ |
| 66 | Deno 沙箱探测读面（缓存命中/重探/成败双守恒） | Envoy health check statistics | T1587–T1588 | 818 | 1066 | ✅ |
| 67 | 内容安全词表双缝判定读面（blocked/masked + 两跳过桶守恒） | OpenAI moderation 双缝对账 | T1589–T1590 | 819 | 1067 | ✅ |
| 68 | 工具配额消耗读面（allowed/quotaBlocks/unmanaged 三桶守恒） | per-API quota 消耗对账 | T1591–T1592 | 820 | 1068 | ✅ |
| 69 | 危险工具守卫判定读面（五结局桶守恒含 escalations） | HITL 授权面对账 | T1593–T1594 | 821 | 1069 | ✅ |
| 70 | 周期预检轮：R61–R69 对账全绿 + API 快照跨会话欠账 4 类型两次补账 + 复跑 verify BUILD SUCCESS | 门 regenerate 指引 + cwd 绝对路径纪律 | T1595–T1596 | 822 | 1070 | ✅ |
| 71 | http_request 受控头丢弃显形（headerDrops 旁路量） | OWASP header injection 试探率 | T1597–T1598 | 823 | 1071 | ✅ |
| 72 | PII 检测引擎读面（引擎原生匹配与业务上报双层对账） | Yara 规则引擎统计 | T1599–T1600 | 824 | 1072 | ✅ |
| 73 | evidence_lookup 证据回查读面（命中率/切片率双守恒） | Redis cache hit-rate | T1601–T1602 | 825 | 1073 | ✅ |
| 74 | 沙箱版 run_command 执行分布读面（runs + 五拒绝桶守恒） | 同 R52 Job status | T1603–T1604 | 826 | 1074 | ✅ |
| 75 | 会话归档操作读面（archived/emptySkipped/pdbRejected 四桶） | S3 lifecycle 归档统计 | T1605–T1606 | 827 | 1075 | ✅ |
| 76 | Runaway 预算 hook 判定读面（blocked/allowed/disabled 三桶守恒） | 上游闸门空结果率同族 | T1607–T1608 | 828 | 1076 | ✅ |
| 77 | Spill 溢出 hook 判定读面（offloaded/cleanInline 等五桶守恒） | logrotate 轮转率 | T1609–T1610 | 829 | 1077 | ✅ |
| 78 | 归档清理任务读面（purgeRounds/purgedTotal/skippedLocked 三面） | Quartz/Chron job statistics | T1611–T1612 | 830 | 1078 | ✅ |
| 79 | Spill 加解密读面（加密/解密双组四计数） | KMS 操作审计 | T1613–T1614 | 831 | 1079 | ✅ |
| 80 | 周期预检轮：R71–R79 对账全绿 + 孤儿 spec 补登（三型欠账范式齐） + 复跑 verify BUILD SUCCESS | 门反向检测就近补登 | T1615–T1616 | 832 | 1080 | ✅ |
| 81 | 读写对称守恒组合测试轮（bytesWritten==bytesRead 纯测试） | 组合语义 spec 1540 先例 | T1617–T1618 | 833 | 1081 | ✅ |
| 82 | 双档 run_command 对账组合测试轮（timeout 语义分叉钉住） | 纯测试轮 R81 先例 | T1619–T1620 | 834 | 1082 | ✅ |
| 83 | J 系读面统一契约冒烟轮（15 读面非负/归零/稳定元验证） | 性质收缩 meta-readout | T1621–T1622 | 835 | 1083 | ✅ |
| 84 | fs 全链路四读面组合测试轮（沙箱/写/读/黑名单链路守恒） | 纯测试轮第三弹 | T1623–T1624 | 836 | 1084 | ✅ |
| 85 | Skill 管理操作读面（create/update/publish/disable/delete 五面） | GitHub repo admin API statistics | T1625–T1626 | 837 | 1085 | ✅ |
| 86 | 双守卫（黑名单+SSRF）组合测试轮（互不串账钉住） | 纯测试轮第四弹 | T1627–T1628 | 838 | 1086 | ✅ |
| 87 | memory 域双工具组合测试轮（compact_now×episodic 互不串账钉住） | 纯测试轮第五弹 | T1629–T1630 | 839 | 1087 | ✅ |
| 88 | memory 双台账组合测试轮（fact/episodic 互不串账钉住） | 纯测试轮第六弹 | T1631–T1632 | 840 | 1088 | ✅ |
| 89 | spill 域 offload+evict 生命周期组合测试轮（溢出→逐出→回读链路） | 纯测试轮第七弹 | T1633–T1634 | 841 | 1089 | ✅ |
| 90 | 周期预检轮：R81–R89 对账 + README 1085 吞噬与 1090 自漏双修复 + 复跑 verify BUILD SUCCESS | 覆盖门双向检测实证 | T1635–T1636 | 842 | 1090 | ✅ |
| 91 | 冒烟清单扩展轮（ArchivePurgeJob/SpillCipher 纳入 15 读面冒烟） | 清单同步扩展纪律 | T1637–T1638 | 843 | 1091 | ✅ || 92 | Spotlight×Moderation 顺序协作组合测试轮（双读面计数一致） | 纯测试轮第七弹 | T1639–T1640 | 844 | 1092 | ✅ |
| 93 | offload→readBack 双轴闭环组合测试轮（闭环计数一致） | 纯测试轮第八弹 | T1641–T1642 | 845 | 1093 | ✅ |
| 94 | guard 三 hook 链顺序协作组合测试轮（脱敏→包裹→检查三读面一致） | 纯测试轮第九弹 | T1643–T1644 | 846 | 1094 | ✅ |
| 95 | SkillAdmin×Search 可见性联动组合测试轮（发布/下架→搜索命中联动） | 纯测试轮第十弹 | T1645–T1646 | 847 | 1095 | ✅ |
| 96 | （开工时按缺口核查选题） | — | T1647–T1648 | 848 | 1096 |  |

（编号空洞：spec 1035 有意空洞；票号 T1515–T1516/T1525–T1526 漂移 cosmetic——均已在审计轮 spec 1044 入档。）
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
