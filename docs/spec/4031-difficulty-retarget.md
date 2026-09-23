# Spec 4031 — 难度目标重定（effort #4031，R32）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6063–T6064，impl 2132）。
> 借鉴：Bitcoin difficulty retarget（2016 块窗 ±4× 钳制）。

## Problem Statement

工作负载难度（预算/采样率/配额阈值）对算力（流量）漂移的
病的两端：不重定则目标失效（算力翻倍后出块速率失锚），自由
重定则尖叫摆动——**周期性有界重定面**缺失。

## Solution

`DifficultyRetarget`（core/policy）：

- 窗式重定：每窗 `windowBlocks` 个间隔（2016 思想）按窗内实际
  耗时 vs 目标耗时重定一次目标；
- 钳制：实际耗时钳到 [target/4, target×4]——单窗调整幅度天然
  ≤ 4×（不尖叫）；
- `powLimit` 封顶（目标上限 = 难度地板——最易不越过协议地板）；
- 窗滚动：重定块自身开启新窗（Bitcoin 同语义），持续运行；
- 非递减时间戳（倒流 fail-fast——确定性）+ BigInteger 精确。

## User Stories

1. 作为预算作者，负载翻倍后目标自动重定回锚——速率不失锚。
2. 作为审计作者，同到达序列同重定轨迹（确定性可回放）。

## Testing Decisions

- 准时不变 / 快 2× 目标减半 / 慢 8× 钳制 ×4（非 ×8）/
  powLimit 封顶四证；窗滚动再重定 + blocksToRetarget 读数；
  倒流与畸形定构 fail-fast；确定性回放同轨迹。

## Out of Scope

- 不做 SHA-256 PoW 验证（本件是难度治理面非挖矿面）；
- 不做每块 ASERT 类逐块重定（周期窗口径）。

## Further Notes

- 与 Eip1559BaseFee 同族不同面：费用市场每轮小幅软调节 vs
  难度周期窗钳制硬重定。Wave 6 第二件。
- 里程碑：32/50。
