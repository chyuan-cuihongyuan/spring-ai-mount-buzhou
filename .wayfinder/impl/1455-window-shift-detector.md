# impl 1455 — WindowShiftDetector 双窗口漂移检测（R55 = effort #1854 / spec 1854 / T2909-T2910）

**What**：`WindowShiftDetector`（core/metrics 静态纯函数）——detect 三态
（STABLE/SHIFTED_UP/SHIFTED_DOWN）双闸判定（绝对×相对，零基线分母
max(|基线|,1) 退化）；空窗/负闸参/null 与 NaN 样本 fail-fast。

**Why**：Netflix/Google SRE 双窗口异常检测惯例——单闸二难（只相对被小
基数噪声刷屏、只绝对漏报缓变）；双闸同过才「重要且显著」，升降方向
分开（变差告警 vs 优化验证）。

**Verify**：`WindowShiftDetectorTest` 4 用例全绿（首跑红为用例闸参过严
修正）。

**Status**：done（2026-09-16）
