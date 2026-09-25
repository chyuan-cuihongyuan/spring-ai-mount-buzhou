# Spec 6015 — Simple8b 位打包（effort #6015，T15）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6229–T6230，impl 2215）。
> 借鉴：Simple8b 思想（InfluxDB/时序列存同源）。

## Problem Statement

小基数整数列存储的病：逐值 8 字节直存（小值存储放大数十
倍）——**字对齐多值共居面**缺失。

## Solution

`Simple8b`（core/message）：

- 4 位选择子+60 位载荷：16 档（值数×位宽：240×0 至 1×60），
  贪心选首个可容档（同输入同字流）；全零档单字 240 值；
- decode 尾部截断到原值数（选择子定档数+计数诚实截断）；
- 读数：wordCount/valueCount/wordsCopy 审计；
- fail-fast：null/负值/值≥2^60。

## User Stories

1. 作为列存作者，小整数列一档打包数十值——存储数十倍减。
2. 作为审计作者，字流可静态解码——离线可核。

## Testing Decisions

- 混合量级 500 值往返全等；960 零恰 4 字+尾 100 零 1 字
  截断；单 60 位值满字（选择子 15）钉住；确定性；fail-fast。

## Out of Scope

- 不做浮点/负数（zigzag 由调用方先变换）；不做 SIMD 反转
  字节序。

## Further Notes

- 与 VarintCodec 同族不同面：流式逐字节 vs 字对齐并行；
  与 BitPacking（T16）互补：混合自适应 vs 单一固定宽。
- 里程碑：T15/50（30%）。
