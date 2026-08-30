---
Type: task
Status: closed
blocked-by: T203-cache-advisor-key.md
---
## Question

adviseStream：命中 Flux.just(cached) 重放（LiteLLM 语义）；未命中 nextStream 聚合完整
（内容+usage）后写缓存；流式取消不写半截。

## Resolution

impl-170 落地：命中 Flux.just 重放；未命中 doOnComplete 聚合组装（取消/错误天然不写半截）。
流式同问二订阅模型只调一次、内容等价。T205 关闭。
