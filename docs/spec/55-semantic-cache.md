# Spec 55 — 语义缓存（effort #15）

> effort #15 唯一篇。§A：SemanticCacheStore + cosine 判定 + LRU/TTL（T240）；
> §B：SemanticCacheAdvisor 命中短路/透传/聚合写/位序（T241）；§C：EmbeddingModel 装配 +
> 配置参数组（T242）；§D：红队（T243）；§E：流式重放 + perf + 演示 + 文档（T244–T247）。
> 外部事实源：LiteLLM（~26K★）semantic caching——查询 embedding + 相似度阈值判定
> （Redis 向量或 local 进程内）。本地裁定：**进程内向量线性扫描**（Redis 向量需
> RediSearch 模块——非标准 Redis，诚实 out-of-scope）；**Buzhou 不内置 embedding
> provider**（宿主注入 Spring AI `EmbeddingModel`，spring-ai-model 2.0.0 已在类路径）；
> **默认关闭**（opt-in——语义命中存在 false-positive 残余风险，由嵌入模型质量与阈值
> 共同决定，框架只保证机制正确性——分离诚实入档）。

## §A SemanticCacheStore（T240 / impl-191）

### Problem Statement

FAQ / 评估 run / 固定前缀负载中同义问法（非逐字相同）每次都打满模型调用；精确缓存
（spec 53）键含 messages 全文，问法变一字即 miss。

### Solution

`SemanticCacheStore`（buzhou-resilience.cache）：向量条目 =（embedding float[]、终态
ChatResponse、expireAt、桶键）；查询 = embed(query) → 桶内线性 cosine 最近邻 ≥ 阈值
（默认 0.95）即命中。

### Implementation Decisions

- **分桶**：桶键 = modelName + options 采样（`ResponseCacheKeys.optionsSample` 同口径
  复用——跨模型/参数变体天然隔离，不比相似度）。桶内线性扫描：条目量级数百（FAQ 型
  负载 + max-entries 上限），量级由 perf 哨兵钉住。
- **LRU + TTL 惰性过期**：单锁 LinkedHashMap accessOrder（ResponseCacheStore 同风格）；
  TTL 过期命中路径惰性判定；容量逐出 LRU 谓词驱动；hit/miss/evicted 计数可读。
- **cosine 规范实现**：零向量防护（范数 0 → 相似度 0，不 NaN）；维度不匹配条目跳过
  （防御坏数据）。
- 可注入 Clock（TTL 测试零等待）。

### Testing Decisions

- 同桶同义命中 / 阈值下不命中 / TTL 过期弃 / LRU 逐出计 evicted / 跨桶隔离。
- 零向量与维度错配防御（不抛、按不命中处理）。

## §B SemanticCacheAdvisor（T241 / impl-191）

### Solution

`SemanticCacheAdvisor`（BaseAdvisor，order = ToolCallingAdvisor.DEFAULT_ORDER + 460：
精确缓存 +450 **之后**——精确键命中先短路（零成本），语义查（嵌入成本）只在精确 miss
后发生）。adviseCall：语义查命中 → `new ChatClientResponse(缓存, request.context())`
（新建包装不共享可变引用）；未命中 → 透传 + 终态聚合后写。

### Implementation Decisions

- **嵌入文本规范化**：messages 全文拼接（role 不入嵌入文本——问法语义与角色无关；
  memory advisor 注入后的视图，与精确缓存同位序口径）。
- **写入边界**：复用 `ResponseCacheAdvisor.isTerminal`（无 toolCalls 且内容非空）——
  带工具调用响应绝不缓存（副作用安全沿用 spec 53 §B 裁定）。
- **嵌入故障旁路降级**：嵌入调用异常 → 语义层旁路（WARN + bypass 计数），主调用不受
  阻断——嵌入故障不该弄坏主路径（与限流 fail-fast 语义不同：这里降级不损害正确性，
  只是少一次可能的命中）。
- 命中响应重放不触发模型调用/熔断窗/MODEL_CALL span（与精确缓存同诚实语义）。

### Testing Decisions

- call 命中短路（模型零调用）；未命中透传且终态写入；带 toolCalls 不写；嵌入异常旁路
  主调用成功。

## §C EmbeddingModel 装配与配置（T242 / impl-192）

### Solution

- `ResilienceProperties.SemanticCache` 参数组（enabled=false / similarity-threshold=0.95 /
  max-entries=128 / ttl=1h）——顶层 record 第 15 组件，14 参兼容构造保留。
- advisor 装配：semantic-cache.enabled=true 且上下文无 `EmbeddingModel` bean →
  `BuzhouConfigurationException` fail-fast 带修法（与 shadow 解析同口径：显式开启而
  依赖缺失 = 配置错误，不静默降级）；有 bean → SemanticCacheAdvisor 挂链。
- spring-configuration-metadata.json 四键入档 + 绑定矩阵登记（T214 防线）。

### Testing Decisions

- enabled 无 bean → 启动失败带修法；有 bean → advisor 在链（order 460）；默认关 →
  不挂链零行为变化；四键 yml 绑定。

## §D 红队（T243 / impl-193）

### Testing Decisions

- **否定对**：stub 嵌入模型把「X」与「不是 X」编为相近向量 → 框架按阈值诚实命中——
  钉住机制语义：**框架保证阈值/分桶/边界正确，语义判别力归嵌入模型**；残余风险由
  默认关闭 + runbook 声明 + 阈值可调承担（分离入档，不装全能）。
- 跨模型桶隔离（不同 modelName 不互命中）；参数敏感（temperature 变 → 不同桶）；
  enabled 无 bean fail-fast；带 toolCalls 不写；嵌入异常旁路不阻断。
- 阈值边界：恰等于阈值 → 命中（≥ 语义钉住）。

## §E 流式重放 + perf + 演示 + 文档（T244–T247 / impl-193/194/195）

- 流式（T244）：命中 → `Flux.just` 重放（精确缓存同口径）；未命中 → 透传聚合终态写；
  取消/错误不写半截。
- perf 哨兵（T245）：stub 384 维嵌入 + 128 条满桶语义查路径 P95（进程内毫秒级预期，
  硬顶 10 倍宽幅；真实嵌入延迟归 provider 另计——baseline 标注 stub 口径）。
- 演示（T246）：FAQ 同义问法（「退货政策是什么」/「退货规则是啥」）二问零模型调用 +
  seenPrompts 不增——宿主视角样例。
- 文档（T247）：runbook 语义缓存节（适用场景 / 成本口径：省模型调用 vs 花嵌入调用 /
  残余风险与阈值调优 / 默认关）+ CONTEXT 术语 + api-surface + 快照再生 + 绑定矩阵
  登记 semantic-cache.* 四新键。
