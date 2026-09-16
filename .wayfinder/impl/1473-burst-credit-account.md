# impl 1473 — BurstCreditAccount 突发信用账户（R73 = effort #1872 / spec 1872 / T2945-T2946）

**What**：`BurstCreditAccount`（core/backpressure，synchronized）——基准
蓄水封顶 + trySpend 透支（枯竭计数见底一次性）+ burstHeadroomMillis 余量
换算；畸形与时钟回拨 fail-fast。

**Why**：AWS CPU credit/T3 unlimited 语义——限流拒绝之外的降速第三态；
突发余量与枯竭频率让突发预算可规划。

**Verify**：`BurstCreditAccountTest` 4 用例全绿。

**Status**：done（2026-09-16）
