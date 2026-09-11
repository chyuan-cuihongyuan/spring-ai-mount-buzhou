# Spec 534 — 提示词版本行级 diff（effort #534）

> wayfinder map：`.wayfinder/maps/effort-534.md`（T821–T822）。E 会话第 34 轮。

## Problem Statement

注册表版本单调递增——晋级/回滚评审「到底改了什么」无读数面（肉眼比正文）。

## Solution

`prompt.PromptVersionDiff`（纯函数）：行级 LCS 最小变更集——
DiffLine(equal|insert|delete) + VersionDiff(fromVersion, toVersion,
lines, added, removed, net)；异名 fail-fast。与注册表 versions() 组合
（相邻版本评审）。

## User Stories

1. 作为评审者，我想看 v3→v4 的行级变更集， so 晋级评审只看变化不重读全文。

## Implementation Decisions

- 经典 LCS 长度表（最小性由 LCS 保证）；行级不做词内 diff。

## Testing Decisions

- 最小对齐分类断言；全等零变更；异名 fail-fast；注册表组合（版本号
  与增删计数）。

## Out of Scope

- 词内/语义 diff。

## Further Notes

- 新公共类型 `PromptVersionDiff`（嵌套 `VersionDiff`/`DiffLine`）随轮
  regenerate 快照 + api-surface.md 加行。
