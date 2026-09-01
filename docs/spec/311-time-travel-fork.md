# Spec 311 — 时间旅行 fork（effort #311）

> wayfinder map：`.wayfinder311/MAP.md`（T613–T614）。借鉴：LangGraph
> checkpointer time-travel（从任意检查点重放分支）。

## Problem Statement

会话 fork 只能从「最后消息」整史复制：定位「从第 N 轮走岔」要回放对照时，
没有回到第 N 轮重走一条分支的原语——宿主得手工裁剪消息历史。

## Solution

`AgentRuntime.forkFromTurn(sourceSessionId, appId, agentName, newSessionId,
upToTurn)`（default 抛 UnsupportedOperationException，DefaultAgentRuntime 实现）：

- 复制 `turnSeq <= upToTurn` 的历史前缀到新会话（完整 spawn 管线——容量闸/
  租约/装配全生效）；源会话不动，两分支独立演化。
- **Summary 不复制**：最新摘要可能覆盖 upToTurn 之后的轮次——复制即未来
  泄漏（诚实边界：分支从干净摘要基线重走）。
- State 不复制（预算重置——重试/探索语义同 fork 20）。
- fork 监听器（26）与 `session.forked` 事件（payload 加 upToTurn）管线复用。
- upToTurn < 1 / 前缀为空 → IllegalArgumentException。

## User Stories

1. 作为开发者，从第 N 轮 fork 两个分支各走一条路——对照定位走岔点。
2. 作为运维，A/B 提示词实验同起跑线（同前缀不同分支）。
3. 作为宿主，分支不带未来摘要——重放结果不被后见污染。

## Implementation Decisions

- 轮号即检查点（turnSeq 已持久化于消息——无需新增检查点存储）。

## Testing Decisions

- `TimeTravelForkEndToEndTest`（对齐 SessionForkEndToEndTest）：三轮源会话
  forkFromTurn(2) → 分支历史只含前两轮（含续聊 prompt 断言不含第三轮）；
  摘要不复制；源不动；upToTurn 越界/<1 拒绝。

## Out of Scope

- 按轮对齐的摘要裁剪；跨 store 时间旅行（导出/导入族已有 28）。

## Further Notes

- 分支族：fork（20）/ 导入重映射（28）/ **时间旅行（本轮）**。
