# Spec 158 — 虚拟 key yml 装配（effort #123）

> wayfinder map：`.wayfinder/maps/effort-123.md`（T511–T512）。spec 148 fog「autoconfig
> yml 键」收口——编程面变装配面。

## Problem Statement

key 级预算闸（spec 148）只有编程面：宿主手工建 VirtualKeys、传 5 参构造——
yml 用户得不到，能力等于没上线。

## Solution

`buzhou.virtual-keys.*` 两键：`active-key`（本实例扣减归属；省缺 = 不启用，
零变化）+ `limits.<key>=<token 硬顶>`（Map 结构化面）。autoconfig：
active-key 配置时装配 `VirtualKeys` bean（limits 全量注册）并经
`tokenBudgetHook` 接进预算闸；健康段经 @ConditionalOnBean(VirtualKeys)
自动出现（spec 154）。校验分工：bind 期只验 limits 值 ≥1（矩阵逐键绑定
不炸）；「active-key 配置但 limits 空」与「active-key 不在 limits」在装配期
fail-fast——省缺 key 的扣减是静默直通，诚实边界反被误用必须响亮。
元数据与矩阵：additional-metadata 手维护补两键 + limits 走 SKIPPED（Map
结构化面）+ 子键样例 `limits.app-key`（active-key 样例的配套——否则装配期
fail-fast 会正确拒掉矩阵上下文）。

## User Stories

1. 作为 yml 用户，两行配置即得 key 级 token 硬顶 + 耗尽拦截 + 健康段，
   所以编程面能力零代码上线。
2. 作为运维，active-key 配错（不在 limits / limits 空）启动即失败带修法，
   所以不会带病上线静默直通。

## Testing Decisions

- 红队：属性三分支（省缺惰性 / 非正 fail-fast / 合法绑定）+ 矩阵两测
  （宇宙覆盖 + 真实装配路径含子键样例）全绿 + 启动校验回归。

## Out of Scope

- Redis 共享额度后端；成本面；动态换 key。

## Further Notes

- 装配链完整：yml → registry bean → 预算闸（spec148）→ 健康段（spec154）。
