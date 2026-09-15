# impl 1420 — BudgetPacingCurve 花费匀速曲线（R20 = effort #1819 / spec 1819 / T2839-T2840）

**What**：`BudgetPacingCurve`（core/budget 静态纯函数）——evaluate 三态
（ON_PACE 带内/OVER_PACING 超前/UNDER_PACING 落后）+ deviation 偏离 +
runRate 运行率（-1 哨兵）；FLOAT_EPSILON=1e-12 边界浮点噪声免疫。

**Why**：广告 spend pacing 思想——匀速是预算健康基线，runRate=2 直接读出
「按当前速度期末烧两倍」；前半周期烧 80% 的风险在断粮前显形。

**Verify**：`BudgetPacingCurveTest` 4 用例全绿（首跑双红为 0.55−0.5 二进制
尾差翻态的浮点病理实证，实现侧 EPSILON 根治——L 系 impl1305 同病理对照）。

**Status**：done（2026-09-16）
