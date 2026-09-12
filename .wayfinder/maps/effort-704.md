# effort #704 — 提示词角色构成拆解读数（换题轮）

- 会话：G 会话 700 系第 5 轮 ｜ spec [704](../../../docs/spec/704-prompt-composition.md) ｜ 票 [T1008](../tickets/T1008-prompt-composition.md)/[T1009](../tickets/T1009-prompt-composition-verify.md) ｜ impl604
- **换题注记**：原计划「指标标签基数审计（Loki）」勘察撞 spec 132 TagCardinalityGuard（同思想已落地）——按池规则换入备选「提示词分段预算读数」。
- 借鉴：Langfuse（≈15K star）prompt analytics——提示词体量的可分析性

## 勘察（排重）

- 181 ContextWatermarkHook：本轮注入**总量**/窗口水位 gauge+低水位事件——总量超限后「**谁在吃预算**」（system 大还是工具结果大）无拆解面。
- TagCardinalityGuard（132）是指标面非提示词面——撞题确认。
- grep PromptComposition/composition：零命中。

## 决定

`PromptComposition`（core.message 纯函数）：analyze(Prompt)→Report——按 MessageType 角色聚合 chars（getText 长度和，null 安全）/消息数/占比 share（total=0 诚实 0），sections 按字符降序+同值角色名字典序稳定。无状态无行为——排障「上下文为什么满」的构成证据面。

## 测试

构成降序+占比精确/空 prompt 诚实零/null fail-fast+稳定序。

## 诚实边界

字符口径（token 精算归装配侧——181 同口径）；纯读数不拦截不压缩（动作归既有机制）；media 字节不计（文本口径）。
