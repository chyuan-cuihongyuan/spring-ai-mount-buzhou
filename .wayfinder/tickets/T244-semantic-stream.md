---
Type: task
Status: done
blocked-by: T242-semantic-wiring.md
---
## Question

流式 E2E：adviseStream 语义命中 → Flux.just 重放（对齐精确缓存流式重放口径）；未命中
→ 透传 + 聚合终态写；取消/错误不写半截。
