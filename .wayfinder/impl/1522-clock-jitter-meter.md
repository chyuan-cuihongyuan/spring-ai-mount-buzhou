# impl 1522 — ClockJitterMeter 时钟抖动测量（R122 = effort #1921 / spec 1921 / T3043-T3044）

**What**：`ClockJitterMeter`（core/metrics 持态 keeper）——record
（定长滚动窗口采样偏差可负）+ jitterMillis（窗口总体标准差，样本
<2 哨兵 -1.0）+ meanOffsetMillis（偏斜分量符号读数）+ snapshot/
capacity 诊断；窗口≥2 fail-fast。

**Why**：NTP discipline 惯例——偏斜与抖动是两个量：偏斜可校
（ClockSkewClamp 钳位）、抖动只可测；抖动多大时钳位校正不可信、
该告警换源的前置仪表。50 轮里程碑轮（R122 = 本会话第 50 轮）。

**Verify**：`ClockJitterMeterTest` 4 用例全绿（恒定偏斜抖动 0/已知
集 stddev=5/不足样本哨兵/窗口滚动与畸形 fail-fast）。

**Status**：done（2026-09-23）
