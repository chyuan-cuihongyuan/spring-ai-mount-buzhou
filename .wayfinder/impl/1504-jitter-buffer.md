# impl 1504 — JitterBuffer 自适应抖动缓冲（R104 = effort #1903 / spec 1903 / T3007-T3008）

**What**：`JitterBuffer`（core/backpressure 静态纯函数）——
requiredDelay（升序第 ⌈target×n⌉ 个样本覆盖保证延迟）+
coverageRatio（给定延迟实际覆盖占比）；样本非空非负/target∈(0,1]/
delay≥0 fail-fast。

**Why**：VoIP/WebRTC 自适应抖动缓冲——出队节奏两头难（立即出队
空转、固定大延迟陪绑）；覆盖分位延迟让「多等一毫秒多覆盖多少」
可量化。与满队阻塞背压互补（队列满 vs 时间轴抖动）。

**Verify**：`JitterBufferTest` 4 用例全绿（覆盖分位两例/覆盖率三例/
极值/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
