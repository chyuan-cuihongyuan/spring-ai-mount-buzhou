# Spec 2015 — 最小 RTT 滑窗滤波器（effort #2015，R16）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3131–T3132，impl 1566）。
> 借鉴：TCP BBR min-RTT——基线取窗内最小而非均值（均值被队列膨胀污染）。

## Problem Statement

对冲延迟基线 / 自适应超时下限需要「真传播时延」参照：均值被偶发队列
膨胀拉高（基线虚高 → 对冲过晚超时过长），而全局历史最小又永不更新
（网络恶化后基线永久过时）。

## Solution

`MinRttTracker`（core/metrics，synchronized 小临界区）：

- `observe(rtt, now)`：窗内入样（默认窗 10 分钟——BBR 惯例）；过期
  样惰性清除；**见新最小**（rtt < 窗内旧最小，首样本必刷新）记
  `lastFreshMinAt`；
- `minRtt(now)`：窗内最小（滑出后旧最小失效，次小接管——网络恶化
  后基线可上浮追认）；空窗 0——调用方以 sampleCount 判有效性；
- `lastFreshMinAt()`：最近刷新时刻（-1 从未）——基线陈旧度显形；
- 契约：window > 0、rtt ≥ 0、now ≥ 0 fail-fast。

## User Stories

1. 作为对冲作者，min-RTT 基线不被膨胀样本拉高——对冲时机贴真时延。
2. 作为超时调参者，网络恶化后基线自动上浮（窗滑出重算）——不永久
   过时。
3. 作为 SRE，lastFreshMinAt 距今过久 = 基线陈旧——可信度显形。

## Testing Decisions

- 窗内最小稳定（膨胀样本不扰动）；滑出边界（999 界内/1000 出窗）次
  小接管；网络改善追认；空窗零 + sampleCount；持平不刷新新鲜度；零
  RTT（本地命中）合法；畸形三型 fail-fast。

## Out of Scope

- 不接对冲/超时管线（接线归后续轮）；不做 BW·RTT 容积估计（BBR BDP
  留白）。

## Further Notes

- 与对冲延迟策略（O-1831）正交：那轮定策略形状，本件供其真基线。
