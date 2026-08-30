---
Type: task
Status: closed
---
## Question

memory 微压缩事件化（T111 fog）：微压缩只有指标（buzhou.compaction counter），无 SessionEvent 面——黄金轨迹与运维事件流看不到「何时折入了多少」。是否补事件？

## Resolution

AFK 自决：补。MemoryViewProcessor 无事件通道（视图处理器纯函数）——事件在 HotTail/微压缩的实际落点发：DefaultMicroCompactor 无 emitter 依赖，改由 SpillOffload/微压缩挂点？最小正确面：MicroCompactionResult 已带 compactedIds/reclaimed——在 memory 模块装配层（MemoryModule 配置的视图处理器闭包内）发 `memory.compacted`（payload：compactedCount/reclaimedChars/protectedTurns），经 SessionEvent 通道（视图处理发生在 get() 读路径——会话事件通道可用？读路径无会话 emitter……诚实核对：视图处理器在 BuzhouChatMemory.get 内同步执行、无事件通道）→ 降级方案：事件由 memory 模块经全局监听不可得时，改为 **observability 事件双写**（ObservabilityStore.saveEvents——RunawayCounters 同款通道），黄金轨迹断言改经 eventsOfSession。产 spec 34 §A + impl-90。
