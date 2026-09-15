# Spec 1818 — 顺序读预读顾问（effort #1818，R19）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2837–T2838，impl 1419）。借鉴：
> Linux readahead——顺序访问检测即指数放大预读窗（封顶），随机访问即关预读。

## Problem Statement

Spill RangeRead 的预读窗固定则两头亏：顺序扫描大 handle 时窗太小（磁盘往返
多）、随机点查时窗太大（读了白读还挤缓存）——预读大小没有随访问形状自
适应的判定面。

## Solution

`ReadAheadAdvisor`（buzhou-spill，静态纯函数）：

- `ReadEvent(offset, length)` 读事件（契约 length ≥ 1）；
- `advise(blockSize, recent)` → `Advisory(pattern, readAheadBytes, chainLength)`：
  取最长尾链（相邻读首尾相接 = prev.offset+prev.length == next.offset）判
  形状；SEQUENTIAL 预读 = blockSize × 2^min(链长−1, 3)（指数放大封顶 8 倍）；
  RANDOM 零预读；COLD 样本不足三态。

## User Stories

1. 作为读路径优化者，顺序扫描时预读 1 块→2 块→8 块自适应放大——磁盘
   往返次数随形状收敛。
2. 作为缓存治理者，随机点查自动关预读——不再为白读挤掉热缓存。
3. 作为框架宿主，事件窗有界自声明，纯建议零执行可回放。

## Implementation Decisions

- 纯建议不执行；只看尾链（近况优先，断链即换形状）。
- 常量：MAX_GROWTH_FACTOR_EXPONENT=3、MIN_CHAIN_FOR_PATTERN=2（禁魔法数）。

## Testing Decisions

- 链 2/4/8 的指数放大与封顶；跳读零预读+头断尾在取尾链；COLD 三态哨兵；
  畸形 fail-fast。

## Out of Scope

- 不执行预读；不接 RangeReadEngine 热路径（接线归后续轮）。

## Further Notes

- 与 HotTailViewProcessor（热尾视图）正交：那是保留策略，这是预读大小。
