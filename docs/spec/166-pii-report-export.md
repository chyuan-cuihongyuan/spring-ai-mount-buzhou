# Spec 166 — PII 命中报表导出（effort #127）

> wayfinder map：`.wayfinder/maps/effort-127.md`（T519–T520）。spec 164 fog「报表
> JSONL 导出」收口。

## Problem Statement

PiiHitStats 只有进程内 top() 查询——合规报表的时序分析（各窗口哪类 PII 高发）
需要导出面，与既有 OLAP 导出族（观测/评估/错误签名/A-B）汇入同一管线。

## Solution

`guard/pii/PiiHitStatsJsonl.export(stats, writer)`：全部在册命中平铺一行一
JSON（`{"name":...,"count":N}`），与 top() 同序（count 降序 + 名字典序）；
返回行数。export → reset 循环 = 每窗口一份合规报表（spec 121 同纪律）。
Jackson 管线转义纪律（规则名含引号/换行不撕行）；空表零行诚实。

## User Stories

1. 作为合规分析师，我把命中报表灌进 DuckDB 与时间轴 join，所以脱敏策略
   调优有窗口级趋势依据。
2. 作为运维，export 后 reset 表永有界，与错误签名窗口管线同一套 cron。

## Testing Decisions

- 红队：同序 + 行独立解析 + 转义（自定义规则名含引号换行）；空表零行；
  export→reset→再计数的窗口循环。

## Out of Scope

- gzip 面；命中样本明细；分侧（输入/输出）列拆分。

## Further Notes

- 导出族第六员：观测/评估/A-B/错误签名/清单之外的合规面。
