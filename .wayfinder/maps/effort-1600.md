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
