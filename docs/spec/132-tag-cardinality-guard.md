# Spec 132 — tag 基数守卫（effort #111）

> wayfinder map：`.wayfinder/maps/effort-111.md`（T457–T458）。借鉴：Grafana Loki
> label cardinality limit。

## Problem Statement

「tag 值有界枚举」是项目纪律而非机制——一处失守（agent 名/工具名/错误文本进
tag），时序库的 cardinality 就被一个高基数值打爆，账单与查询性能一起恶化，
且失守点事后难定位。

## Solution

`metrics/TagCardinalityGuard`：装饰任意 `BuzhouMetrics`。per (指标名, tag 键)
去重值集封顶（默认 64），越限<b>新值</b>折 `__overflow__`——既有值照常直通；
被折样本不丢（计数/时长照记），丢的是维度细分。指标名空间亦有界（512，满则
新名全折——新名本就是代码新增面的信号）。守卫面：`folds()` 折入计数。
热路径纪律：畸形键值对（奇数/null）透传不抛——守卫失守不放大为指标路径故障；
并发双插最坏超限 1-2 值（ErrorSignatures 同先例）。

## User Stories

1. 作为 SRE，越界 tag 值被折入 __overflow__，所以一次纪律失守不打爆时序库，
   且 folds() 告诉我失守发生了几次。
2. 作为宿主开发者，我 wrap 任意 metrics 实现即得守卫，所以接入面是一行装饰。

## Testing Decisions

- 红队：封顶内直通（重复在册直通）；越限折入 + 既有值继续直通；多 tag 键
  独立守卫；名空间满全折 + 既有名不受影响；畸形透传；timer 面同守卫；wrap
  参数 fail-fast。

## Out of Scope

- 默认装配接入（opt-in 键——后续 fog）；per-tag-key 差异化封顶；守卫自登记
  micrometer。

## Further Notes

- 与 spec 83/117 的进程内有界表同族：把「有界」从各表自管提炼为可装饰机制。
