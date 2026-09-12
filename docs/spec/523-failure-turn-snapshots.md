# Spec 523 — 失败轮快照面（effort #523）

> wayfinder map：`.wayfinder/maps/effort-523.md`（T797–T798）。E 会话第 24 轮。

## Problem Statement

错误轮的排障最小集（错误类/消息/输入）散在日志——无环形快照+导出读数
面（423 是评测采样域，排障域空白）。

## Solution

`core.recovery.FailureTurnSnapshots implements SessionObserver`：

- onTurnStart 记输入（LRU 64 轮防泄漏）、onTurnError 落快照：
  Snapshot(turnSeq, errorClass, errorMessage≤256, inputPreview≤512+…)。
- 环形 128（totalErrors 环外总量）；exportJsonl 一行一快照（转义完备）。
- 注册：SessionAssemblyContext.addObserver（assemblyCustomizer 宿主组合）。

## User Stories

1. 作为排障工程师，我想错误轮一屏看到「什么输入、什么错」， so 复现
   最小集不用翻日志。

## Implementation Decisions

- 输入预览截断（完整输入可能含敏感面——导出归宿主管道）。
- 只观测不干预（错误路径零额外开销面）。

## Testing Decisions

- 错误轮捕获+双截断断言；成功轮不入；环形有界+环外总量；JSONL 转义
  单行；会话 E2E（ScriptedChatModel 抛错）。

## Out of Scope

- 完整输入持久化；跨会话聚合；自动上报。

## Further Notes

- 新公共类型 `FailureTurnSnapshots`（嵌套 `Snapshot`）随轮 regenerate
  快照 + api-surface.md 加行。
