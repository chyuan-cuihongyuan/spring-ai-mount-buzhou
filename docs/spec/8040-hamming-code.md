# Spec 8040 — HammingCode（effort #8040，V40）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8081–V8082，impl 2342）。
> 借鉴：Hamming 1950 SECDED（贝尔实验室）思想。

## Problem Statement

传输误码的病：重传代价高——**海明 SECDED：单比特纠错+双比特检错**。

## Solution

HammingCode（core/crypto）：Hamming(8,4) 编码 4 数据位→8 码位（含整体奇偶）+decode 三态（无错/纠 1 位/检 2 位不可纠）+越域 fail-fast+确定性。

## Testing Decisions

16 数据全 roundtrip+64 种单比特翻转全纠正+双比特全检出+确定性；fail-fast。

## Out of Scope

- 不做参数化变体（固定经典参数——明示）。

## Further Notes

- 勘误入档：原拟 Wave 7 第五件 ReedSolomon（GF(256) 算术面）
  实现预算超限退雾区——ChineseRemainder 补位（V42 后波次
  内调序——同族加密信任主题不变）。
- 里程碑：V40/50。
