---
Type: task
Status: open
blocked-by: T81
---
## Question

备模型降级链怎么做？现状：onModelError 只能回填静态兜底文案，无备模型切换。决策点：fallback chain 配置形态（模型 bean 名列表？ChatClient 重建方式？）、触发条件（熔断 open 时直接走降级 vs 终态失败后降级）、降级后的会话内粘性（本 turn 降级 vs 后续 turn 记忆）、流式边界（M1 不做中途切换的边界如何声明）、与 T81 熔断状态的交互。产出 spec 15 增量 + impl 57。
