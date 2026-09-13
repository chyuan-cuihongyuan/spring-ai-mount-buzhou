# effort #805 — 路由流量分布倾斜读数

- 会话：H 会话 800 系第 6 轮 ｜ spec [805](../../../docs/spec/805-route-distribution-skew.md) ｜ 票 [T1111](../tickets/T1111-route-distribution-skew.md)/[T1112](../tickets/T1112-route-distribution-skew-verify.md) ｜ impl558
- 借鉴：Spark skew detection（apache/spark ≈41K star）——分区倾斜检测思想

## 勘察（排重）

- WeightedChatModel：routes() 返回声明权重——无实际调用分布维度。
- RoutingSlowStart/HealthDampener：改权重的执行面——无「声明 vs 实际」对账读数。
- grep -i `skew|gini|imbalance`：无命中——倾斜族缺位。

## 决定

`RouteDistributionReadout`（resilience.routing）双层：Collector 按 route 计数（封顶 32+truncated+null/空白忽略但计 total）；analyze(actual, expected) 纯函数——per-route share/expectedShare/deviation 按 |偏差| 降序（打平典序破平——首测 Map.of 无序抓获）、缺权重路由期望=0、声明无流量也是偏差行；gini 实际分布基尼（0=均匀、单路由全集中=(n-1)/n）；dominantRoute/share。

## 测试

三路由偏差降序+份额期望精确值/缺权重与零流量双偏差行/基尼已知值三例(均匀 0·集中 0.5·无流量 0)/空报告/收集器封顶 32+联动分析/null fail-fast——6 例全绿。

## 诚实边界

只读不纠偏（改权重归 HotReload 族）；期望份额以权重和归一（权重全非法时全 0 期望——如实）；基尼对「份额集中度」敏感而非方向（高于/低于期望靠 deviation 行）；调用点由装配侧喂（Collector 不自动挂 WeightedChatModel——改行为面是刻意的职责切分）。
