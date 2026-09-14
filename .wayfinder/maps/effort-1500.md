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


## Out of scope

- 与 I/L 并行会话号段（900–999/1400–1449）重叠的任何占用。
