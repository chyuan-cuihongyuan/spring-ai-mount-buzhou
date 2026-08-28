# Spec 83 — 错误签名聚类（effort #44）

> wayfinder map：`.wayfinder44/MAP.md`（T321–T322）。借鉴：Sentry fingerprint。

## Problem Statement

工具/模型失败在指标面只有 `outcome=failed` 计数：哪一族错误在烧（超时族？
连接拒绝族？）不可见，排障要翻日志逐条人肉聚类。

## Solution

`ErrorSignatures`：失败按「异常简名 + 归一化消息首行」聚成有界签名族——数字串
折 `#`、≥8 位十六进制折 `hex#`（时间戳/端口/traceId 不再分裂族）、多行取首行、
截 96 字符；进程内有界 Map 256 条封顶，满后新族折 `<kind>:__overflow__`（既有族
继续细分）；`top(n)` / `snapshot()` 输出稳定排序（count 降序 + 字典序）。全局旋钮
（`global()`/`install()`）；工具错误路径（HookedToolCallback）自动记录。

## User Stories

1. 作为运维，我要 top 错误族一眼可见，所以排障不用翻日志。
2. 作为看板作者，我要 snapshot/top 拉取面，所以健康端点/仪表盘直连。
3. 作为库用户，我要错误族不爆 micrometer tag，所以指标系统不被无界维度打爆。

## Implementation Decisions

- 进程内表而非 micrometer tag（项目「tag 值有界枚举」纪律；签名维度天生无界）。
- 单趟正则归一（替换符不含数字——避免被后续趟误折）。
- overflow 按 kind 分开（tool/model 各自溢出不合并）。

## Testing Decisions

- 归一：数字/十六进制折叠等值、多行取首行、null→<blank>；
- 封顶：256 满后新族折 overflow、既有族继续细分、独立实例与 global 隔离；
- top 排序稳定；throwable 面（简名:归一消息）。

## Out of Scope

- model 失败路径接线（resilience 错误码计数已覆盖计数面，签名接线另议）；
  健康端点暴露；跨实例聚合。

## Further Notes

- `top(n)` 是健康端点的天然候选（fog 记账）。
