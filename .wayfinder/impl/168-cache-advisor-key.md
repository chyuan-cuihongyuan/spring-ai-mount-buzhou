# 168 — ResponseCacheAdvisor 骨架与键设计

**Parent:** spec 53 §A / [T203](../tickets/T203-cache-advisor-key.md)

**Status:** done

- [x] ResponseCacheAdvisor（resilience.cache 包，BaseAdvisor）：order +450（memory 后、
  observability/resilience 前——命中短路两者，诚实语义：无模型调用无 span 不进熔断窗）
- [x] 键 = sha256(model ‖ messages 规范序列化 ‖ options 采样)；options 近似性入档
  （temperature/topP/topK/maxTokens/model + 类名；未采样自定义参数不破键）
- [x] 命中重放 `new ChatClientResponse(cached, context)`（不共享可变引用）
- [x] 端到端：同 messages 二问模型只调一次；不同 messages 不串命中
