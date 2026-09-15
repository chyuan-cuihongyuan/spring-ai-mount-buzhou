# impl 1433 — OverwritingRingBuffer 覆写环形缓冲（R33 = effort #1832 / spec 1832 / T2865-T2866）

**What**：`OverwritingRingBuffer`（core/concurrent，synchronized）——add 满
覆最老（overwrites 计数）+ items 最老到最新防御拷贝 + stats 快照；capacity<1
与 null 元素 fail-fast。

**Why**：LMAX Disruptor 思想——最近窗采样「不挡主路」第一美德 + 覆写代价
显式入账；替代各读面手搓「样本封顶 N」截断。

**Verify**：`OverwritingRingBufferTest` 5 用例全绿。

**Status**：done（2026-09-16）
