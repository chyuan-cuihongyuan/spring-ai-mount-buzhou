# 733 — 提示词使用缺口读数

> 来源：G 会话第 34 轮 = effort #733（401/545 提示词族的使用治理）/ [T1066](../../.wayfinder/tickets/T1066-prompt-usage-gaps.md) / [T1067](../../.wayfinder/tickets/T1067-prompt-usage-gaps-verify.md) / impl 633。

## Problem
注册表里堆积的提示词哪些从未被使用（清理候选）、统计里哪些名字已不在注册表（registry 被清但统计残留的漂移）——两个面各自存在（names()/snapshot()）但没有联合读数。

## Solution
`PromptUsageGaps.analyze(declaredNames, rows)` 纯函数：unused=声明∩零使用（字典序）；orphans=stats 有而声明无（漂移信号）。纯读数不改双方。

## Out of Scope
自动删除未用提示词（治理动作归宿主）；版本粒度（name 级聚合先行）。
