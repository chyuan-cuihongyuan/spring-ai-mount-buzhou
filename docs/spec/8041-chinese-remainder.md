# Spec 8041 — ChineseRemainder（effort #8041，V41）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8083–V8084，impl 2343）。
> 借鉴：孙子算经「物不知数」/CRT 思想。

## Problem Statement

同余方程组的病：逐枚举模积域爆炸——**CRT：扩展欧几里得逆元加权合并 O(k·log M)**。

## Solution

ChineseRemainder（core/crypto）：crt(long[] remainders,long[] moduli)+两两互质校验（gcd≠1 fail-fast 带下标）+模溢出域明示+数组不齐/null fail-fast+确定性。

## Testing Decisions

孙子原题（2,3,2 mod 3,5,7→23）+500 随机双模 vs 枚举圣像全等+非互质 fail-fast；确定性。

## Out of Scope

- 不做参数化变体（固定经典参数——明示）。

## Further Notes

- 勘误入档：原拟 Wave 7 第五件 ReedSolomon（GF(256) 算术面）
  实现预算超限退雾区——ChineseRemainder 补位（V42 后波次
  内调序——同族加密信任主题不变）。
- 里程碑：V41/50。
