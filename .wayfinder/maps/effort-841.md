# effort #841 — 会话空闲时长分桶直方

- 会话：H 会话 800 系第 42 轮 ｜ spec [841](../../../docs/spec/841-idle-duration-histogram.md) ｜ 票 [T1183](../tickets/T1183-idle-duration-histogram.md)/[T1184](../tickets/T1184-idle-duration-histogram-verify.md) ｜ impl594
- 借鉴：S5 备选池（IdleSessionMonitor 扩散——「谁空闲」之外的「空闲多久分布」面）

## 勘察（排重）

- IdleSessionMonitor：空闲识别+清理执行——无分布直方。
- WatermarkHealth 等：非时长分布族。
- grep -i `idle.*histogram|idle.*distribution`：无命中。

## 决定

`IdleDurationHistogram`（core.session，纯记账）：可配升序边界（默认 1m/5m/15m/60m→5 桶）n+1 桶 AtomicLongArray；record 负值忽略；total/longestIdle；bucketLabel 人话区间（[0,1m)/≥1h）；labeledSnapshot 全桶行。喂点=Monitor/清理器装配侧。

## 测试

默认边界五桶落位（恰达边界归右桶+≥1h）/自定义边界+负值忽略/标签四例/带标签全桶快照——4 例绿（humanize 毫秒档纯数字统一修正）。

## 诚实边界

边界固定构造期（调参=换实例——直方本征）；桶是放置法非等宽（边界语义调用方定）；喂点手动。
