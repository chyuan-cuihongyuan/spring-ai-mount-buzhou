# Wayfinder Map — Buzhou 精确响应缓存（effort #12）

> effort #12，延续 #5–#11（累计 123 轮 / T1–T202 / impl 1–167）。
> 本 effort 主线：**LLM 精确响应缓存（Exact Response Cache）**——advisor 层拦截、
> 请求规范化键、终态响应缓存（无 toolCalls）、LRU+TTL 进程内后端、命中/未命中可观测；
> 默认关，开启零行为回归。
> 到达 = 11 轮自迭代落地、全仓 verify 绿、防线（红队/perf）与文档齐备、MAP 闭合。

## Destination

同请求（model + 注入后 messages + 采样 options）二次调用零模型开销命中；
缓存行为全可观测（hit/miss/evicted）；带 toolCalls 的中间态响应不缓存（工具副作用安全）；
默认关 = 现有行为零变化；配置 fail-fast 沿用。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6/#9/#10/#11 MAP Notes。
- 外部研究（2026-08-16，只认 ≥10K★）：LiteLLM（~26K★）response caching——cache key =
  请求参数哈希（messages/prompt 入键）；后端 in-memory/Redis/DualCache（L1 内存 + L2 分布式）；
  per-call 控制 no-cache/no-store/ttl/namespace；流式完整组装后缓存、命中重放。本地收窄：
  V1 只做进程内精确缓存（Redis 层 = fog）；per-call 控制暂不做（Spring AI Prompt 透传面
  窄，fog）；**新增本地裁定：带 toolCalls 的响应不缓存**（工具副作用安全——LiteLLM 代理层
  无此约束，agent harness 有）。
- 本地勘察（2026-08-16）：CachedEmbeddingProvider 先例（LRU 单锁 + sha256 键 + hit/miss
  计数可观测——同风格复用）；advisor 层为本地拦截惯例（ObservabilityAdvisor +500 /
  ResilienceAdvisor 既有 order）；ChatClientRequest.messages() = memory 注入后视图
  （键基于它即含会话历史）；fog「语义边界未清」裁定——**只做精确缓存**（同键同回复），
  语义缓存（embedding 相似命中）继续 fog。
- 过程教训沿用：多构造器嵌套 record 显式 @ConstructorBinding（T187）；下游单跑 -am；
  断言以端上计数为准；勘察可能有误判实现期纠偏入档。

## Decisions so far

- [T203 缓存 advisor 与键](tickets/T203-cache-advisor-key.md) — order +450 命中短路 observability/resilience；sha256 规范键（options 采样近似性入档）；spec 53 §A。
- [T204 写入边界](tickets/T204-cache-write.md) — isTerminal 公开钉住（无 toolCalls 非空）；本地裁定 vs LiteLLM 入档；spec 53 §B。
- [T205 流式路径](tickets/T205-cache-stream.md) — 命中 Flux.just 重放；未命中 doOnComplete 聚组装（取消不写半截）；spec 53 §C。
- [T206 LRU+TTL](tickets/T206-cache-ttl.md) — 惰性过期（可注入 Clock）；过期不返回陈旧；spec 53 §D。
- [T207 计数与装配](tickets/T207-cache-metrics.md) — ResponseCache 第 14 组件（单构造器防盲区）；默认关零注入；metadata 3 键；resilience 113 绿；spec 53 §E。
- [T208 缓存面红队](tickets/T208-cache-redteam.md) — 元字符注入不串键/过期不陈旧/热键存活/重放只读四用例。
- [T209 缓存面 perf 哨兵](tickets/T209-cache-perf.md) — 命中路径/键计算/流式重放三哨兵（首轮 <3ms）+ baseline 落档。
- [T210 缓存演示](tickets/T210-cache-demo.md) — 同问二调零模型调用 + miss 真调宿主视角。
- [T211 runbook 第八轮](tickets/T211-runbook-8.md) — §8 响应缓存运营（适用/不适用/红线/排查）。
- [T212 CONTEXT/api-surface + 元数据](tickets/T212-context-api-12.md) — 术语节 4 条 + 公共面 + 3 键入档；破坏性变更 1 处（canonical 13→14）。
- [T213 里程碑 verify + 收口](tickets/T213-effort12-closing.md) — 全仓 18 模块 1259 测试全绿；**effort#12 到达判定达成：11/11 票（impl 168–177）**；累计 134 轮（T1–T213，impl 1–177）核对一致。

## Not yet specified

- 语义缓存（embedding 相似命中；依赖与边界双重未清，沿用 fog）。
- Redis/分布式缓存层（多实例共享；LiteLLM DualCache 语义已研究，多实例边界沿用）。
- per-call 缓存控制（no-cache/no-store 透传面；Spring AI options 通道窄）。
- skill 语义排序 / outbox Redis SCAN 下推 / 观测 OLAP / store 静态加密（沿用 fog）。

## Out of scope

- 沿用 effort #7–#11 Out of scope 全部条目。
- 缓存穿透/击穿防护（单进程低并发场景不过度设计；压测证据出现再议）。
- 缓存持久化（进程重启重建；评估场景 run 内命中已覆盖主收益）。

## Tickets

初始 11 张（T203–T213，均含 AFK 决议，按轮逐张闭合）：

- [T203 ResponseCacheAdvisor 骨架与键设计](tickets/T203-cache-advisor-key.md)
- [T204 终态判定与缓存写入（无 toolCalls 边界）](tickets/T204-cache-write.md)（blocked-by T203）
- [T205 stream 路径缓存（组装后缓存+命中重放）](tickets/T205-cache-stream.md)（blocked-by T203）
- [T206 LRU+TTL 容量与惰性过期](tickets/T206-cache-ttl.md)（blocked-by T203）
- [T207 计数可观测与配置装配](tickets/T207-cache-metrics.md)（blocked-by T203；response-cache 组 + 默认关零变化）
- [T208 缓存面红队对抗](tickets/T208-cache-redteam.md)（blocked-by T204/T206）
- [T209 缓存面 perf 哨兵](tickets/T209-cache-perf.md)（blocked-by T205）
- [T210 examples 缓存演示](tickets/T210-cache-demo.md)（blocked-by T205/T207）
- [T211 runbook 第八轮](tickets/T211-runbook-8.md)（blocked-by T207）
- [T212 CONTEXT/api-surface + 元数据 + 绑定验证](tickets/T212-context-api-12.md)（blocked-by T203–T207）
- [T213 里程碑 verify + 收口](tickets/T213-effort12-closing.md)（blocked-by T208–T212）
