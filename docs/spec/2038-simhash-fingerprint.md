# Spec 2038 — SimHash 近重复指纹（effort #2038，R39）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3177–T3178，impl 1589）。
> 借鉴：Charikar SimHash——特征加权投票指纹，近重复 O(1) 位比较。

## Problem Statement

文本近重复检测（数据集去重 / 相似输入合并 / 重复工具描述）：逐对
全文比对 O(n×m) 不可扩展；精确 hash 只认全同——近重复（小改编辑/
增量词元）漏检。

## Solution

`SimHashFingerprint`（core/metrics，纯函数零状态）：

- `fingerprint(tokens)`：词元逐位 FNV/splitmix64 散列 → 每 bit 加权
  投票（+1/−1 求和取符号）→ 64 位指纹——**相似文本指纹汉明距离小**；
- `hammingDistance(a, b)`（bitCount 对称）；`isNearDuplicate(a, b,
  threshold ≤ 3 默认)`；
- **诚实边界入档**：小词元集平局 bit（票和恰 0）对增量敏感——经验
  ≥16 词元较稳，短文本宜相对判定或精确比对；
- 契约：tokens 非空非 null 元素、threshold ≥ 0 fail-fast。

## User Stories

1. 作为数据集作者，指纹比对 O(1)——万级条目近重复筛查不逐对比对。
2. 作为去重管道，汉明阈值可调——宽严口径随场景。

## Testing Decisions

- 同文本同指纹；**重复词元距离恰 0**（同向票加倍符号不变——确定性
  数学性质）；小增量距离 << 不交文本（相对判定）；不交 >16（期望
  32）；汉明对称与 64 上界；阈值 0 精确匹配语义；畸形四型 fail-fast。

## Out of Scope

- 不做分词/权重（词元清单口径归调用方）；不做指纹索引（分桶查找
  归后续轮）；数据集近重复读数（847）与正交互补。

## Further Notes

- 首版测试教训：短文本平局噪声（6 词元 +2 增量实测距离 10）——
  SimHash 小文本固有，改确定性性质断言（重复词元=0）+ 相对判定。
