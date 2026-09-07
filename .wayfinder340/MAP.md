# Wayfinder Map — Buzhou 路由权重热调整（effort #340，C 会话第 41 轮）

> C 会话第 41 轮。339 立了多模型加权路由——但权重 yml 定死：调流量
> 配比要重启。320 已立 rebind 热重载模式（舱容量改 yml 发事件热生效），
> 路由权重是同类运维弧线需求（金丝雀期微调配比）。

## Destination

`WeightedChatModel.setWeight(beanName, weight)`（运行时 API——平滑 WRR
状态保留，改的只是步进权重）+ `RoutingWeightsHotReload`（BuzhouConfig-
RefreshEvent 重读 buzhou.routing.weights 逐路 setWeight——已知路热调；
新增/删除路不热加【诚实边界：候选面构造期定死，面变更须重启】）+
装配（路由器在场即挂监听）。

## Notes

- 号段：spec 340 / T671–T672 / impl-363。
- 借鉴源：Spring Cloud Context rebind（320 同源）+ Nginx upstream weight 热调。
- 纪律：无路由器 = 监听不存在（零变化）；未知路 WARN 跳过不红
  （面外名字常见于环境差——诚实跳过）。

## Decisions so far

- setWeight 保留 WRR 状态（current 不动——存量动量不因调权清零，
  调权后比例自然收敛到新配比）。

## Out of scope

- 运行时加/删候选路（候选面固定——面变更重启，与 acquire-timeout
  不热改同口径）；权重历史/审计（记 reload 计数即可）。

## Tickets

- [x] [T671 setWeight 运行时调权](tickets/T671-set-weight.md)
- [x] [T672 热重载监听 + 装配 + 收口](tickets/T672-weights-hot-reload.md)
