# Spec 34 — 压缩事件化与黄金轨迹全覆盖

> effort #7（T115–T117 / impl-90–92）。

## §A 压缩事件化（T115 / impl-90）

- `InjectionViewProcessor.setCompactionListener(BiConsumer<String, MicroCompactionResult>)`：
  实际折入（compactedMessageIds 非空）才通知；监听器异常 lenient（不影响视图主链）。
- `MemoryModule` 装配把监听器接 ObservabilityStore 双写 `memory.compacted` 事件
  （payload：compactedCount/reclaimedChars）——视图读路径无会话事件通道，
  RunawayCountorners 同款观测侧写通道。黄金轨迹/运维事件流经 eventsOfSession 可见。
