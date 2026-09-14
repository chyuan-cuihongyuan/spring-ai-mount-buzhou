# 1425 — 会话历史形态审计

> 来源：L 会话第 26 轮 = effort #1425（票 T2151 / T2152 / impl 1078）。借鉴：MLflow 数据画像 / OpenAI conversation shape 分析（上下文健康的第一画像是结构形态而非内容）。

## Problem Statement

会话历史的角色分布与结构异常无审计面：USER/ASSISTANT 比例漂移（模型独白式多轮）、连续同角色非 TOOL 消息（管线写坏 history）、空内容条目（写入异常）、turnSeq 跳变（回放/乱序写入）——都在恶化为上下文污染前无形态信号。

## 目标

- `ConversationShapeAudit`（core/message，纯函数静态面，private 构造）：
  - `analyze(List<BuzhouMessage>)` → `record ShapeReport(totalMessages, roleHistogram, consecutiveSameRole, emptyContent, maxTurnGap)`；
  - 角色直方（数量降序平名典序）；**连续同角色**按「相邻且角色相同且非 TOOL」计（TOOL 链内连续是并行工具调用正常形态——口径显式）；
  - 空内容 = content null/空白且无 toolCalls（带工具调用的 ASSISTANT 空 content 是正常形态）；
  - maxTurnGap = 相邻消息 turnSeq 最大跳变（>1 = 回放/乱序写入嫌疑）；
  - 空输入零形态哨兵。
- 纯函数零状态：单会话全史/压缩窗口口径由调用方选择。

## 兼容性

纯函数零 IO；只读不裁决（清洗/拦截归既有的 compaction/fence 机制）。

## Out of Scope

- 内容级质量（语义漂移/重复检测——RepetitionDetector 域）。
- 清洗动作（读面不裁决）。
- 跨会话聚合（单列表口径显式）。
