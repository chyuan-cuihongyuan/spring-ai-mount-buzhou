# 805 — 路由流量分布倾斜读数

> 来源：H 会话第 6 轮 = effort #805 / [T1111](../../.wayfinder/tickets/T1111-route-distribution-skew.md) / [T1112](../../.wayfinder/tickets/T1112-route-distribution-skew-verify.md) / impl 558。
> 借鉴：Spark skew detection（≈41K star）。

## Problem

权重说 50/50、实际流量 95/5（慢启动残留/压权未恢复/会话粘连）无人对账：WeightedChatModel.routes() 只有声明值，「声明 vs 实际」漂移不可见。

## Solution

`RouteDistributionReadout`（resilience.routing）双层：

- **Collector**：`record(route)` 线程安全计数（封顶 32 + truncated；null/空白忽略但计入 totalRecorded）。
- **analyze(actualCounts, expectedWeights)** 纯函数：per-route share/expectedShare/deviation 按 |偏差| 降序（打平按路由名典序——确定性）；缺权重路由期望=0、有权重零流量也列偏差行；`gini` 实际分布基尼系数；dominantRoute/dominantShare。

## 兼容性

纯新增（Collector 由装配侧/应用喂点——不自动改 WeightedChatModel 行为）；无配置键。

## 诚实边界

读数不纠偏；基尼量集中度不辨方向；期望=权重归一（全非法→全 0 期望如实）；两路由系统 |deviation| 恒相等（数学事实——典序破平保证确定性输出）。
