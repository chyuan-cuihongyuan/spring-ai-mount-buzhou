# 1603 · http_request per-host 并发上限（Nginx limit_conn 思想）

> 来源：N 会话 R4（effort #1603 / T2357–T2358 / impl 1156）。借鉴对象：Nginx
> `limit_conn`（>10K star 项目）——按 key（此处 = 目标 host）限制并发连接数，超限
> 拒绝（503 语义），不排队。

## Problem Statement

工具执行脊柱的虚拟线程 fan-out 允许同一轮并行调用多个工具，跨会话还会叠加。
模型对同一 host 的并发请求（分页抓取、批量 API 调用）没有任何 host 维度的上限——
单 host 可以瞬间占满 HttpClient 连接资源并把目标服务打挂（等效对外 DoS）。
SSRF 守卫管「能不能访问」，per-host 并发闸管「同时能压多狠」——正交缺口。

## Solution

`PerHostConcurrencyGuard(maxPerHost)`：host → CAS 计数器；`tryEnter` 超
`maxPerHost` 即 false，`http_request` 快速失败返回拒绝文案（limit_conn 拒绝语义，
不排队不阻塞）；请求结束（成功/失败/异常）`exit` 释放名额。
`maxPerHost ≤ 0` = 关（默认零行为）。归零计数条目留 map（数十字节/条，host 集合
受 SSRF 放行域约束量级可控——不做激进清理避免释放竞争）。

## User Stories

1. 作为服务方，我想让单 host 并发有上限，所以模型行为失控也不会瞬间打挂目标服务。
2. 作为运维者，我想让拒绝可观测，所以 hostLimitRejects 进 stats 第七桶（守恒式不变）。
3. 作为运维者，我想保持默认零变化，所以不配 max-per-host 时行为与现状一致。

## Implementation Decisions

- 新类 `tools/http/PerHostConcurrencyGuard`（tryEnter/exit/inFlight 读数）。
- `HttpRequestTool` 构造器重载 +guard（null=关）；SSRF 校验后 tryEnter（host 维度在
  安全校验之后——被拒地址不占名额）；finally 释放。
- stats record 扩 `hostLimitRejects`（第七桶），`totalRejects` 与守恒式同步更新。
- yml：`buzhou.tools.http-request.max-per-host`（缺省 0=关）。

## Testing Decisions

- `PerHostConcurrencyGuardTest` 五断言：关=无限 / 上限拒绝+释放再进 / 跨 host 独立 /
  工具集成（占满名额 call() 得 limit_conn 拒绝文案 + 桶计数 + 守恒式；释放后放行）/
  无闸零变化（非法方法照旧走 method 桶）。
- Prior art：`HttpToolStatsTest`（桶守恒式）。

## Out of Scope

- 排队等待语义（limit_conn 是拒绝；需要等待属限流器/泳道家族）。
- 全局（跨 host 合计）并发上限（fan-out 上限 8 已有全局闸）。

## Further Notes

- 集成测试的「释放后放行」断言不含外网依赖：真实请求成败皆可，只断言不再被闸短路。
