---
Type: task
Status: closed
---
## Question

`RoutingWindow`（LocalTime start<end 同日窗+weights 非空）解析校验 +
`RoutingScheduleAdjuster.evaluateOnce`：首窗命中整表替换/无窗回落基础/
同目标幂等零动作/未知 bean 名跳过。

## Resolution

done（2026-09-12）：impl-406；窗内/窗外/幂等/多窗序用例绿（Clock 注入）。
