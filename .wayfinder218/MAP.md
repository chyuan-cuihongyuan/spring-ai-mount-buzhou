# Wayfinder Map — Buzhou 平滑加权路由（effort #218，B 会话第 41 轮）

> B 会话第 41 轮。降级链是「串行换人」；等价多模型（同能力不同供应商）间
> <b>分流</b>缺位——只能全压一个。借鉴 Nginx smooth weighted round-robin
> （按权重分流且分布平滑，不集中爆发）。

## Destination

WeightedRouter<T>（core/concurrent 泛型）：构造 (candidate, weight) 表；
pick() 平滑加权轮转（当前权重累加取最大者扣总权重——Nginx 同算法）；
动态 setWeight 调权零重建；单候选退化直选。模型分流/工具实例选择通用。

## Notes

- 号段：B=奇数 spec（本轮 199）；轮次 .wayfinder200+。
- 与降级链（15）/驱逐（149）/演练（195）组合：先过滤健康池，再加权分流。

## Decisions so far

- 平滑算法（非随机）——同序调用可复现，回归测试可断言。

## Not yet specified

- 权重热更新事件；最少连接数策略。

## Out of scope

- 沿用各轮；随机策略；一致性哈希。

## Tickets

- [x] [T571 WeightedRouter（Nginx 平滑 WRR）](tickets/T571-weighted.md)（impl-313）
- [x] [T572 加权回归（比例正确/平滑/动态调权/退化）](tickets/T572-weighted-tests.md)（impl-313）
