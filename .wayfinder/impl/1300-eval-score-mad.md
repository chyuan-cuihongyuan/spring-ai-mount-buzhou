# impl 1300 — EvalScoreMad 评测分数 MAD 鲁棒离散度（R1 = effort #1700 / spec 1700 / T2601-T2602）

**What**：`EvalScoreMad`（core/eval 静态纯函数）——`analyze(scores[, maxZ])` 吐
`MadReport(count/median/mad/scores/outliers/dispersion)`；MAD=median(|xᵢ−median|)，
修正 z=0.6745·|x−med|/MAD>3.5 判离群；`Dispersion` 闭集 INSUFFICIENT（n<3，mad=−1
哨兵）/TIGHT（MAD=0 收紧档：偏离中位点直接判离群）/SPREAD。

**Why**：评测三维补齐——门（过/不过 EvalGate）→ 趋势（方向 EvalPassRateTrend
spec 1444）→ 离散+离群（本轮）；均值/标准差被离群 run 拖走，中位数/MAD 不让步
（Prometheus/Thanos MAD 异常检测思想）。

**Verify**：`EvalScoreMadTest` 8 断言（哨兵/奇偶中位/离群定位/收紧档/自定义阈值/
null 防御/不可变）。同轮落 1700 系对账门 `LSession1700LedgerAuditTest`
（starter 常驻，范围自扩展）。

**Status**：done（2026-09-15，L 会话 1700 系第 1 轮）
