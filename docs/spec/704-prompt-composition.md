# 704 — 提示词角色构成拆解读数

> 来源：G 会话第 5 轮 = effort #704（181 水位的构成拆解对偶面）/ [T1008](../../.wayfinder/tickets/T1008-prompt-composition.md) / [T1009](../../.wayfinder/tickets/T1009-prompt-composition-verify.md) / impl 604。
> 换题注记：原池「指标标签基数审计（Loki）」撞 spec 132 TagCardinalityGuard——换入备选池本题。

## Problem

181 ContextWatermarkHook 回答「本轮注入占窗口百分之几」——但利用率告警触发后，「**谁在吃预算**」没有拆解面：system 提示词膨胀、工具结果堆积、历史失控，三者治理动作完全不同。排障只能肉眼翻消息列表。

## Solution

Langfuse prompt analytics 思想（≈15K star：提示词体量可分析）：

- `PromptComposition`（core.message，纯函数静态原语）：
  - `analyze(Prompt)` → `Report`——按消息角色（MessageType：SYSTEM/USER/ASSISTANT/TOOL）聚合：`Section(role, messages, chars, share)`；chars=各消息 `getText()` 长度和（null 安全）；share=chars/totalChars（total=0 → 全 0 诚实口径）。
  - sections 按字符**降序**（最大吃预算者排首）+同值角色名字典序稳定序——快照断言可复现。
  - `Report(sections, totalChars)` 不可变。
- 无状态无行为：纯证据面，与 181（总量水位）、502（模型窗口门）分层互补——观测不拦截，压缩动作归既有机制。

## User Stories

1. 排障：水位告警响——analyze 当轮 prompt 即见 SYSTEM 占 62%（模板膨胀）还是 TOOL 占 70%（工具结果失控），一屏定位治理方向。
2. 容量规划：持续 USER/TOOL 占比高 → 该上压缩/截断策略；SYSTEM 高 → 该精简模板。

## Implementation Decisions

- 字符口径（token 精算归装配侧 ContextWindowResolver——181 同口径同边界）。
- 角色用 Spring AI MessageType（公开 API 零反射）；media 字节不计（文本口径，诚实边界）。
- 放 core.message（Role.java 同域——角色语义归属）。

## Testing Decisions

- 三角色构成：TOOL 500/USER 300/SYSTEM 200 → 降序排列、share=0.5/0.3/0.2、messages 计数正确。
- 空 prompt/纯 null 文本 → total=0、share 全 0 不 NaN。
- null prompt fail-fast；同值稳定序（字典序）断言。

## Out of Scope

- media/图片字节计量（文本口径先行）。
- 截断/压缩动作（归既有机制——纯读数分层）。
- per-message 明细（角色聚合先行——明细需要时再加）。

## Further Notes

与 R1（能力门审计）/R3（熔断 journal）同会话主题线：**把「异常瞬间」变成「可查询证据面」**；本题是 181 水位告警的证据面深化。
