---
id: T5075
title: Q 会话 R38 自相关的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

序列的周期/趋势/白噪性怎么一把尺判定？（spec 3037 / effort #3037 / R38）

## Resolution

**Autocorrelation（core/metrics，纯函数）**：Box-Jenkins lag-k
ACF——x 与滞后 k 平移的 Pearson 相关：周期自锁定 +1/半周期反相
−1/趋势惯性高正/白噪 ~0。零方差 NaN 诚实；「打转检测/惯性量化/
独立性检验」三用一尺。
