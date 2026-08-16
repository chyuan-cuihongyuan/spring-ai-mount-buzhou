# Wayfinder Map — Buzhou 语义缓存（effort #15）

> effort #15，延续 #5–#14（累计 151 轮 / T1–T230 / impl 1–190）。
> 本 effort 主线：**语义缓存（Semantic Cache）**——在精确响应缓存（spec 53）之上加
> embedding 相似度命中层：同义问法命中已有答案（FAQ/评估 run/固定前缀负载省模型调用）；
> 借鉴 LiteLLM（~26K★）semantic caching（embedding 相似度阈值判定）；进程内向量存储
> （LiteLLM 亦支持 local 模式）。默认关闭（opt-in）；机制正确性与嵌入质量分离诚实入档。

## Destination

`buzhou.resilience.semantic-cache.enabled=true` + EmbeddingModel bean 时：同义问法
（embedding 相似度 ≥ 阈值）命中已缓存的终态答案，零模型调用；精确缓存（零成本）优先、
语义缓存（嵌入成本）在后；否定句/跨模型/参数变化不误命中（红队钉住）；成本口径
（省模型调用 vs 花嵌入调用）runbook 写明；默认关闭零行为变化。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6–#14 MAP Notes。
- 外部事实源（LiteLLM ~26K★ semantic caching）：查询 embedding + 相似度阈值判定
  （默认阈值口径 0.95 量级）；支持 Redis 向量与 local 模式；false-positive 风险由
  threshold 控制。**本地裁定**：进程内向量线性扫描（Redis 向量需 RediSearch 模块——
  非标准 Redis，诚实 out-of-scope）；条目量级数百（FAQ 型负载）线性扫描足够，perf
  哨兵钉量级。
- 本地勘察（2026-08-17）：spring-ai-model 2.0.0 已在 resilience 编译类路径
  （spring-ai-client-chat 传递）——`org.springframework.ai.embedding.EmbeddingModel`
  接口可直接注入，无需新依赖；ResponseCacheAdvisor/Store/Keys（spec 53）为叠加基座
  （终态判定 `isTerminal` 复用；options 采样口径复用）。
- 过程教训沿用：多构造器 record @ConstructorBinding；examples 依赖改动后全量 install
  校验；新键必须登记绑定矩阵（T214 防线）——本 effort 真有新键（semantic-cache.* 四键）。

## Decisions so far

- **进程内向量先行**：桶内线性 cosine 扫描（LiteLLM 亦支持 local 模式）；Redis/RediSearch
  向量需非标准模块——诚实 out-of-scope，量级由 perf 哨兵钉住（128×384 满桶 <1ms 预期）。
- **位序 = 精确缓存之后**（+460 vs +450）：零成本层先短路，嵌入成本只在精确 miss 后发生。
- **分桶 = modelName + options 采样**（ResponseCacheKeys 同口径）：跨模型/参数变体不进入
  相似度比较；桶键不是精确键（messages 不入桶键——语义命中本体）。
- **嵌入文本 = messages 全文拼接（role 不入）**：问法语义与角色无关；memory 注入后视图。
- **机制与判别力分离（核心诚实边界）**：框架保证阈值（≥ 含边界）/分桶/终态边界正确；
  「X」vs「不是 X」判别力归嵌入模型——红队否定对钉住（相近嵌入下框架按阈值诚实命中），
  残余风险由默认关闭 + 高阈值 + 适用面自律承担。
- **嵌入故障旁路降级（非 fail-fast）**：嵌入查询/写入失败 → 该调用旁路直通 + bypass
  计数——降级只损失一次可能命中，不损正确性（与限流 fail-fast 语义刻意不同，入档）。
- **enabled 而无 bean = 配置错误 fail-fast 带修法**（与 shadow 同口径）；绑定矩阵以
  stub EmbeddingModel 走 enabled=true 全路径（不弱化矩阵）。
- **写入边界复用 isTerminal**（带 toolCalls 不缓存——spec 53 §B 工具副作用安全沿用）。

## Not yet specified

- Redis 向量存储（RediSearch）与跨实例共享语义缓存（单进程内存向量先行；量级证据后议）。
- 共享熔断状态 / 配额扣减跨实例原子化（#14 勘察：session 配额计数已在共享 state store，
  缺的是并发扣减竞差修复——独立 effort 议）。
- outbox SCAN 下推 / 观测 OLAP / skill 语义排序（沿用 fog）。

## Out of scope

- 沿用 effort #7–#14 Out of scope 全部条目。
- Redis/RediSearch 向量后端（非标准 Redis 模块依赖；进程内先行）。
- 嵌入模型内置实现（Buzhou 不内置 embedding provider——宿主注入 Spring AI EmbeddingModel）。

## Tickets

初始 9 张（T240–T248，均含 AFK 决议，按轮逐张闭合；2026-08-17 全闭合）：

- [x] [T240 语义缓存存储 + cosine 判定 + LRU/TTL](tickets/T240-semantic-store.md)（impl-191）
- [x] [T241 SemanticCacheAdvisor（命中短路/透传/聚合写/位序）](tickets/T241-semantic-advisor.md)（impl-191）
- [x] [T242 EmbeddingModel 装配 + 配置参数组 + metadata 键](tickets/T242-semantic-wiring.md)（impl-192）
- [x] [T243 语义红队（否定对/桶隔离/参数敏感/无 bean fail-fast/终态边界）](tickets/T243-semantic-redteam.md)（impl-193；6 项含流式 E2E）
- [x] [T244 流式语义命中重放 E2E](tickets/T244-semantic-stream.md)（impl-193；随红队文件落）
- [x] [T245 perf 哨兵（嵌入+cosine+查桶开销）+ baseline](tickets/T245-semantic-perf.md)（impl-194；首轮值待 nightly）
- [x] [T246 演示（FAQ 同义问命中）+ examples](tickets/T246-semantic-demo.md)（impl-195）
- [x] [T247 文档面 + 绑定矩阵登记（semantic-cache.* 四键）+ 快照](tickets/T247-semantic-docs.md)（impl-195；矩阵含 enabled=true 全路径）
- [x] [T248 里程碑 verify + 收口](tickets/T248-effort15-closing.md)（全仓 verify + MAP 闭合；累计 160 轮）
