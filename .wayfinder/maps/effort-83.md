# Wayfinder Map — Buzhou 错误签名窗口化清零（effort #83，50 轮自迭代第 48 轮）

> effort #83，延续 #82（T431–T432 / impl-267）。主线：**spec 112 fog「导出后清零
> （窗口化统计）」**——错误族表只增不减：长期运行进程内表趋近封顶，趋势分析要
> 按窗口切分。

## Destination

`ErrorSignatures.reset()`：counts 清零——export → reset 循环 = 每窗口一份 JSONL、
进程内表永有界；清零后新窗口照常计数。运维 cron 驱动（不自装调度）。

## Notes

- 借鉴：Prometheus counter reset 语义的显式版（窗口切分归调用方）。

## Decisions so far

- 显式 reset 而非自动窗口（调度归运维——fsck/audit 同纪律）。

## Not yet specified

- reset 事件（审计——运维需求后议）。

## Out of scope

- 沿用 #7–#82。

## Tickets

- [x] [T435 reset 清零 + 新窗口计数](../tickets/T437-sig-reset.md)（impl-268）
- [x] [T436 红队（循环有界 + 新窗口照常）+ 收口](../tickets/T438-sig-reset-close.md)
