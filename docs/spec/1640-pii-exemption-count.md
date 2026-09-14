# 1640 · PII 豁免计数（与命中统计对照面）

> 来源：N 会话 R41（effort #1640 / T2431–T2432 / impl 1193）。J 系危险工具
> 的 exemptedSkips（spec 1069）对齐补全——PII 侧豁免路径此前无计数。

## Solution

`PiiHitStats.recordExemption()/exemptionsApplied()`（工具级+类型级合并
口径）+ 输出侧/输入侧两 hook 的豁免短路点计数 + `reset()` 同步归零。
对照面价值：exemptionsApplied 持续高于命中数 = 豁免面过宽（登记失控信号）。

## Testing Decisions

- PiiExemptionTest 工具级断言补 `exemptionsApplied()==1`；guard 全量回归。

## Out of Scope

- 豁免按 mechanism 分侧计数（合并口径先行——对照面足够）。
