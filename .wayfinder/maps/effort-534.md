# Wayfinder Map — Buzhou 提示词版本行级 diff（effort #534，E 会话第 34 轮）

> E 会话第 34 轮（401 注册表扩散轮；Git diff 思想）。勘察：注册表版本
> 单调递增——晋级/回滚评审「到底改了什么」无读数面（只能肉眼比正文）。

## Destination

`core.prompt.PromptVersionDiff`（纯函数）：diff(from, to) 行级 LCS 最小
变更集（equal/insert/delete + added/removed/net）；异名 fail-fast。
与注册表 versions() 组合消费。

## Notes

- 号段：spec 534 / T821–822 / impl-436。

## Out of scope

- 词内/语义 diff；渲染 diff。

## Tickets

- [x] [T821 LCS diff](../tickets/T821-prompt-version-diff.md)
- [x] [T822 注册表组合](../tickets/T822-prompt-diff-compose.md)
