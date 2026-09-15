# impl 1459 — FrequencyDecay 频次衰减竞速（R59 = effort #1858 / spec 1858 / T2917-T2918）

**What**：`FrequencyDecay`（core/cache 静态纯函数）——decayed（周期减半
向下取整封底 0）+ overtakePeriod（命中×t > 衰减值的最小 t，零命中 -1
哨兵 + MAX_RACE_PERIODS 保险丝）；负数 fail-fast。

**Why**：Redis LFU counter decay 思想——频次只涨没有退则老热点赖顶、
新热点永追不上；衰减-顶替竞速周期是缓存自适应性读数（过长=衰减慢、
过短=抖动）。

**Verify**：`FrequencyDecayTest` 4 用例全绿（首跑红为心算期望误——R54
入档病理第四次实证：期望值用代码算）。

**Status**：done（2026-09-16）
