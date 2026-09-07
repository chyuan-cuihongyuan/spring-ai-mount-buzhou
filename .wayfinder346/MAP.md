# Wayfinder Map — Buzhou 会话面板端点（effort #346，C 会话第 46+1 轮）

> C 会话第 47 轮。面板三部曲收官：343 生效配置、345 告警态——缺「现在
> 有多少活跃会话、准入地板谁抬着、维护 cordon 是否生效」的会话面。
> 事实源选 SessionIndexStore（318 PDB 同源——status=ACTIVE 计数）。

## Destination

`BuzhouSessionsEndpoint`（/actuator/buzhou-sessions）：activeSessions
（索引分页计数，50k 封顶+truncated 标记诚实降级）+ spawnFloor
（effective + 各源视图——342 多源）+ maintenance（cordon view）。
无索引 bean → activeSessions 段缺席诚实；只读；挂 343/345 同 actuator
条件配置类。

## Notes

- 号段：spec 346 / T683–T684 / impl-369。
- 借鉴源：Grafana/K8s dashboard 的资源总览面板思想（面板三部曲之三）。
- 纪律：地板与 cordon bean 恒在（342）——端点两段恒可读。

## Decisions so far

- 计数分页扫（PAGE 500×MAX 100 页）；短页即止。

## Out of scope

- 会话明细列表（隐私面——聚合数即可）；per-app 分组（需求出现再加）。

## Tickets

- [x] [T683 端点聚合（索引计数+地板+cordon）](tickets/T683-sessions-endpoint.md)
- [x] [T684 装配 + 收口](tickets/T684-sessions-assembly.md)
