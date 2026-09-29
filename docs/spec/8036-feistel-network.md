# Spec 8036 — FeistelNetwork（effort #8036，V37）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8073–V8074，impl 2338）。
> 借鉴：Feistel 1973 DES/Lucifer 思想。

## Problem Statement

可逆置乱的病：全量 substitution 表巨大——**Feistel 结构：分半交替 L/R，轮函数可逆性不要求**。

## Solution

FeistelNetwork（core/crypto）：64 位块 16 轮+轮函数=SipHash24 密钥化派生+encrypt/decrypt 严格互逆+null fail-fast+确定性。

## Testing Decisions

roundtrip 500 随机块全等+雪崩（改 1 位密文位翻转 ~半）+确定性；fail-fast。

## Out of Scope

- 不做参数化变体（固定经典参数——明示）。

## Further Notes

- 勘误入档：原拟 Wave 7 第五件 ReedSolomon（GF(256) 算术面）
  实现预算超限退雾区——ChineseRemainder 补位（V42 后波次
  内调序——同族加密信任主题不变）。
- 里程碑：V37/50。
