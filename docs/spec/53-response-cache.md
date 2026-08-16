# Spec 53 — 精确响应缓存（effort #12）

> effort #12 第一篇。§A：advisor 骨架与键设计（T203）；§B：写入边界（T204）；
> §C：流式路径（T205）；§D：LRU+TTL（T206）；§E：可观测与装配（T207）。
> 外部事实源：LiteLLM（~26K★）response caching——请求参数哈希键 / 流式组装后缓存 /
> 命中重放。本地收窄：进程内精确缓存；**新增本地裁定（LiteLLM 无此约束）：带 toolCalls
> 的响应不缓存**（agent harness 工具副作用安全）。

## §A ResponseCacheAdvisor 骨架与键（T203 / impl-168）

### Problem Statement

重复请求（评估 run、幂等重试、固定 FAQ 前缀）每次都打满模型调用与 token 成本；
无进程内缓存层。

### Solution

`ResponseCacheAdvisor`（buzhou-resilience，implements BaseAdvisor）：order =
ToolCallingAdvisor.DEFAULT_ORDER + 450（memory(+400) 之后、observability(+500)/
resilience(+700) 之前——命中短路后两者不感知，诚实语义：无模型调用即无 MODEL_CALL span、
不进熔断窗）。

### Implementation Decisions

- 键 = sha256(modelName ‖ messages 规范序列化 ‖ options 采样)。messages 序列化：
  每条 `role|content|toolCallId|toolCalls签名`；options 采样 = options 类名 +
  temperature/topP/topK/maxTokens（可空转空串——近似性诚实入档：未采样的自定义参数
  变化不破键）。
- `adviseCall`：查键命中 → `new ChatClientResponse(缓存 ChatResponse, request.context())`
  （新建包装不共享可变引用）；未命中 → `callChain.nextCall(request)`。
- ChatResponse 缓存对象：Spring AI ChatResponse 不可变假设按只读消费（重放构造新包装）。

### Testing Decisions

- 键确定性：同 messages 两次计算同键；role/content/toolCallId 任一变化键变。
- call 命中短路：第二问不走模型（seenPrompts 计数不增）。

## §B 写入边界（T204 / impl-169）

### Problem Statement

缓存什么不缓存什么必须钉死：带 toolCalls 的中间态响应缓存会导致工具副作用
（harness 收到缓存的 toolCalls 会真的再执行工具）。

### Solution

只缓存**终态响应**：无 toolCalls 且输出内容非空；异常 / 空 / 带 toolCalls 一律不写。

### Testing Decisions

- 带 toolCalls 响应不缓存（二次调用仍走模型）；空内容不缓存；异常不写（缓存面干净）。

## §C 流式路径（T205 / impl-170）

### Implementation Decisions

- `adviseStream`：命中 → `Flux.just(重放 ChatClientResponse)`；未命中 →
  `streamChain.nextStream(request)` 并聚合（内容 + usage + finishReason，对齐
  ObservabilityAdvisor 聚合口径）组装完整 ChatResponse 后写缓存（LiteLLM 语义）。
- 流式取消/错误：不写半截（doOnCancel/doOnError 不入缓存；聚合未完成自然不写）。
- 写缓存在 `doOnComplete` 组装完成后（订阅式，无阻塞）。

### Testing Decisions

- 流式同问二订阅：第二次模型 call 计数不增、重放内容等价；取消的流不产生缓存项。

## §D LRU + TTL 惰性过期（T206 / impl-171）

### Implementation Decisions

- 进程内 LRU（单锁 LinkedHashMap accessOrder，CachedEmbeddingProvider 同风格）+
  容量默认 256；TTL 默认 1h，**惰性过期**（命中路径检查 expireAt，过期即弃并计
  evicted——无后台线程）；容量逐出同计 evicted。
- 缓存值记录 `expireAt = now + ttl` 与 ChatResponse。

### Testing Decisions

- TTL 过期后不命中陈旧（可注入 Clock 或短 TTL 实测）；LRU 压挤序正确；
  evicted 计数与原因可感。

## §E 可观测与装配（T207 / impl-172）

### Implementation Decisions

- 配置组 `buzhou.resilience.response-cache.*`：`enabled`（默认 **false**——零行为回归）、
  `max-entries`（默认 256）、`ttl`（默认 1h）；ResilienceProperties 新增第 14 组件
  `ResponseCache` record（**单构造器**——@ConstructorBinding 盲区预防，T187 教训）；
  12/13 参兼容构造保留。
- 指标：`buzhou.cache.response.hit` / `miss` / `evicted` Function counter
  （MeterRegistry 可空——无 registry 时纯计数器可读 API）。
- 装配：response-cache.enabled=true 时 advisor 进链（ResilienceModule.configure）；
  默认关零变化；非法值（max-entries<1 / ttl<0）fail-fast。

### Testing Decisions

- 默认关：装配零变化；开启：advisor 生效；hit/miss 计数宿主可读；非法配置启动失败。
