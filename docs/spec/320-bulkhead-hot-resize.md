# Spec 320 — 舱容量热调整（effort #320）

> wayfinder map：`.wayfinder320/MAP.md`（T631–T632）。借鉴：Spring Cloud
> Context（EnvironmentChangeEvent → rebind）+ resilience4j ResizableSemaphore。

## Problem Statement

舱容量（per-agent 并发 Turn 上限）只能启动期定死：SRE 调容量要改 yml 再
重启实例；R19/20 的扰乱预算与伸缩建议落地同样卡在"改配置 = bounce"——
建议面有了，执行面没有热通道。

## Solution

- `AgentBulkhead.resize(newLimits)`：扩容补 permit / 缩容 reducePermits
  （在飞不受扰，瞬时可超新限，释放到限内才放新请求——resilience4j 同款）；
  新 agent 建舱；移除 agent 摘舱（在飞 Lease 释放到已摘对象，无害）。
  拒绝计数不清零（单调——建议器窗口增量依赖）。
- `BuzhouConfigRefreshEvent`（core.config，空标记事件）：宿主改完
  PropertySource 后发布——core 不引 spring-cloud，事件自持。
- `BulkheadHotReload` 监听器：收到事件重读 `buzhou.bulkhead.agents` →
  resize 全局舱；装配随舱（舱开即监听，未配 agents 也可后续热加）。

## User Stories

1. 作为运维，流量高峰我想不重启把热点 agent 上限 2 调到 8，所以在飞
   会话零中断、新请求立刻按新限放行。
2. 作为运维，峰过我想把上限缩回去，所以超发只到在飞自然收敛——不抢占
   不中断。
3. 作为运维，我想热移除一个配错的 agent 限（回 NOOP），所以不用等发布
   窗口。
4. 作为宿主开发者，我想用自己的配置通道（ConfigMap watch / 管理端点）
   触发重载，所以发一个事件即可——库不强加 spring-cloud。

## Implementation Decisions

- ResizableSemaphore：Semaphore 子类暴露 reducePermits（protected 原语
  公开化——resilience4j SemaphoreBulkhead 同法）；公平性保持。
- resize 原子（synchronized——与 acquire 的竞争窗口可接受：permit 级
  原语本身原子，resize 只做差量调整）。
- 计数器 `buzhou.bulkhead.resized`；移除 agent 的在飞释放语义在 javadoc
  明示。
- 事件无载荷：环境读取交给监听器自己 Binder——事件只做"该重读了"信号。

## Testing Decisions

- `AgentBulkheadResizeTest`（外部行为）：扩容放行/缩容拒新放到在飞释放/
  热加 agent/热移除回 NOOP/拒绝计数保留。
- `BulkheadHotReloadTest`（装配）：舱开即有监听 bean；发布事件 + 改
  PropertySource → limitOf 生效；舱未开不装配。

## Out of Scope

- acquire-timeout 热改（构造期定死）；泳道/spawn 闸热重载；ConfigMap
  文件 watch 触发器（宿主接线——事件即缝）。

## Further Notes

- 运维弧线收口：排水 155 / 维护门 205 / 扰乱预算 318 / 伸缩建议 319 /
  **热落地 320**——建议到执行的最后一块。
