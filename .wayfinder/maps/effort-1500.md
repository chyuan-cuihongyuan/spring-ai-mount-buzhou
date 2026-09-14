# Wayfinder Map — M 会话 1500 系：借鉴高价值开源项目的 50 轮自迭代（effort #1500 总图）

> **M 会话**（2026-09-15 启动）：继 C（300）/ D（400）/ E（500）/ F（600）/ G（700）/ H（800）/ I（900）/ J（1000）/ K（1200）/ L（1400）之后的第十一条自迭代线。
> **号段裁决（号段声明先行）**：M 会话占用 spec **1500–1549**、票 **T2251–T2350**（每轮 2 张：shape + verify）、impl **1103–1152**（每轮 1 片）、efforts **#1500–#1549**（R1 开张轮 = #1500，R2–R50 内容轮 = #1501–#1549；R51 收口轮不占新 effort 号）。fetch 时 github.com 443 不通（本地 main 领先 origin/main，本地即最新真相），实查本地全档：efforts 至 1400、specs 至 1400、tickets 至 T2102、impl 至 1053——**1500 系与 T2251+ 完全空闲**。
> **并存声明**：I（900 系）/L（1400 系）等并行会话 map 与本线互不触碰；每轮开工先 fetch 双查，push 被拒即 pull --rebase 后重推。
> 用户常设授权（沿 F/G/H/I/J/K/L 会话）：**全程 AFK，不问用户**——每轮 = wayfinder（决策票）→ to-spec → to-tickets → implement → git 自动提交推送 GitHub。

## Destination

**50 个完整自迭代 loop 全部完成**——每轮从高价值 GitHub 项目借鉴一个思想，落地为本仓一个小而完整、带测试、默认零行为变化或缺陷修复型/opt-in 的机制改进；每轮 Conventional Commits 提交并推送 GitHub；周期性（每 10 轮）+ 收口轮跑全仓 `mvn -B -ntp clean verify` 绿（16 模块 + 快照门 + SpecCoverage 覆盖门）。

## Notes

- 每轮固定四步产物：决策票（shape 票同轮开+解决，Resolution 注明「用户常设授权 AFK、可推翻」）→ `docs/spec/<15NN>-<slug>.md`（+README「生产级纵深 XI（M 会话 1500 系增量）」表行，SpecCoverageTest 门）→ impl 切片 → 模块代码+测试。
- 每轮模块级定向测试必须绿；新公共 api 面类型入轮再生 API 快照（优先嵌套 record 不进快照面）；每轮 commit 后 push，周期性合并 origin/main 防漂移。
- 选题纪律：每轮开工先缺口核查（grep spec+代码，**含 H 池 R1–R50+S1–S10 与 I/J/K/L 已落地主题一并回避**），已实现则台账记 `ruled-out` 顺延备选池；代码库 300+ effort 高度饱和，排重 grep 必须 `-i` 且按类名后缀查。
- 代码规范：无魔法数字（static final 常量或配置）、SLF4J/System.Logger 占位符、record/sealed 优先；进程级静态读面须配 reset 注入点与注释说明；测试无 Mockito——手写 fake/匿名类/lambda stub。
- 模块依赖边界不变：feature 模块互不直接依赖，跨机制协作走 core 事件总线或 core SPI。

## Decisions so far

（每轮 shape 票 Resolution 的 gist 逐轮补登于此）

- [SessionObserver 通知面异常隔离收口的形状裁决](../tickets/T2251-observer-notify-isolation-shape.md) — DefaultAgentSession 12 处观察者裸 forEach 通知点（onOpen/onTurnStart×2/onTurnEnd×3/onTurnError×5/onCancel）统一改走 notifyObservers 隔离派发：单个观察者 RuntimeException 记 ERROR 日志后继续其余观察者、不向上传播（onOpen 在构造器尾部未隔离时观测组件缺陷可炸掉整个会话构造且半初始化泄漏）——Guava EventBus SubscriberExceptionHandler 思想；impl-30 的 onClose/deliverEvent 隔离先例在 observer 其余回调面的补全；onClose 既有失败收集聚合语义不动。
- [HookChain 事件通知面逐 hook 异常隔离的形状裁决](../tickets/T2253-hook-event-isolation-shape.md) — 通知面/裁决面分离：fireEvent（返回 void、无 Block/Replace 裁决语义）链内逐 hook try/catch 隔离 + 计时 try/finally 入账；run() 裁决面保持 fail-fast 治理语义（治理点异常必须可见）；deliverEvent 链级隔离与链内隔离形成两级防护。
- [F8/F11 判定收尾的形状裁决](../tickets/T2329-f8-f11-shape.md) — F8 编程面 only（标签语义业务自定无默认可兜）；F11 单路径 Hook 化（无双路径即无幂等问题）——F1-F11 全档闭环。
- [spec 05 判定项批量回写的形状裁决](../tickets/T2327-spec05-adjudications-shape.md) — F3 per-session 定案（Builder Bean 不采用）/F4 键表实现重写/F6 纯函数口径——F 系判定项全清。
- [CONTEXT M 系术语段的形状裁决](../tickets/T2323-context-terms-shape.md) — 新节五条术语（一条一机制组跨 spec 聚合）。
- [A/B 进度读面的形状裁决](../tickets/T2321-ab-progress-shape.md) — CompareProgress + 过程/终态快照（skipped null 占位无对象——终态统一 done=total）；spec 1534 扩散。
- [评估进度读面的形状裁决](../tickets/T2319-eval-progress-shape.md) — progress() 不可变快照（三处 volatile 更新点，首版漏 cancelled 占位分支被测试当场抓住）；同步 run 的跨线程轮询面。
- [M 系运维段的形状裁决](../tickets/T2317-runbook-shape.md) — runbook 第 24 节九行机制表（键/信号/要点）——N 系先例同款。
- [批预算装配链测试的形状裁决](../tickets/T2315-batch-budget-assembly-shape.md) — 三用例（值透传/显式 0 语义关/缺省零装配）；M 系三 Holder 装配测试全覆盖。
- [瞬断重试装配链测试的形状裁决](../tickets/T2313-retry-assembly-test-shape.md) — EvalPrune 先例双用例；装配测试当场实证 R13 Duration 转换缺陷（字符串源无转换器炸装配）并修复（DurationStyle 宽松解析）。
- [Javadoc 全仓收口的形状裁决](../tickets/T2311-javadoc-final-sweep.md) — 8 个补齐（AutoConfig×3+Jdbc SPI×5）；core 六包外 30 个裁定不补（internal 域，门辖界维持）——五-1 终轮闭环。
- [memory/spill Javadoc 补齐的形状裁决](../tickets/T2309-memory-spill-javadoc-shape.md) — 40 缺全补（角色一句话+spec 引注）；模块级门扩散留候选池；367 用例零回归。
- [load 已序快路径的形状裁决](../tickets/T2307-load-fastpath-shape.md) — O(n) isSorted 检查免热路径全量排序（正常追加天然有序）；乱序回退全排序；快照语义保持。
- [批预算错误反馈豁免的形状裁决](../tickets/T2305-error-feedback-exempt-shape.md) — isErrorFeedback 候选跳过（纠错信号保护；全部错误反馈极端批按序截保预算语义）；R29 即时补强。
- [批级回喂预算的形状裁决](../tickets/T2303-batch-budget-shape.md) — applyBatchBudget 降序贪心截大者（小结果完整）+ BatchResponseBudgetHolder（>0 声明即启用）+ HarnessAssembler 拾取；单工具限幅之上的批维度护栏。
- [serial-groups yml 通道的形状裁决](../tickets/T2301-serial-groups-yml-shape.md) — fromYml serial-groups map 解析 + configure yml 优先覆盖注解；F2 全档闭环（超时键 ToolTimeoutOverrides 先行）。
- [配置错误显形双小项的形状裁决](../tickets/T2299-dup-name-observer-shape.md) — hook 重名 WARN（order 平局派发序不稳定+对位歧义）；addObserver 同实例幂等去重（listener 域维持——lambda 多实例 identity 去重无意义）。
- [A/B 并行波间早停的形状裁决](../tickets/T2297-ab-wave-earlystop-shape.md) — 分波化让 SPRT 早停/宿主取消波间真生效（此前全量派发近似无效）；波内 scored 原子语义不变；16 用例零回归。
- [并行评估波间剪枝的形状裁决](../tickets/T2295-parallel-prune-shape.md) — 分波执行 + 波间观察窗检查（串行同款语义）：并行剪枝从「诚实不生效」变波间止损；未配策略单波全量零变化；旧行为用例 parallelPathHonestNoPrune 按行为变更纪律改写为 parallelPathPrunesBetweenWaves。
- [MemoryModule yml 样板统一的形状裁决](../tickets/T2293-memory-yml-dedup-shape.md) — memoryLeaf/memorySub 两 helper 统一 9 处嵌套 instanceof 提取（3 处反射/泛型复杂体保留）；等值重构。
- [构造器 this 逃逸修复与三裁定的形状裁决](../tickets/T2291-this-escape-shape.md) — AsyncObservabilityPipeline 惰性启动（首事件 CAS）；DbToolSetProvider 首跑延迟窗口理论性不整改；五-2 三键边界追认；六-2 指纹双轨不统一（值稳定性优先）。
- [日志双门面追认与 spec 04 回写的形状裁决](../tickets/T2289-logger-spec04-shape.md) — 69 文件既成风格不迁移（占位符风格硬约束不变、同文件不混用）；spec 04 补 mcp 装配属性增量。
- [文档间残留矛盾三裁定的形状裁决](../tickets/T2287-doc-adjudication-shape.md) — perf 10ms/20ms = 目标-红线关系非矛盾；promptfoo star 统一时点注记；spec 09 追认 test 边豁免——design-incompleteness 清单经 M 系 20 轮全部闭环或裁定。
- [spill 默认值单一事实源与六-6/六-9 裁定的形状裁决](../tickets/T2285-spill-defaults-shape.md) — 三常量落 SpillProperties（threshold 引用 SpillOffloadHook）三处引用收口；六-6 load 前两参 = SPI 扩展位注记保留；六-9 newSingleThreadScheduledExecutor = ScheduledThreadPoolExecutor(1) 等价（delay queue 无无界风险）不整改。
- [spec 07 回写与序位常量化的形状裁决](../tickets/T2283-spec07-order-consts-shape.md) — spec 07 三处六→七切面；四处 order 魔法值常量化（同值零行为）；CounterAtomicitySpreadTest 经 N 会话承接修复（commit 30197389 引用 M 系 spec1513 记档——跨会话协作闭环确认）。
- [降级存储契约对齐与机制计数口径的形状裁决](../tickets/T2281-store-contract-align-shape.md) — redis 版补 degrade 指标（jdbc impl-41 先例同款，六-1 契约漂移修复）；README 升十大机制（七-1 取「README 升」——韧性层已是生产纵深主力域）。
- [SHA-256 裸异常迁移与死代码清扫的形状裁决](../tickets/T2279-sha256-exception-shape.md) — 全量重扫 11 处（清单后又长出 5 处同型）统一 BuzhouException(CONFIG_INVALID)（spec 50 §A 先例形态）；verifySignature 零调用删除（逻辑已迁 AuditChainVerifier）。
- [中断与异常上下文卫生轮的形状裁决](../tickets/T2277-interrupt-context-hygiene-shape.md) — mcp shutdown 吞 InterruptedException 不恢复（五-4，R5 审计漏网：一把抓把中断包进去）收窄+恢复+break；DiskSpillStore 9 处裸 message 补上下文（五-8）；HEAD 既有 CounterAtomicitySpreadTest 失败（TokenBudgetHook NPE）记档留归属会话。
- [配置全键表 config-reference 的形状裁决](../tickets/T2275-config-reference-shape.md) — 三段式生成（record 组件全表/fromYml 子键指针段/env 直读键）；camelCase≡kebab-case 说明；组件级中文注记声明为后续增量（诚实分档）。
- [幂等工具瞬断重试自动装配通道的形状裁决](../tickets/T2273-transient-retry-shape.md) — 动工查重修正：RetryingToolCallback 已存在（spec 133/302），F1 残留=自动装配通道+瞬断白名单；IdempotentToolRetryHolder（幂等门：注解/白名单）+ RetryPolicy transientOnly 档（isTransient 白名单 cause 链三层，默认 false 既有语义不变）。
- [ConfigMaps indexed 属性数字键归一的形状裁决](../tickets/T2271-indexed-coerce-shape.md) — properties/命令行/env-var 源的 key[i].f=v 被 Binder mapOf 绑成 Map 形态（{key={0={f=v}}}），fromYml 列表键静默失效；normalizeValue 数字键全集按数值序转 List（防字典序 10<2），混合键保持 Map，嵌套递归。
- [design-incompleteness 小缺口清扫的形状裁决](../tickets/T2269-f7-f10-sweep-shape.md) — F7：canary.selected payload 补 sessionId（常量 Javadoc 自钉未兑现，null 省略条件包含）；F10：spec 07 resume 推演名回写指向 resumeWith；F 系查重入档：F1 瞬断重试/F9 config-reference 留候选池，F2 部分修 F5 已修。
- [危险工具默认 HITL 自动带入桥的形状裁决](../tickets/T2267-dangerous-bridge-shape.md) — core 进程级 DangerousToolRegistry 桥（供给方 tools 灌注/消费方 guard 并入，双方只见 core 白名单不破）；guard autoconfig afterName 字符串引用保装配时序；三参默认形态并入 + yml 显式优先去重 + auto-dangerous-bridge=false 逃生；S2 硬偏差闭环。副产出发现：ConfigMaps.sub 对 properties 源 indexed 属性绑 Map 而非 List（guard dangerous-tools 在 .properties 源下静默失效的既有坑，候选池）。
- [MCP 危险工具默认动词模式的形状裁决](../tickets/T2265-mcp-default-dangerous-shape.md) — BuzhouMcpProperties 缺省（null）→ 七动词前缀 glob 默认集（spec 14 §F 承诺，design-incompleteness S1 硬偏差）；显式空列表保留 = 关闭逃生门（yml [] 绑定非 null）；影响面收敛（dangerousToolNames 零执行面消费方）；S2（starter HITL 自动挂接）另轮。
- [A/B 对比 run 宿主取消面的形状裁决](../tickets/T2263-ab-cancel-shape.md) — spec 1505 取消语义扩散到 PairwiseEvalRunner：requestCancel()（compare 开始清零）+ 未起项复用 skipped 桶 + PairwiseSummary.hostCancelled 布尔区分统计达界停（SPRT spec 1605）与宿主叫停，9/7 参兼容构造器保留。
- [EvalRunner 评估 run 协作式取消面的形状裁决](../tickets/T2261-eval-cancel-shape.md) — 实例级 requestCancel()（volatile 标记 + run 开始清零）项边界生效：在飞项做完、未启动项标新状态 cancelled（与 pruned 失败率止损分立）、不进三桶、run 照常落盘可分析已完成部分；此前宿主只能跑完全程或等自动止损（剪枝/预算闸都是自动触发无主动通道）——K8s Job 删除传播语义。
- [BuzhouTool destructive 风险注解的形状裁决](../tickets/T2259-destructive-annotation-shape.md) — @BuzhouTool 加 destructive() default false（注解成员默认值源/二进制兼容）；四个写侧内置工具标注；ToolsModule 危险名单从三处手工 dangerous.add 改为注解扫描（行为等价，新工具标注即入册）；MCP tool annotations destructiveHint 思想。
- [核心 API 包类级 Javadoc 覆盖门的形状裁决](../tickets/T2257-api-javadoc-gate-shape.md) — 六包 192 公共类型 32 缺类级 Javadoc（含 AgentSession/BuzhouHook 最核心 API，规范违例）；逐一补齐 + CoreApiJavadocCoverageTest 纪律变测试（注解夹层感知）；Spotless 静态门思想，spec 213 先例。
- [计时聚合器双子实例清零面的形状裁决](../tickets/T2255-aggregator-reset-shape.md) — HookTimingAggregator/ToolTimingAggregator 各补公开 reset()（清空 timings、幂等、不碰 Holder 开关）：Holder.reset() 只置 null 关聚合，实例账只增不减——测试基线污染与运维基线重建双缺；Prometheus counter reset 语义 + 仓库规范「进程级静态读面须配 reset 注入点」符合性补全，先例 ToolInFlight.reset()。

## Not yet specified

- R17+ 候选池（design-incompleteness 剩余 + 本线副产出）：六-1 DegradingObservabilityStore 双份分叉（redis 版缺 degrade 指标）；五-2 @Bean 裸读 Environment 约 13 处（M 系 R13 装配沿 EvalPrune 先例也用了 getProperty——后续统一整改时一并收口）；五-6 魔法值（advisor order 四处 + spill 默认值散落）；四-5 spec 07「六切面」回写七切面；四-7 spec 04 回写 mcp 属性增量；七-1 README/CLAUDE 机制计数口径（9+韧性=10）；MCP 动态危险名单桥（连接后才知道工具名，静态灌注不适配——雾区）；CounterAtomicitySpreadTest HEAD 既有失败（TokenBudgetHook NPE request()=null——R21 预检轮复查归属）。
- 规避：N 会话活跃于 resilience（LFU 采样驱逐/SPRT/锁迁移），选题避开其 effort-1600 台账已落主题。

## 轮次台账

| 轮 | 主题 | 借鉴源 | 票 | impl | spec | ✅ |
|---|------|--------|----|------|------|---|
| 1 | 开张轮：SessionObserver 通知面异常隔离（12 处裸 forEach → notifyObservers 隔离派发） | Guava EventBus SubscriberExceptionHandler | T2251–T2252 | 1103 | 1500 | ✅ |
| 2 | HookChain 事件通知面逐 hook 隔离（通知面/裁决面分离） | spec 1500 思想在 hook 域的同源应用 | T2253–T2254 | 1104 | 1501 | ✅ |
| 3 | 计时聚合器双子实例清零面（reset() 幂等 + Holder 不动） | Prometheus counter reset 语义 | T2255–T2256 | 1105 | 1502 | ✅ |
| 4 | 核心 API 包类级 Javadoc 覆盖门（32 类型补齐 + 纪律变测试） | Spotless 静态门 + spec 213 纪律变测试先例 | T2257–T2258 | 1106 | 1503 | ✅ |
| 5 | BuzhouTool destructive 风险注解（危险名单注解驱动化，行为等价） | MCP tool annotations destructiveHint | T2259–T2260 | 1107 | 1504 | ✅ |
| 6 | EvalRunner 评估 run 协作式取消（requestCancel + STATUS_CANCELLED） | Kubernetes Job 删除传播语义 | T2261–T2262 | 1108 | 1505 | ✅ |
| 7 | A/B 对比 run 宿主取消（hostCancelled 区分叫停原因） | spec 1505 扩散（NNN 扩散先例模式） | T2263–T2264 | 1109 | 1506 | ✅ |
| 8 | MCP 危险工具默认动词模式（S1 硬偏差修复） | spec 14 §F 承诺落地（design-incompleteness 清单驱动选题） | T2265–T2266 | 1110 | 1507 | ✅ |
| 9 | 危险工具默认 HITL 自动带入桥（S2 硬偏差修复） | core 注册表桥 + afterName 装配编排（Spring Boot 官方解耦模式） | T2267–T2268 | 1111 | 1508 | ✅ |
| 10 | design-incompleteness 小缺口清扫（F7 canary payload + F10 spec 回写） | 评审清单驱动选题 | T2269–T2270 | 1112 | 1509 | ✅ |
| 11 | 周期预检轮：全仓 clean verify——三轮后全绿（①自造构建竞争 NoClassDefFound 非缺陷；②LaneLimitingToolCallbackWaitTest 时序 flake 57μs 越界就近处置：容差 1ms→50ms 常量化；③17 模块 BUILD SUCCESS 含 JaCoCo/enforcer/Spec 双门） | 周期 verify + flake 就近处置 | — | — | — | ✅ |
| 12 | ConfigMaps indexed 属性数字键归一（properties 源列表键静默失效修复） | Spring Binder mapOf 弱点的通用 coerce（R9 副产出发现） | T2271–T2272 | 1113 | 1510 | ✅ |
| 13 | 幂等工具瞬断重试自动装配通道（F1 落地：查重发现既有装饰器，补声明式装配+瞬断档） | spec 05 承诺 × AWS SDK 幂等重试纪律 | T2273–T2274 | 1114 | 1511 | ✅ |
| 14 | 配置全键表 config-reference（F9 闭环：57 record/198 组件键 + fromYml + env 三段式） | spec 21 承诺债 | T2275–T2276 | 1115 | 1512 | ✅ |
| 15 | 中断与异常上下文卫生轮（mcp shutdown 中断恢复 + DiskSpillStore 9 处上下文） | design-incompleteness 五-4/五-8 | T2277–T2278 | 1116 | 1513 | ✅ |
| 16 | SHA-256 裸异常迁移 CONFIG_INVALID（11 处，较清单多 5）+ 审计死代码删除 | design-incompleteness 四-4/六-2 部分 | T2279–T2280 | 1117 | 1514 | ✅ |
| 17 | 降级存储契约对齐（redis 补 degrade 指标）+ 机制计数口径统一（九大→十大） | design-incompleteness 六-1/七-1 | T2281–T2282 | 1118 | 1515 | ✅ |
| 18 | spec 07 七切面回写 + 四处序位常量化（N 会话承接 CounterAtomicity 修复闭环确认） | design-incompleteness 四-5/五-6 部分 | T2283–T2284 | 1119 | 1516 | ✅ |
| 19 | spill 默认值单一事实源（五-6 收口）+ 六-6/六-9 裁定入档 | design-incompleteness 五-6/六-6/六-9 | T2285–T2286 | 1120 | 1517 | ✅ |
| 20 | 文档间残留矛盾三裁定（perf 口径/promptfoo star/test 边豁免）——design-incompleteness 可做项全档闭环 | design-incompleteness 七-2/七-3/四-8 | T2287–T2288 | 1121 | 1518 | ✅ |
| 21 | 周期预检轮：全仓 verify 16 模块绿唯快照门红→隔离 worktree 再生快照补账（HEAD 已提交的 10 新类型：M 系 IdempotentToolRetryHolder + 并行会话 9 个 Holder/Hook；api-surface/CONTEXT 计数 466→893 陈旧口径刷新） | 周期 verify + 快照再生 | — | — | — | ✅ |
| 22 | System.Logger 双门面追认（69 文件既成风格）+ spec 04 mcp 属性回写 | design-incompleteness 五-3/四-7 | T2289–T2290 | 1122 | 1519 | ✅ |
| 23 | 观测管线构造器 this 逃逸修复（惰性启动）+ 五-2/六-2 指纹/DbToolSetProvider 三裁定 | design-incompleteness 六-7/五-2/六-2 | T2291–T2292 | 1123 | 1520 | ✅ |
| 24 | MemoryModule yml 解析样板统一（9/13 → 两 helper） | design-incompleteness 六-5 部分 | T2293–T2294 | 1124 | 1521 | ✅ |
| 25 | 并行评估波间剪枝做实（spec 901「并行诚实不生效」边界收口） | Rayon 分波 cooperative batching 思想 | T2295–T2296 | 1125 | 1522 | ✅ |
| 26 | A/B 并行波间早停（SPRT/取消波间真生效，spec 1522 扩散） | spec 1522 扩散 | T2297–T2298 | 1126 | 1523 | ✅ |
| 27 | 配置错误显形双小项（hook 重名 WARN + observer 幂等注册） | Kong 插件重名诊断思想 | T2299–T2300 | 1127 | 1524 | ✅ |
| 28 | serial-groups yml 通道（F2 残留收口——yml 覆盖注解合并） | design-incompleteness F2 | T2301–T2302 | 1128 | 1525 | ✅ |
| 29 | 批级工具结果回喂预算（贪心截大者，opt-in） | Anthropic 工具结果 token 预算思想 | T2303–T2304 | 1129 | 1526 | ✅ |
| 30 | 批预算错误反馈豁免（spec 1526 即时补强） | 「错误即反馈」通道语义的预算域延伸 | T2305–T2306 | 1130 | 1527 | ✅ |
| 31 | 周期预检轮：主区撞并行 mvn 竞争（匿名类 NoClassDefFound）→隔离 worktree 全仓 verify 16 模块绿唯快照门欠账→再生（M 系 BatchResponseBudgetHolder + 并行 NegativeCachingHolder 两类型补账） | 周期 verify + 快照再生 | — | — | — | ✅ |
| 32 | InMemoryMessageStore.load 已序免排序快路径（热路径退化点消除） | 有序性检查快路径模式（TimSort 先验同款思想） | T2307–T2308 | 1131 | 1528 | ✅ |
| 33 | memory/spill 40 个公开类型类级 Javadoc 补齐（五-1 扩散） | R4 core 门的地模块扩散 | T2309–T2310 | 1132 | 1529 | ✅ |
| 34 | 类级 Javadoc 全仓收口（8 补齐 + 六包外辖界裁定，五-1 终轮） | spec 1503/1529 收口 | T2311–T2312 | 1133 | 1530 | ✅ |
| 35 | 瞬断重试装配链测试（R13 补账 + Duration 转换缺陷实证修复） | EvalPrune 装配链测试先例 | T2313–T2314 | 1134 | 1531 | ✅ |
| 36 | 批预算装配链测试（R29 补账——M 系三 Holder 装配测试全覆盖） | 装配链测试扩散 | T2315–T2316 | 1135 | 1532 | ✅ |
| 37 | M 系增量运维段（runbook 第 24 节九行机制表） | N 系第 23 节同款先例 | T2317–T2318 | 1136 | 1533 | ✅ |
| 38 | 评估 run 进度读面（progress：done/total/cancelled 跨线程轮询） | tqdm 进度条思想 | T2319–T2320 | 1137 | 1534 | ✅ |
| 39 | A/B 对比进度读面（spec 1534 扩散：过程+终态快照） | spec 1534 扩散 | T2321–T2322 | 1138 | 1535 | ✅ |
| 40 | CONTEXT.md M 系术语段（五条：通知/裁决分离等） | 领域术语台账同步 | T2323–T2324 | 1139 | 1536 | ✅ |
| 41 | 周期预检轮：worktree 全仓 verify——tools RunCommandHardeningTest 稳定红破案（谓词裸 "sleep 30" 子串误伤同机并行会话轮询 shell 命令行——实证 pgrep 命中 N 会话快照循环）；谓词锚定 marker 唯一路径 + 垂死窗口轮询；starter 快照门欠账待 R51 收口统一再生 | 周期 verify + 测试缺陷修复 | — | — | — | ✅ |
| 42 | spec 05 判定项批量回写（F3 advisor per-session 定案/F4 键表实现重写/F6 随机源口径） | design-incompleteness 判定项清扫 | T2327–T2328 | 1141 | 1538 | ✅ |
| 43 | F8/F11 判定收尾（编程面 only/单路径 Hook 化——F 系全清） | design-incompleteness 判定项收尾 | T2329–T2330 | 1142 | 1539 | ✅ |


## Out of scope

- 与 I/L 并行会话号段（900–999/1400–1449）重叠的任何占用。
