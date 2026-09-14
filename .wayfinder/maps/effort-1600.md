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
