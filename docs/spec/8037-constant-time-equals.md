# Spec 8037 — ConstantTimeEquals（effort #8037，V38）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8075–V8076，impl 2339）。
> 借鉴：timing attack 防御（DJB/OAuth 思想）。

## Problem Statement

秘密比较的病：短路 == 泄漏前缀长度——**XOR 折叠全长度累积差**——分支与时序双无关。

## Solution

ConstantTimeEquals（core/crypto）：equals(byte[],byte[]) 常数时间布尔+equalsHex(String,String)+null fail-fast+确定性。

## Testing Decisions

等/不等/长度差三态+500 随机与 Arrays.equals 语义全等；fail-fast。

## Out of Scope

- 不做参数化变体（固定经典参数——明示）。

## Further Notes

- 勘误入档：原拟 Wave 7 第五件 ReedSolomon（GF(256) 算术面）
  实现预算超限退雾区——ChineseRemainder 补位（V42 后波次
  内调序——同族加密信任主题不变）。
- 里程碑：V38/50。
