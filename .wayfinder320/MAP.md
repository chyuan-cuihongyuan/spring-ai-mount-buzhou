# Wayfinder Map — Buzhou 舱容量热调整（effort #320，C 会话第 21 轮）

> C 会话第 21 轮。R19/20 把 K8s 维护三件套+HPA 建议面补齐，但建议落地
> 要重启进程——SRE 调个 yml 容量就 bounce 实例，建议面的价值打折。
> Spring Cloud Context（@RefreshScope / EnvironmentChangeEvent → rebind）
> 与 K8s ConfigMap watch 的共同思想：配置变了，运行时热生效。

## Destination

`AgentBulkhead.resize`（resilience4j ResizableSemaphore 同款：扩容补 permit、
缩容 reducePermits——在飞不受扰，新请求按新限）+ `BuzhouConfigRefreshEvent`
（宿主改完配置发布的通用刷新事件）+ `BulkheadHotReload` 监听器（重读
`buzhou.bulkhead.agents` → resize 全局舱）。建议→落地闭环补全。

## Notes

- 号段：spec 320 / T631–T632 / impl-343。
- 借鉴：Spring Cloud Context rebind；resilience4j SemaphoreBulkhead 的
  ResizableSemaphore（reducePermits 是 protected——子类暴露）。

## Decisions so far

- 缩容语义：reducePermits 可为负——在飞超新限瞬时共存（resilience4j 同款），
  释放到新限内才放新请求；不抢占在飞。
- 移除 agent：从 limits/semaphores 摘除——在飞 Lease.close() 释放到已摘
  对象（无害，GC），新 acquire NOOP。
- 拒绝计数不随 resize 清零（单调——R20 建议器窗口增量依赖）。
- 事件为空标记（无载荷）：宿主负责先改 PropertySource 再发布；core 不引
  spring-cloud（依赖不进门，事件自持）。
- acquire-timeout 热改不收（构造期定死）——诚实边界入 Out of scope。

## Out of scope

- acquire-timeout 热改；其他容量面（泳道/spawn 闸）的热重载（按需后续轮）；
- ConfigMap/文件 watch 触发器（宿主接线——事件即缝）。

## Tickets

- [x] [T631 resize + 热重载事件/监听](tickets/T631-bulkhead-resize.md)（impl-343）
- [x] [T632 回归与装配收口](tickets/T632-bulkhead-close.md)（impl-343）
