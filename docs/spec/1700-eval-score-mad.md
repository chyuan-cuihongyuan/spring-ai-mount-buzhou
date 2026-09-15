# Spec 1700 — 评测分数 MAD 鲁棒离散度读面（effort #1700，L 会话 1700 系 R1）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2601–T2602，impl 1300）。借鉴：
> Prometheus / Thanos 的鲁棒统计（MAD 异常检测）——均值/标准差被单次离群 run
> 拖走，**中位数/MAD 不为离群点让步**。

## Problem Statement

逐 run 评测分数的离散度现状只有「全距/均值」直觉：一个离群 run（网络抖动导致
整 run 崩盘、或抽样恰好命中简单集拉高）会把均值与标准差拖偏，「这批 run 到底
稳不稳、哪个 run 异常」无从稳健回答。门判定（EvalGate）只给过/不过瞬时结论，
趋势（EvalPassRateTrend spec 1444）只给方向——**离散度与离群定位**缺位。

## Solution

`EvalScoreMad`（core/eval，静态纯函数，同族 EvalPassRateTrend 房规）：

- `analyze(scores)` → `MadReport(count, median, mad, scores, outliers, dispersion)`：
  - median/MAD = 中位数绝对偏差（MAD = median(|xᵢ − median|)）；
  - 离群定位 = 修正 z 值 `0.6745·|xᵢ − median| / MAD > 3.5`（Iglewicz–Hoaglin）；
  - `Dispersion` 闭集：`INSUFFICIENT`（n<3，mad=−1 哨兵）/ `TIGHT`（MAD=0，
    收紧档——任何偏离中位数的点直接判离群，z 值无定义）/ `SPREAD`（正常档）。
- `analyze(scores, maxZ)`：阈值可调重载（自定义敏感度）。
- 零状态纯函数、不可变报告（入参防御性拷贝）——默认零行为变化，读面 opt-in。

## User Stories

1. 作为评测维护者，我看到 median=0.82 / MAD=0.03——中位水平高且稳定，尽管
   均值被一个 0.1 的崩溃 run 拉到 0.74，我知道那是离群而非回归。
2. 作为评测维护者，outliers=[3] 直接告诉我第 4 个 run 异常，无需肉眼扫全表。
3. 作为框架宿主，我把 maxZ 调到 2.0 收紧哨戒——同一批分数更敏感地暴露波动。

## Implementation Decisions

- 修正 z 值常数 0.6745（= 1/1.4826，正态一致化因子）为公共常量；
  默认阈值 3.5（Iglewicz–Hoaglin 推荐）亦公共常量——宿主可复用算式。
- MAD=0（>半数点相同）不退化除零：走 TIGHT 档直接比较（|xᵢ−median|>0 判离群）。
- 不引入第三方统计库；不改变任何既有评测流程——纯读面。

## Testing Decisions

- 空表/n=2 → INSUFFICIENT 且 mad=−1；[1..5] → median=3/MAD=1/无离群；
- 含离群 [1,1,2,2,100] → outliers=[4]；全同分 → TIGHT 无离群；
- 收紧档含偏离 [5,5,5,5,6] → TIGHT 且 outliers=[4]；
- 自定义阈值 maxZ=1.0 时 [1..5] 两端点入离群；null 输入按空表处理；
- 报告不可变（改入参列表不影响报告）。

## Out of Scope

- 不做滑动窗口/时序衰减（趋势归 spec 1444）；不与 EvalGate 联动改判定。
- 不做分位数族全表（p95/p99 归 TurnLatencyPercentiles 先例）——只 MAD 一轴。

## Further Notes

- 评测三维补齐：门（过/不过）→ 趋势（方向，spec 1444）→ 离散+离群（本轮）。
- 本轮同落 1700 系对账门 `LSession1700LedgerAuditTest`（1400 系
  LSessionLedgerAuditTest 同款公式化机检：spec↔票↔impl↔README 四面互证），
  防漂移于未然而非事后对账（L R40 教训的预防式应用）。
