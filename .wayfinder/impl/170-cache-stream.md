# 170 — 流式路径缓存

**Parent:** spec 53 §C / [T205](../tickets/T205-cache-stream.md)

**Status:** done

- [x] adviseStream 命中 Flux.just(重放)；未命中聚合（内容+usage+finishReason）后写
  （doOnComplete 组装——取消/错误天然不写半截；LiteLLM 流式组装语义）
- [x] 端到端：流式同问二订阅模型只调一次、内容等价
