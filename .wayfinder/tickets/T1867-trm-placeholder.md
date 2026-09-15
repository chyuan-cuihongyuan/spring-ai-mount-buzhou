---
id: T1867
title: R26 发现——captureInjectionSnapshot 对 ToolResponseMessage 的占位符提取依赖 getText()（builder 路径聚合语义待核）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

R26 快照占位符正反例测试中观察到的 ToolResponseMessage 文本语义：TRM 的 getText() 在不同构造路径下可能为空或不为空——captureInjectionSnapshot 的占位符提取（evidence/spill）对 TOOL 消息何时生效？

## Resolution

**用户常设授权 AFK（可推翻）**

核验结论（2026-09-15，R27 = effort #1226）：

1. **语义定论（javap 反编译实证）**：ToolResponseMessage 的 protected 构造将 textContent 置为空串（ldc "" 传给 AbstractMessage）且不聚合 responses——`getText()` 恒空是**设计现状**而非缺陷。
2. **生产捕获路径已正确**：HEAD 的 captureInjectionSnapshot 遍历 `trm.getResponses()` 取 responseData 拼接后做模式匹配（源注释明言「TRM 正文在 responses（getText() 恒空）」）——提取不依赖 getText()，R26 正例（ev-7/s-1/3）实测通过。
3. **观察闭环**：R26 的「TRM 占位符提取永不生效」疑点不成立——初版观察基于旧实现/误建模；当前实现提取路径正确且已有正反例断言覆盖（snapshotPlaceholderExtractionPositiveAndNegative）。
4. **本轮产物**：删除按错误合同写的 getText() 聚合断言（assert getText contains 模式——恒 "" 假红）；保留正反例提取断言。主代码零变化。
