# Wayfinder Map — Buzhou 工具泳道优先级装配（effort #422，D 会话第 23 轮）

> D 会话第 23 轮（#411 扩散轮——spec 411 明注「工具泳道接线为扩散轮
> 候选」收口）。勘察：PriorityLane（411）零引用——纯原语无接线面；
> ToolLaneRegistry/LaneLimitingToolCallback（173）同为库级原语且是平面
> 公平 Semaphore（FIFO）无优先级语义、无 yml 装配面。宿主要给工具分
> 优先级只能手写代码。

## Destination

yml 声明即装配的工具泳道优先级面：

- `exec.PriorityLaneToolCallback`：装饰器——执行前 `PriorityLane.acquire
  (priority, timeout)`、finally release；超时 IllegalStateException（与
  LaneLimitingToolCallback 同词汇——「工具泳道许可等待超时」）；定义透传。
- `ToolLaneRegistry.priorityLane(name, permits)`：命名单例 PriorityLane
  （与既有 `lane()` 分仓——Semaphore/PriorityLane 类型不同不共 namespace）。
- `config.BuzhouToolLaneProperties`：`buzhou.tool-lanes.lanes.<名>.{permits,
  acquire-timeout}` + `buzhou.tool-lanes.tools.<工具名>.{lane, priority}`。
- 装配：Binder 预绑条件（lanes 非空）→ RuntimeConfig assemblyCustomizer
  名匹配包装（406 同法）；tools 引用未声明泳道 fail-fast（启动即红）。

## Notes

- 号段：spec 422 / T735–T736 / impl-395。
- 借鉴源：Envoy priority levels（411 同源接线）+ 406 装配方（Binder 预绑
  Condition + wrapToolCallbacks 名匹配）。
- 纪律：priority 0-9 有界（PriorityLane.checkPriority 兜底）；permits>=1、
  timeout>0 构造校验；R12 教训——插队测例逐次 release+完成屏障。

## Out of scope

- 抢占持有者（411 已定协作式）；动态调优先级；跨实例共享泳道
  （BackendLanePermit 316 域）；等待数观测面装配（宿主持引用可查
  waitingByPriority）。

## Tickets

- [x] [T735 PriorityLaneToolCallback+registry 扩展](../tickets/T735-priority-lane-tool-callback.md)
- [x] [T736 yml 装配面与 fail-fast](../tickets/T736-tool-lane-assembly.md)
