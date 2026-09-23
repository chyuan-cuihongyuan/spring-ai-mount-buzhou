# Spec 1920 — 复合健康分（effort #1920，R121）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3041–T3042，impl 1521）。借鉴：
> 监控面板（Grafana 站点健康分/半导体产线 OEE 惯例）复合健康分——
> 多维健康读数（延迟/错误率/饱和度/新鲜度）按权加权合成 0–100
> 单一分，三档判级——「总体健康吗」一句话可答。

## Problem Statement

多维健康读数各自为政：延迟绿、错误率黄、新鲜度红——「总体到底
怎么样」需要人肉权衡；加权合成单一健康分 + 三档判级，让看板与
告警有一句话结论。

## Solution

`HealthScoreComposite`（core/health，静态纯函数）：

- `composite(scores, weights)`：加权平均——Σ(score×w)/Σw（0–100
  分）；scores/weights 同长非空；
- `band(score)`：三档判级——≥ 80 HEALTHY；≥ 50 DEGRADED；否则
  UNHEALTHY（边界含下）。

## User Stories

1. 作为看板作者，延迟 90/错误 80/新鲜度 40 等权 → 70 分
   DEGRADED——一句话结论。
2. 作为 SRE，新鲜度权重加倍 → 分数被关键维度主导——权重即
   运维语义。
3. 作为告警作者，< 50 UNHEALTHY 接告警——判级即告警级。

## Implementation Decisions

- 纯函数零状态；scores/weights 同长非空、score ∈ [0,100]、
  weight > 0 fail-fast；判级边界含下（恰 80 HEALTHY）。

## Testing Decisions

- 加权合成两例（等权/带权）；三档判级各一例（80/50/30 含下）；
  畸形三型（空表/越界分/零权重）fail-fast。

## Out of Scope

- 不做维度健康读数采集（归各健康面）；不做动态权重。

## Further Notes

- 与 BuzhouHealth 各机制面互补：那是单机制 UP/DOWN，这是多维
  合成单一分。
