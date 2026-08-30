---
Type: task
Status: closed
---
## Question

hook 接线（beforeTool 拒 OPEN / afterTool 记结局）+ 状态机回归。

## Resolution

done（2026-08-30）：impl-278；ToolCircuitBreakerHook（order 240，block 文案带冷却
提示 + buzhou.tool-breaker.blocked 计数；afterTool 走 ToolFeedbackType 结构化标记）
+ 十测试绿（五状态机面 + 五 hook 集成面）。
