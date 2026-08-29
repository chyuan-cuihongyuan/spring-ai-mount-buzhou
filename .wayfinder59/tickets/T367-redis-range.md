---
Type: task
Status: closed
---
## Question

Redis scanByKeyRange 覆写。

## Resolution

done（2026-08-29）：impl-245；SMEMBERS 键侧过滤排序 + 命中键批量取值；不用 ZSET
（写路径双结构成本 > 收益，诚实取舍）。
