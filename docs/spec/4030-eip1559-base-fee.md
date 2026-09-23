# Spec 4030 — EIP-1559 基础费调节（effort #4030，R31）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6061–T6062，impl 2131）。
> 借鉴：EIP-1559 弹性费用市场（go-ethereum calcBaseFee）。

## Problem Statement

配额定价的病：固定费率对供需无感（拥挤不涨价浪费、闲置
不降价饿死），或自由浮动无锚（尖叫摆动）——**有界供需
调节面**缺失。

## Solution

`Eip1559BaseFee`（core/policy）：

- 每轮按用量 vs 目标调节基础费：满块（2×target）涨、空块跌、
  恰目标不变；
- 弹性钳制：gas limit = 2×target 机器性约束下单步幅度天然
  ≤ ±1/8（BASE_FEE_MAX_CHANGE_DENOMINATOR）——有界不尖叫；
- 地板 minBaseFee（ETH 初始 1 gwei 思想）——跌到地板止跌；
- 整数精确运算（BigInteger floor division——go-ethereum
  Euclidean division 同口径，下跌方向不舍入回零）；
- 超弹性（gasUsed > 2×target）fail-fast——机器性约束由调用方
  gas limit 保证，越界即违约上抛不静默钳制。

## User Stories

1. 作为配额定价作者，拥挤自动涨价、闲置自动降价——供需有界调节。
2. 作为审计作者，同序列同轨迹（确定性可回放）。

## Testing Decisions

- 满块 8e9→9e9 / 空块乘法回落（9e9→7.875e9，−1/8 乘法
  非对称）/ 恰目标不变三证；
- floor 语义（base=7 空块→6 非不变）；地板止跌；
- 超弹性/负用量/畸形定构 fail-fast；钳制不变量
  （任意合法用量单步 ≤ base/8）+ 确定性回放。

## Out of Scope

- 不做 tip（priority fee）小费市场；不做账户余额记账；
- 不做跨轮 meta 费用治理（归 budget 族既有件）。

## Further Notes

- 与 ModelRateLimiter（令牌桶硬闸门）互补：费用市场软调节
  vs 速率硬闸。Wave 6（定价与协议数族）开波。
- 里程碑：31/50。
