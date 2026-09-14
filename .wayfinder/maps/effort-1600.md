# Effort #1600 — N 会话 1600 系总图（50 轮自迭代，兼 R1）

> **号段声明（声明先行制度）**：N 会话占用 effort 1600–1649 / specs 1600–1649 / 票 T2351–T2450 / impl 1153–1202。
> 开号前已核对：maps 至 1400（L 系，进行中）+ M 系 1500 已开工（票 T2251–T2350，本文首写时 MAP 已登记，
> 首版 T2301–T2400 与之撞号即让出改 T2351–T2450）；impl 至 1053（M 续 1103–1152，我顺延 1153 起）。
> fetch origin 时网络不通（github 443 超时）——以本地 main 为号段真值源，push 恢复后以远程为准复核。

## Destination

50 轮自迭代闭环：每轮一个「借鉴 GitHub 高价值开源项目思想」的小而完整纵切片（opt-in / 默认零行为变化），
含 spec + 决策票 + 实现 + 测试 + 单轮 commit，全绿收口。与 L（1400 系）/ M（1500 系）并行会话互不越号段。

## Notes

- 每轮四步：wayfinder（本图登记）→ to-spec（docs/spec/16NN + README XII 章表行）→ to-tickets（形状/验收两票，创建即 closed+Resolution）→ implement（TDD + 模块测试 + commit）。
- 仓库工程约束：中文注释、SLF4J 占位符、无魔法数字（static final）、record 优先、Conventional Commits + emoji 首行、正文引用票/impl/spec/effort。
- 全仓 verify 有并行会话构建竞争史（core 挂死 32 分钟教训）——常规轮跑受影响模块测试，收口轮走隔离 worktree 全仓 verify。
- GitHub push 网络不通时本地累积 commit，恢复后补推。

## Decisions so far

- [R1 形状：语义缓存 LFU 采样驱逐](../tickets/T2351-r1-lfu-shape.md) — 借鉴 Redis allkeys-lfu + maxmemory-samples：驱逐从「纯 eldest」升级为「采样窗口内最低命中数先出」，opt-in evictionSampleSize（默认 0=纯 LRU 零变化）
- [R1 验收](../tickets/T2352-r1-lfu-verify.md) — 行为测试钉死四断言（默认零变化/热条目保护/计数封顶/负参拒绝）+ 属性组校验 + 装配传参

- [R2 形状：MCP 连接最大寿命](../tickets/T2353-r2-lifetime-shape.md) — HikariCP maxLifetime 思想：连接到寿退役重建（复用 spec 703 rebuildEntry 排水口径），在飞连接推迟到下轮（归还时退役语义）
- [R2 验收](../tickets/T2354-r2-lifetime-verify.md) — 伪连接+可控时钟四断言：未到寿不动/到寿重建/在飞推迟/关零行为

- [R3 形状：熔断启动宽限](../tickets/T2355-r3-warmup-shape.md) — K8s startupProbe 思想：进程构造后 warmup 期内的跳闸判定豁免（窗口照记、成功照常冲淡——宽限结束已积累样本立即恢复完整判定，只放过启动抖动不放过真故障）
- [R4 验收](../tickets/T2356-r3-warmup-verify.md) — 可控时钟四断言：宽限内豁免/宽限后恢复跳/成功冲淡自愈/默认关零变化

- [R4 形状：http_request per-host 并发闸](../tickets/T2357-r4-limiterconn-shape.md) — Nginx limit_conn 思想：同 host 在飞请求超上限快速失败（拒绝不排队），CAS 计数器实现，第七拒绝桶进守恒式
- [R4 验收](../tickets/T2358-r4-limiterconn-verify.md) — 闸单测四断言 + 工具集成（占满拒绝/释放放行/守恒式/无闸零变化）

- [R5 形状：响应缓存 stale-if-error](../tickets/T2359-r5-stale-shape.md) — Varnish grace / RFC 5861 思想：staleWindow 内过期条目保留，模型调用失败时 getStale 救场不抛；无救场条目异常照抛（失败语义不静默吞）
- [R5 验收](../tickets/T2360-r5-stale-verify.md) — store 宽限语义四断言 + advisor 失败救场两断言 + 默认关零变化

- [R6 形状：SPRT 序贯提前终止](../tickets/T2361-r6-sprt-shape.md) — Wald 序贯概率比检验（GrowthBook/Statsig sequential testing 同源）：符号检验口径 + 方向分离（B 全胜不得算成 A 优——MLE 双侧符号修正），达界即停省剩余项执行成本
- [R6 验收](../tickets/T2362-r6-sprt-verify.md) — 判定器边界（4 胜继续/5 胜达界/对称）+ runner 集成（40 项第 5 项停、skipped=35、决策入 summary）+ 未启用零变化

- [R7 形状：虚拟线程 pinning 审计+金丝雀热路径修复](../tickets/T2363-r7-pinning-shape.md) — 全仓审计 17 组风险（高危 6 组/中危 11 组）；Top1=CanaryToolCallback.route 锁内完整工具执行：三段式修复（路由决策锁内→执行锁外→计数锁内），计数原子/回滚最终一致不变
- [R7 验收](../tickets/T2364-r7-pinning-verify.md) — 双 latch 并行断言（锁内执行时代码必超时）+ 计数守恒 + 既有 7 用例零回归

- [R8 形状：RollingJsonlWriter 锁迁移](../tickets/T2365-r8-jsonl-lock-shape.md) — spec 1606 排队项落地：monitor→ReentrantLock（互斥语义零变，虚拟线程 unmount 不 pin——HarnessToolCallingManager 先例）；行完整性/计数守恒用并发测试钉住
- [R8 验收](../tickets/T2366-r8-jsonl-lock-verify.md) — 8 虚拟线程 ×50 行并发追加零撕裂零丢失 + 既有 10 用例零回归

- [R9 形状：DiskSpillStore 锁迁移](../tickets/T2367-r9-spill-lock-shape.md) — spec 1606 高危 #3 落地：store/usage 两方法 monitor→ReentrantLock，「一次调用一次 spill」互斥语义不变
- [R9 验收](../tickets/T2368-r9-spill-lock-verify.md) — 同 uri 6 并发恰一成功五拒绝（IllegalStateException）+ 异 uri 8 并发全成功 + spill 模块 168 用例零回归

- [R10 形状：WebhookOutbox 锁迁移](../tickets/T2369-r10-outbox-lock-shape.md) — spec 1606 中危 #1 落地：append/appendRetry/orphanIndexCount/requeueDead 四方法 monitor→ReentrantLock wrapper（*Locked 方法体不动），虚拟线程 dispatcher 下锁内 store IO unmount 不 pin
- [R10 验收](../tickets/T2370-r10-outbox-lock-verify.md) — webhook 包 105 用例零回归（互斥语义由既有 outbox 行为测试全量覆盖）

- [R11 形状：离群驱逐生产接线+分类感知](../tickets/T2371-r11-outlier-wire-shape.md) — 重大发现：spec 149 建的 ModelOutlierEjection 是未接线孤类（recordError/filter 生产路径零调用=机制等于关闭）；本轮接线：advisor 全路径喂入（主/金丝雀/降级候选的成功与终态失败）+ 备模型候选过滤 + 进程级装配（outlier.enabled opt-in）；分类感知（failureCategories 默认 NETWORK/SERVER/TIMEOUT——AUTH/CONTENT 驱赶端点无意义，熔断 failure-categories 同口径）
- [R11 验收](../tickets/T2372-r11-outlier-wire-verify.md) — 分类过滤四断言（默认集/自定义集/大小写/成功复位）+ 装配转换 + resilience 377 用例零回归

- [R12 形状：孤类普查+熔断遥测接线](../tickets/T2373-r12-census-shape.md) — R11 模式推广：全仓普查确认孤类 15 项（19 类）+疑似 6 项入档 spec 1611；本轮修复 resilience 域两项（CircuitCrashLoopDetector/ HalfOpenProbeStats——withTelemetry 注入 + 跳闸/恢复/半开探测喂点 + 装配恒挂）
- [R12 验收](../tickets/T2374-r12-census-verify.md) — 遥测接线三断言（crash-loop 闩锁语义/半开探测成败计数/未注入零行为）+ resilience 380 用例零回归

- [R13 形状：guard 孤类装配面](../tickets/T2375-r13-guard-orphans-shape.md) — spec 1611 孤类修复第二弹：ToolRoleGuardHook（141 角色权限）/InputFloodGuardHook（167 泛洪防护）自 Builder 声明即注册（此前 GuardModule 无装配路径），默认未声明零注册
- [R13 验收](../tickets/T2376-r13-guard-orphans-verify.md) — 装配三断言（permissions 声明注册/泛洪配置注册/默认双双零注册）+ guard 334 用例零回归

- [R14 形状：目录漂移看门狗接线](../tickets/T2377-r14-drift-shape.md) — spec 201 孤类修复第三弹：CatalogDriftHolder 进程级基线（RetryBudgetHolder 模式）+ HarnessAssembler 会话构造节拍拍指纹（首拍建基线，变化 WARN+计数；包装层不改指纹——只捕目录语义变化）
- [R14 验收](../tickets/T2378-r14-drift-verify.md) — 三会话序列集成断言（建基线零事件/目录变化一事件含增删明细/稳定无事件）+ Holder 直喂面

- [R15 形状：指标新鲜度接线](../tickets/T2379-r15-freshness-shape.md) — spec 802 孤类修复第四弹：metrics 装配链恒包 MetricFreshnessTracker（有界 512 名纯旁路）+ MetricFreshnessHolder 静态 audit 面
- [R15 验收](../tickets/T2380-r15-freshness-verify.md) — 装饰写入 touch + audit 报陈旧（active/dead 分离年龄断言）+ 未装配 empty + 既有 6 用例零回归

- [R16 形状：泄漏聚合接线](../tickets/T2381-r16-leakagg-shape.md) — spec 839 孤类修复第五弹：LeakSuspectHolder.compositeWith 把聚合器复合进检测器 listener 链（宿主 listener 与聚合器都收），装配处一行替换
- [R16 验收](../tickets/T2382-r16-leakagg-verify.md) — 复合双收断言（host 3 次 + 聚合排行 count/maxAge）+ null 宿主仅聚合器 + 既有 4 用例零回归

- [R17 形状：工具失败负缓存](../tickets/T2383-r17-negcache-shape.md) — DNS negative caching / NXDOMAIN 短 TTL 思想：同 key（工具名+argsHash）失败短 TTL 记忆（默认 30s——恢复窗口即 TTL，短窗纪律），窗内复读直接回错误文本不再真调；成功不缓存（与 spec 183 成功 memo 正交）；异常路径同缓存；测试暴露语义缺陷（成功清除机制在 TTL 短路下永远不可达）后删简化为纯 DNS 语义
- [R17 验收](../tickets/T2384-r17-negcache-verify.md) — 四断言（失败缓存窗内拦截真调一次/过期放行/异常同缓存/不同参数独立 key）

- [R18 形状：梯度式自适应并发](../tickets/T2385-r18-gradient-shape.md) — Netflix Gradient2 / Envoy adaptive_concurrency 思想：延迟梯度驱动（baseline/recent 双 EMA）——劣化乘性下调（失败前规避）、变快加性上调、容错带防抖、warmup 学习期；与 spec 145 失败驱动 AIMD 正交
- [R18 验收](../tickets/T2386-r18-gradient-verify.md) — 七断言（劣化下调零失败/变快加性/容差带不动/warmup 只学/上限联动/封顶/配置校验）

- [R19 形状：校准审计接线](../tickets/T2387-r19-calib-shape.md) — spec 819 孤类修复第六弹：TokenBudgetHook.afterModel 同点对账（CharHeuristic 估算 prompt vs usage.promptTokens）+ CalibrationAuditHolder 读出面——「预算按估算设、账单按真实来」的偏差从感觉变数字
- [R19 验收](../tickets/T2388-r19-calib-verify.md) — Holder 读数两断言 + 既有预算/校准 12 用例零回归

- [R20 形状：spill 写速率限速](../tickets/T2389-r20-ratelimit-shape.md) — RocksDB rate limiter 思想：令牌桶节流（bytes/s + burst 突发容忍 + maxWait 软限速超时放行 degraded 计数——限速器故障不放大成 spill 失败）；ReentrantLock+Condition（虚拟线程 unmount）；opt-in null=关
- [R20 验收](../tickets/T2390-r20-ratelimit-verify.md) — 五断言（burst 吸收/超速节流/超时放行/关闭零开销/集成写不破）+ spill 180 用例

- [R21 形状：空闲监控全链接线](../tickets/T2391-r21-idle-shape.md) — spec 179/841 双孤类+喂数面 SessionFeaturesHook（spec 161，本身也未装配）三件一次接线：IdleMonitorHolder 进程级（store/monitor/histogram）+ SessionFeaturesHook afterTurn 每 32 轮节拍 sweep + 装配 bean 默认开（纯记账旁路）
- [R21 验收](../tickets/T2392-r21-idle-verify.md) — 全链三断言（sweep 判空闲+翻转通知+直方入账/Holder 便捷面/hook 喂数）+ 既有 12 用例零回归

- [R22 形状：会话检疫装配](../tickets/T2393-r22-quarantine-shape.md) — spec 143 双孤类（SessionQuarantine+Hook）接线：opt-in buzhou.quarantine.enabled（默认关——检疫 block 轮次行为面大），阈值/退避可配（3/30s/10m 缺省），成功复位留公共 API（hook 面不谎装——原设计诚实边界）
- [R22 验收](../tickets/T2394-r22-quarantine-verify.md) — hook 行为两断言（三连败隔离 block + 冷却过放行 / 健康会话零状态）+ 既有 6 用例零回归

- [R23 形状：对账 NPE 修复](../tickets/T2395-r23-npe-shape.md) — R19 引入的 request()=null NPE（M 会话 R15 记档归属本会话）：对账前置三重 null 防御（request/prompt/instructions 缺席跳过——测试替身链路）
- [R23 验收](../tickets/T2396-r23-npe-verify.md) — CounterAtomicitySpreadTest 恢复绿 + 校准/预算 7 用例零回归

- [R24 形状：影子探针接线](../tickets/T2397-r24-shadow-shape.md) — spec 189 孤类接线：主路成功后确定性采样对照首个备模型（Istio mirror 思想——「备模型若被启用结果是否一致」容量预案信心面）；deadlineExecutor submit 即忘 + REE 关闭竞态防护；fallback.shadow-probe-percent（0=关默认）
- [R24 验收](../tickets/T2398-r24-shadow-verify.md) — 四断言（一致/分歧双计数+分歧样本环、确定性采样、零率零执行、影子故障计 error 不抛）+ resilience 384 用例

- [R25 形状：HITL 豁免征询](../tickets/T2399-r25-exempt-shape.md) — GuardExemptionRegistry（spec 820）首个消费者：危险工具 hook 在授权检查后征询（mechanism=dangerous-tool、subject=工具名）——未过期豁免放行+审计事件，过期/撤销/无豁免恢复确认流程；GuardModule.exemptions() 暴露 grant/revoke 面
- [R25 验收](../tickets/T2400-r25-exempt-verify.md) — 四断言（有效豁免放行/无与过期仍 block/撤销恢复 block/模块暴露 registry）+ guard 343 用例

- [R26 形状：泄漏金丝雀接线](../tickets/T2401-r26-canary-shape.md) — spec 528 孤类接线：SessionCanaryHook（beforeTurn 种植确定性令牌 + afterModel 输出扫描——他会话令牌即泄漏事件）；令牌注入面留宿主（honeytoken 需放进数据才可触发——诚实边界随原注）；LayeredPolicy（1003）裁决纯函数工具豁免不清亡
- [R26 验收](../tickets/T2402-r26-canary-verify.md) — 两断言（他会话令牌→泄漏事件+自会话回显不算/种植确定性）+ registry 既有 4 用例零回归

- [R27 形状：中期对账审计](../tickets/T2403-r27-audit-shape.md) — 26 轮跨 6 模块改动首跑隔离 worktree 全仓 verify：唯一红=API 快照非破坏新增 10 类 → worktree 再生修复+md 入档；工件对账补 spec1622 悬空引用；全部 16xx spec/README/票/impl 双向实存
- [R27 验收](../tickets/T2404-r27-audit-verify.md) — ApiSurfaceSnapshotTest+SpecCoverageTest 双绿（worktree 实证）+ 对账清单入档

- [R28 形状：PII 豁免双粒度](../tickets/T2405-r28-pii-shape.md) — 820 第二消费者：工具级（该工具输出整体豁免短路）+ 类型级（type:TYPE 从生效集剔除、其余类型照脱）——「规则误报已核验」与「该数据源可信」两种生产痛点各得其所
- [R28 验收](../tickets/T2406-r28-pii-verify.md) — 三断言（工具级原样透传/类型级 EMAIL 脱敏 PHONE 保留/无豁免基线全脱）+ guard 359 用例

- [R29 形状：熔断慢调用维度](../tickets/T2407-r29-slow-shape.md) — resilience4j slow call rate 思想：withSlowCallPolicy(duration,rate) 链式注入（不扩 Config——零配置零行为），慢样本环形窗与失败窗并行，慢率或失败率任一达界开闸；无时长入账不计慢（既有语义零变化）；advisor 主路径 nanoTime 喂入
- [R29 验收](../tickets/T2408-r29-slow-verify.md) — 五断言（未注入零行为/慢率开闸零失败前提/快调用不触发/无时长面不计慢/参数校验）+ resilience 389 用例

- [R30 形状：输入边界四护栏](../tickets/T2409-r30-bounds-shape.md) — Envoy HTTP/2 SETTINGS_MAX_* 思想：body 64K（超长走 bodyPath Onload 通道带指引）/URL 8K/头数量 64/单头值 8K——模型自报超长输入不进执行层；单头超限独立异常不计失败桶
- [R30 验收](../tickets/T2410-r30-bounds-verify.md) — 五断言（四护栏各拒入桶+合规输入零影响）+ tools 118 用例

- [R31 形状：Wilson 置信区间](../tickets/T2411-r31-wilson-shape.md) — 统计报告标准工具（小样本/极端比例不越界不出负值——正态近似的经典缺陷）：ab.run.completed 事件加 winRateA 95% CI（decided 口径分母），与 SPRT 决策面互补的报告面
- [R31 验收](../tickets/T2412-r31-wilson-verify.md) — 四断言（含点估计/极端不越界/小样本宽于大样本/退化零区间）+ Pairwise 回归

- [R32 形状：退避抖动模式](../tickets/T2413-r32-jitter-shape.md) — AWS「Exponential Backoff and Jitter」思想：JitterMode 可配（EQUAL=既有 ±j 对称/FULL=[0,cap] 全随机防同步最优/DECORRELATED=[base,min(cap,prev×3)] 去相关），withJitterMode 链式 + yml jitter-mode；默认 EQUAL 零行为
- [R32 验收](../tickets/T2414-r32-jitter-verify.md) — 四断言（EQUAL 带内/FULL 全区间有落点/DECORRELATED prev×3 界/解析 fail-fast）+ resilience 393 用例

- [R33 形状：输入侧 PII 豁免](../tickets/T2415-r33-piiin-shape.md) — 820 第三消费者：PiiInputRedactionHook 双粒度（会话级 subject=sessionId「内部已合规通道」+ 类型级 type:TYPE 生效集剔除）——mechanism 域分侧（pii-input-redaction）与输出侧独立豁免；含 J 会话 DangerousToolStatsTest 脱锚解卡（import/包路径/yml 形态三处）
- [R33 验收](../tickets/T2416-r33-piiin-verify.md) — guard 368 用例全绿（含解卡后的 J 系 4 用例 + 输入侧豁免回归）

- [R34 形状：负缓存装配面](../tickets/T2417-r34-negcache-shape.md) — NegativeCachingHolder（进程级开关默认关 + TTL 可调）+ HarnessAssembler 全工具包装链（未启用原引用透传零开销）——spec 1616 装饰器自宿主 wrap 升级为开关装配
- [R34 验收](../tickets/T2418-r34-negcache-verify.md) — 两断言（关透传同引用/开包装 TTL 拦截+会话装配链不破坏）+ exec 包 249 用例

- [R35 形状：dashboard gzip](../tickets/T2419-r35-gzip-shape.md) — writeJson 客户端协商 gzip（Accept-Encoding 含 gzip 且响应 ≥512B 才压——阈值下压缩头倒挂）；Content-Encoding 头+体可解压回 JSON
- [R35 验收](../tickets/T2420-r35-gzip-verify.md) — 两断言（协商压解回/无协商恒明文）+ dashboard 34 用例

- [R36 形状：失败项重跑](../tickets/T2421-r36-rerun-shape.md) — run 加 onlyItemIds 子集重载（null=全量零变化）：上轮 fail/error 的 id 传入即 rerun-failed——CI 红了只重跑失败项省时 + flaky 区分（重跑过=flaky、仍败=真回归）；汇总/落盘口径不变（total=子集数）
- [R36 验收](../tickets/T2422-r36-rerun-verify.md) — 两断言（失败子集新 runId total=1/null 全量 3）+ eval 包 245 用例

- [R37 形状：校准系数建议](../tickets/T2423-r37-factor-shape.md) — 偏差读数可操作化：meanRelativeError 一阶换算修正系数（1/(1+e)，高估<1 调低），样本不足/零偏差 empty（不基于噪声给建议）
- [R37 验收](../tickets/T2424-r37-factor-verify.md) — 三断言（高估 0.8 界/低估 >1/不足与零偏差 empty）+ 校准域 10 用例

- [R38 形状：慢调用 yml 装配](../tickets/T2425-r38-slowyml-shape.md) — Circuit 组扩参 slow-call-duration/slow-call-rate-threshold（语义归位熔断组而非顶层 19 参；rate 缺省 0.5 与失败率阈同档；null=维度关）+ Module withSlowCallPolicy 传导
- [R38 验收](../tickets/T2426-r38-slowyml-verify.md) — 三断言（组归一与缺省/非法 fail-fast/装配端到端 OPEN）+ resilience 396 用例

- [R39 形状：金丝雀 yml 装配](../tickets/T2427-r39-canaryyml-shape.md) — buzhou.guard.leak-canary.salt 声明即启用（salt 防离线推演建议环境变量注入）；编程面 spec 1625 已验，此处补 autoconfig 传导与装配产物复验
- [R39 验收](../tickets/T2428-r39-canaryyml-verify.md) — 两断言（salt 装配链种植检出+自回显不算/无 salt 零 hook）+ guard 370 用例

- [R40 形状：梯度限流器观测接线](../tickets/T2429-r40-gradient-shape.md) — spec 1617 装配面：executeToolCalls 批耗时经 GradientLimiterHolder 喂入（观测先行不接 tryAcquire 闸——批时延是全局工具路径负载天然信号，闸接入待数据积累独立裁决）
- [R40 验收](../tickets/T2430-r40-gradient-verify.md) — 两断言（Holder 喂入 View 读数/install 替换重置）+ 梯度域 9 用例

- [R41 形状：PII 豁免计数](../tickets/T2431-r41-piicount-shape.md) — PiiHitStats.recordExemption（工具级+类型级合并口径——与命中统计对照面：「豁免了多少 vs 命中了多少」），reset 同步归零；J 系危险工具 exemptedSkips 对齐补全
- [R41 验收](../tickets/T2432-r41-piicount-verify.md) — 豁免路径计数断言 + guard 370 用例

- [R42 形状：负缓存 yml 装配](../tickets/T2433-r42-negyml-shape.md) — buzhou.core.negative-cache.{enabled,ttl}（DisposableBean 关闭钩子停用——已包装会话缓存自然过期）——spec 1633 Holder 补配置面
- [R42 验收](../tickets/T2434-r42-negyml-verify.md) — 三断言（启用+关闭停用/ttl 生效/缺省零 bean）

- [R43 形状：N 系运维手册段](../tickets/T2435-r43-runbook-shape.md) — ops-runbook 第 23 节四族（缓存限流/熔断降级/护栏豁免/工具观测）——16xx 机制的配置键、观测读数与失控信号运维面集中入档
- [R43 验收](../tickets/T2436-r43-runbook-verify.md) — 段落完整性（四族覆盖 spec 1600-1641 全部可运维机制）

- [R44 形状：流式 PII 类型级豁免](../tickets/T2437-r44-pstream-shape.md) — 820 第四消费者：replyStreamFilter 创建时生效集剔除（type:TYPE）；StreamTextFilter SPI 无会话上下文——会话级豁免不适用流式面（诚实边界）；豁免族四消费者闭环
- [R44 验收](../tickets/T2438-r44-pstream-verify.md) — 两断言（豁免类型原文保留其余照脱/无豁免双脱）+ guard 372 用例

- [R45 形状：@since 补全](../tickets/T2439-r45-since-shape.md) — N 系 15 个新公开类型 Javadoc 补 @since 1.0.0（api 子包语义版本承诺规范——api-surface 入档类型的文档一致性）
- [R45 验收](../tickets/T2440-r45-since-verify.md) — 15/15 覆盖 + 五模块编译绿（spill 测试挂为并行会话 .m2 旧 jar 域）

- [R46 形状：快照增量再生](../tickets/T2441-r46-snapshot-shape.md) — R27 后新增 GradientLimiterHolder 等类型快照再生（worktree -am）；同文件并行冲突化解（GuardModule.dangerousTools() 双方同时加——M 系版本保留我方撤）
- [R46 验收](../tickets/T2442-r46-snapshot-verify.md) — ApiSurfaceSnapshotTest 门绿（worktree 实证）+ guard 376 用例（冲突化解后）

- [R47 形状：MAP 刷新与台账终核](../tickets/T2443-r47-maprefresh-shape.md) — MAP N 系行状态更新（收口中+成果摘要）；台账 46 行无缺号、票 57/impl 50 落盘核对
- [R47 验收](../tickets/T2444-r47-maprefresh-verify.md) — 台账 gaps=[] + 票/impl 双向实存

- [R48 形状：终验启动](../tickets/T2445-r48-final-shape.md) — 隔离 worktree 全仓 mvn verify 后台启动（R47 工件终核的对账面已在位；验证面结果 R50 回填）
- [R48 验收](../tickets/T2446-r48-final-verify.md) — verify EXIT 码与失败清单（若有）回填 spec 1648

## Not yet specified

- R2+ 选题池（借签思想候选，逐轮裁决）：Caffeine refresh-ahead、Envoy retry precedence、Kafka ISR 健康视图、Tokio coop budget、Postgres autovacuum 式后台整理、Bazel flaky 检出、RocksDB rate limiter……按当轮代码现状取「小而完整」者优先。

## Out of scope

- 修改既有号段（900/1200/1400/1500 系）的 spec 与票。
- perf 测试组激活（nightly 语义不变）。

## 台账（N 会话轮次）

| 轮 | effort | 主题 | 票 | impl | spec | 状态 |
|---|---|---|---|---|---|---|
| R1 | #1600 | 语义缓存 LFU 采样驱逐（Redis allkeys-lfu 思想） | T2351–T2352 | 1153 | 1600 | done |
| R2 | #1601 | MCP 连接最大寿命（HikariCP maxLifetime 思想） | T2353–T2354 | 1154 | 1601 | done |
| R3 | #1602 | 熔断启动宽限期（K8s startupProbe 思想） | T2355–T2356 | 1155 | 1602 | done |
| R4 | #1603 | http_request per-host 并发上限（Nginx limit_conn 思想） | T2357–T2358 | 1156 | 1603 | done |
| R5 | #1604 | 响应缓存 stale-if-error（Varnish grace / RFC 5861 思想） | T2359–T2360 | 1157 | 1604 | done |
| R6 | #1605 | A/B 评估 SPRT 序贯提前终止（Wald SPRT / sequential testing 思想） | T2361–T2362 | 1158 | 1605 | done |
| R7 | #1606 | 虚拟线程 pinning 审计 + 金丝雀热路径修复（Netty 不阻塞事件循环铁律） | T2363–T2364 | 1159 | 1606 | done |
| R8 | #1607 | RollingJsonlWriter 锁迁移（spec 1606 排队项：j.u.c 锁不 pin） | T2365–T2366 | 1160 | 1607 | done |
| R9 | #1608 | DiskSpillStore 锁迁移（spec 1606 排队项） | T2367–T2368 | 1161 | 1608 | done |
| R10 | #1609 | WebhookOutbox 锁迁移（spec 1606 中危 #1：dispatcher 虚拟线程放大） | T2369–T2370 | 1162 | 1609 | done |
| R11 | #1610 | 离群驱逐生产接线 + 分类感知（spec 149 孤类救活） | T2371–T2372 | 1163 | 1610 | done |
| R12 | #1611 | 孤类普查（15 项入档）+ 熔断遥测接线（spec 811/836 喂点落地） | T2373–T2374 | 1164 | 1611 | done |
| R13 | #1612 | guard 孤类装配面（spec 141/167 两 hook 救活） | T2375–T2376 | 1165 | 1612 | done |
| R14 | #1613 | 工具目录漂移看门狗接线（spec 201 孤类救活） | T2377–T2378 | 1166 | 1613 | done |
| R15 | #1614 | 指标新鲜度追踪接线（spec 802 孤类救活） | T2379–T2380 | 1167 | 1614 | done |
| R16 | #1615 | 泄漏疑似聚合接线（spec 839 孤类救活） | T2381–T2382 | 1168 | 1615 | done |
| R17 | #1616 | 工具失败负缓存（DNS negative caching 思想） | T2383–T2384 | 1169 | 1616 | done |
| R18 | #1617 | 梯度式自适应并发闸（Netflix Gradient2 思想） | T2385–T2386 | 1170 | 1617 | done |
| R19 | #1618 | Token 校准审计接线（spec 819 孤类救活） | T2387–T2388 | 1171 | 1618 | done |
| R20 | #1619 | spill 写速率限速（RocksDB rate limiter 思想） | T2389–T2390 | 1172 | 1619 | done |
| R21 | #1620 | 空闲监控全链接线（spec 161/179/841 三孤类救活） | T2391–T2392 | 1173 | 1620 | done |
| R22 | #1621 | 会话隔离检疫装配（spec 143 双孤类救活） | T2393–T2394 | 1174 | 1621 | done |
| R23 | #1622 | R19 对账 NPE 修复（跨会话记档承接） | T2395–T2396 | 1175 | 1622 | done |
| R24 | #1623 | 影子读探针接线（spec 189 孤类救活，Istio mirror 思想） | T2397–T2398 | 1176 | 1623 | done |
| R25 | #1624 | 危险工具 HITL 豁免征询（spec 820 孤类首个消费者） | T2399–T2400 | 1177 | 1624 | done |
| R26 | #1625 | 跨会话泄漏金丝雀接线（spec 528 孤类救活） | T2401–T2402 | 1178 | 1625 | done |
| R27 | #1626 | 中期对账审计（全仓 verify + API 快照再生 + 工件对账） | T2403–T2404 | 1179 | 1626 | done |
| R28 | #1627 | PII 脱敏豁免双粒度（820 第二消费者） | T2405–T2406 | 1180 | 1627 | done |
| R29 | #1628 | 熔断慢调用率维度（resilience4j slow call rate 思想） | T2407–T2408 | 1181 | 1628 | done |
| R30 | #1629 | http_request 输入边界四护栏（Envoy SETTINGS_MAX_* 思想） | T2409–T2410 | 1182 | 1629 | done |
| R31 | #1630 | A/B 胜率 Wilson 置信区间 | T2411–T2412 | 1183 | 1630 | done |
| R32 | #1631 | 退避抖动模式可配（AWS full/decorrelated jitter 思想） | T2413–T2414 | 1184 | 1631 | done |
| R33 | #1632 | 输入侧 PII 豁免（820 第三消费者）+ J 系测试解卡 | T2415–T2416 | 1185 | 1632 | done |
| R34 | #1633 | 负缓存装配面（spec 1616 装饰器 Holder 化） | T2417–T2418 | 1186 | 1633 | done |
| R35 | #1634 | dashboard 响应 gzip（客户端协商） | T2419–T2420 | 1187 | 1634 | done |
| R36 | #1635 | eval 失败项重跑（rerun-failed） | T2421–T2422 | 1188 | 1635 | done |
| R37 | #1636 | 校准系数建议（偏差读数可操作化） | T2423–T2424 | 1189 | 1636 | done |
| R38 | #1637 | 慢调用维度 yml 装配（circuit 组扩参） | T2425–T2426 | 1190 | 1637 | done |
| R39 | #1638 | 泄漏金丝雀 yml 装配（spec 1625 配置面补全） | T2427–T2428 | 1191 | 1638 | done |
| R40 | #1639 | 梯度限流器观测接线（spec 1617 装配面） | T2429–T2430 | 1192 | 1639 | done |
| R41 | #1640 | PII 豁免计数（与命中统计对照面） | T2431–T2432 | 1193 | 1640 | done |
| R42 | #1641 | 负缓存 yml 装配（spec 1633 配置面补全） | T2433–T2434 | 1194 | 1641 | done |
| R43 | #1642 | N 系运维手册段（ops-runbook 第 23 节） | T2435–T2436 | 1195 | 1642 | done |
| R44 | #1643 | 流式 PII 类型级豁免（820 第四消费者，豁免族闭环） | T2437–T2438 | 1196 | 1643 | done |
| R45 | #1644 | 新公开类型 @since 补全（Javadoc 规范） | T2439–T2440 | 1197 | 1644 | done |
| R46 | #1645 | API 快照增量再生 + 同文件并行冲突化解 | T2441–T2442 | 1198 | 1645 | done |
| R47 | #1646 | MAP 刷新与台账终核 | T2443–T2444 | 1199 | 1646 | done |
| R48 | #1647 | 终验启动（隔离 worktree 全仓 verify） | T2445–T2446 | 1200 | 1647 | done |
| R49 | #1648 | 收口 spec 与台账封卷 | T2447–T2448 | 1201 | 1648 | done |
