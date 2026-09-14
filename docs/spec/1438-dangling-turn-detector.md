# 1439 — 悬空轮检测器

> 来源：L 会话第 39 轮 = effort #1438（票 T2177 / T2178 / impl 1091）。借鉴：Temporal activity 检测（中断后「activity 无结果」是恢复语义的边界形态）。

## Problem Statement

会话被取消/中断/崩溃重启后，history 可能留下**有 USER 输入但无 ASSISTANT 回复**的悬空轮——续聊时模型看到「自己被问了却没答」的历史，回复质量与上下文一致性受损。悬空轮无检测面（TurnSequenceAudit 管 turn 序号缺号、ConversationShapeAudit 管相邻对——按轮分组的「问了没答」形态无归属）。

## 目标

- `DanglingTurnDetector`（core/message，纯函数静态面，private 构造）：
  - `analyze(List<BuzhouMessage>)` → `record Report(totalTurns, danglingTurnCount, danglingSamples)`；
  - 判定口径：轮内**有 USER 且无 ASSISTANT** 即悬空（TOOL 链无 ASSISTANT 收尾同悬空）；仅 TOOL/SYSTEM 轮不算悬空（外部写入形态非「问了没答」）；
  - 样本榜：悬空轮 turnSeq 升序封顶 8（定位用）；
  - 空输入零报告哨兵 + `hasDangling()` 风险哨兵派生。
- 纯函数零状态：单会话 history 口径（顺序无关，按 turnSeq 分组）。

## 兼容性

纯函数零 IO；只读不裁决（悬空轮的续写/清除归宿主与恢复服务）。

## Out of Scope

- 悬空轮自动续写/清除（RunRecoveryService 域）。
- span 层悬空（TurnErrorSampler 已采样错误轮）。
- 跨会话聚合（单会话口径显式）。
