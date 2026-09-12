# 748 — 执行策略汇总读数

> 来源：G 会话第 49 轮 = effort #748（五件套配置确认面）/ [T1098](../../.wayfinder/tickets/T1098-execution-policy-readout.md) / [T1099](../../.wayfinder/tickets/T1099-execution-policy-readout-verify.md) / impl 649。

## Problem

EvalRunner 五件套策略各自 setter——「当前 run 会有哪些行为修饰」无一屏确认面：detail 里出现 [RUN-BUDGET]/[RETRIED]/[MEMO] 时，排障先要翻代码确认哪些策略在生效。

## Solution

`executionPolicy()`：Map 回显当前策略态——runBudgetChars（0=关）/errorRetryOnce/perItemTimeoutMs（null=未设）/memoizationKey（null=关）/driftWindow（0=关）/driftWarnShift（关时 null）。纯读数。

## Out of Scope
策略校验建议（合法性由各 setter 既有校验保证）。
