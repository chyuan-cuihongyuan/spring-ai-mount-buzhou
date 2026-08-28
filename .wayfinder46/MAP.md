# Wayfinder Map — Buzhou 错误签名健康面（effort #46，50 轮自迭代第 11 轮）

> effort #46，延续 #45（T323–T324 / impl-231）。主线：**#44 fog 毕业生**——
> ErrorSignatures 有 top()/snapshot() 但只在进程内，运维看板/`/actuator/buzhou`
> 快照看不到。

## Destination

`ErrorSignaturesHealth implements BuzhouHealth`：mechanism=error-signatures；
恒 UP（观测面——错误多≠机制失能，DOWN 语义留给核心职能）；details = top-5 族
（"sig xN" 有界字符串列表）+ distinct 数；autoconfig 在 EndpointConfiguration 内
挂 bean（有 actuator 才装配）；零新键。

## Notes

- 借鉴：Spring Boot actuator health details 纪律（有界、无敏感内容）。

## Decisions so far

- top-5 截断（健康 details 有界纪律；全量走指标面/进程内 snapshot()）。

## Not yet specified

- 错误签名导出（OLAP JSONL 面）；model 失败路径的签名接线。

## Out of scope

- 沿用 #7–#45；DOWN 判定（观测面永不下 DOWN）。

## Tickets

- [x] [T327 ErrorSignaturesHealth + autoconfig 挂载](tickets/T327-signatures-health.md)（impl-232）
- [x] [T328 3 例红队（恒 UP 有界 top/空表/端点聚合）+ 文档收口](tickets/T328-signatures-health-close.md)
