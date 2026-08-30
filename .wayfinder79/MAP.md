# Wayfinder Map — Buzhou bulkhead 拒绝计数（effort #79，50 轮自迭代第 44 轮）

> effort #79，延续 #78（T417–T418 / impl-263）。主线：spec 84 fog「per-agent 拒绝
> 计数」——全局 counter（buzhou.bulkhead.rejected）无 agent 维（tag 无界纪律），
> 限流风暴定位不到热点 agent。

## Destination

AgentBulkhead 进程内有界拒绝表（agent → count；256 封顶折 __overflow__——
ErrorSignatures 同纪律）：acquire 失败路径 recordRejection；`topRejections(n)`
（count 降序+字典序稳定）；BulkheadHealth 详情增 topRejected（3 条 "agent xN"
字符串）。NOOP agent（未配置上限）不拒不计。

## Notes

- 借鉴：ErrorSignatures 的进程内有界表模式第三次复用（tag 无界维的看板等价物）。

## Decisions so far

- 计数不影响容量语义（纯旁路）。

## Not yet specified

- 拒绝计数 reset；按时间窗的拒绝率。

## Out of scope

- 沿用 #7–#78。

## Tickets

- [x] [T421 recordRejection + topRejections + 健康面 topRejected](tickets/T423-rejection-stats.md)（impl-264）
- [x] [T422 红队（排序稳定/NOOP 不计/释放可再取）+ 收口](tickets/T424-rejection-close.md)
