---
Type: task
Status: closed
---
## Question

ResponseCacheAdvisor（resilience 模块）：adviseCall 查键短路/未命中过链；键 = sha256(
modelName + messages 规范序列化 + options 采样)；order 语义（缓存先于 resilience）。

## Resolution

impl-168 落地：advisor order +450（命中短路 observability/resilience——无模型调用无 span
不进熔断窗，语义诚实）；键 sha256 规范序列化（options 采样近似性入档）；命中新建包装不共享
可变引用。端到端同问二调模型只调一次。T203 关闭。
