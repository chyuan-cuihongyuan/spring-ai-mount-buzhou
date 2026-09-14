# 1065 — 完成轮检测器读面

> 来源：J 会话第 65 轮 = effort #1065（[T1585](../../.wayfinder/tickets/T1585-completedturn-stats-shape.md) / [T1586](../../.wayfinder/tickets/T1586-completedturn-stats-verify.md) / impl 817）。借鉴：OpenTelemetry span 完成判定（上游闸门的空结果率是整条管线失能的第一信号）。memory/compact 域第二轴：R59 手动压缩 / R65 检测前提。

## Problem Statement

`DefaultCompletedTurnDetector.detectTurns()`（微压缩触发的前提判定——只有完成的轮才可折入）全路径零计数：**历史全为悬挂轮（ASSISTANT 消息带未闭合 toolCalls）时检出恒为空，微压缩静默失能**——「压缩为什么从不触发」无法回答；检出率的分母（扫过的含工具调用轮）也无从对账。

## 目标

- `DefaultCompletedTurnDetector` 增量（memory/compact，静态面）：三 `AtomicLong`。
  - `detectCalls`：detectTurns 入口计数；
  - `spansDetected`：检出完成轮累计；
  - `toolCallTurnsSeen`：扫过的含工具调用 ASSISTANT 消息数（可检出量分母）。
- 嵌套 `record CompletedTurnStats(long detectCalls, long spansDetected, long toolCallTurnsSeen)` + `stats()` + `resetForTest()`。
- 口径诚实：spansDetected ≤ toolCallTurnsSeen 为弱校验非硬守恒（同轮可被多次扫描反复计入分母）；检出率 = spansDetected / toolCallTurnsSeen。

## 兼容性

纯增量读面：detectTurns 返回语义、TurnSpan 形状逐位不变；静态面理由同 R46–R64 先例；无新配置项。

## Out of Scope

- 悬挂轮成因分类（归上游回调缺失，另轴）。
- 按历史长度分桶（敏感面红线）。
