# Spec 6016 — Dictionary Encoding 字典编码（effort #6017，T17）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6233–T6234，impl 2217）。
> 借鉴：Parquet/ORC 字典编码思想。

## Problem Statement

低基数列存储的病：重复率高的列（枚举/状态/标签）逐值全宽
直存（存储放大）——**基数收缩换窄域面**缺失。

## Solution

`DictionaryEncoding`（core/message）：

- 首次出现序登记字典表（值→下标），下标列以
  ⌈log₂(去重数)⌉ 位定宽打包（组合 BitPacking——层间解耦
  各自可审计）；单值列 0 位宽；
- decode/valueAt 还原；读数：dictionary/distinctCount/
  indexBitWidth/size/indicesWordCount（压缩对账）；
- fail-fast：null/空列、下标越界。

## User Stories

1. 作为列存作者，状态标签列 64→3 位/值——存储锐减。
2. 作为审计作者，字典表独立可查——基数分布显形。

## Testing Decisions

- 5 基数 300 行往返全等+字典首次出现序+位宽 3 钉住；
  单值列 0 位宽；全混列 4 基数 2 位宽；1000 行双值 16 字
  对账；fail-fast。

## Out of Scope

- 不做字典溢出回退（Parquet fallback 策略层）；不做
  排序字典（首次出现序定构）。

## Further Notes

- 与 BitPacking（T16）组合不同层：下标列底座；与
  Simple8b（T15）不同面：基数收缩 vs 同域变长混合。
- 里程碑：T17/50（34%）。
