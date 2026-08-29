# Wayfinder Map — Buzhou 输入侧命中统计接线（effort #126，A 会话第 21 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 144 fog「输入侧钩子同款接线」收口。

## Destination

PiiInputRedactionHook 双点打点 PiiHitStats（内置 matches 循环 + 自定义占位符
提取共用 extractCustomRuleNames）——输入/输出双侧命中进同一张合规报表。

## Notes

- extractCustomRuleNames 上移为 PiiHitStats 静态工具（双钩共用，内置类型名
  剔除防双计）。

## Decisions so far

- [输入侧接线](tickets/T517-input-stats.md) — 镜像输出侧双点 + 共用提取器。

## Not yet specified

- 报表 JSONL 导出（窗口 export→reset 的 export 半边）。

## Out of scope

- 输入/输出分开两张表（同表更利于「同一类型两侧都炸」的发现）。

## Tickets

- [x] [T517 输入侧命中接线](tickets/T517-input-stats.md)（impl-293）
- [x] [T518 收口提交](tickets/T518-input-stats-close.md)（impl-293）
