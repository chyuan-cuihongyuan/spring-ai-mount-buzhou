# Spec 2057 — 确定性散列公共件（effort #2057，R58）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3215–T3216，impl 1608）。
> 借鉴：DRY 收敛——六处同款内联散列的公共件化。

## Problem Statement

P 系五件（HLL/频率素描/布谷鸟/一致性哈希环/SimHash）各自内联同一份
FNV-1a 64 + splitmix64 终结实现——同一算法五份拷贝：改一处漏四处，
口径漂移风险（散列变则素描/环的键归属全变——兼容性灾难）。

## Solution

`DeterministicHash`（core/metrics，纯静态）：

- `hash64(String)`：FNV-1a 64（标准偏移基/质数常量）+ splitmix64
  终结混合——同输入同输出、无随机无种子、跨实例跨进程稳定（素描
  merge 与环归属的前提）；
- 五调用点收敛（私有内联删除，跨包 import）；**哈希值不变**——五件
  既有断言（含确定性/分布/回放用例）零改动通过即为同一性证明；
- 契约：s 非 null fail-fast；非加密口径（分布质量用，安全归 guard）。

## Testing Decisions

- 同一性：五件全部既有测试零改动全绿（36 用例——散列行为完全不变）；
  DeterministicHash 无独立测试（被五件传递覆盖）。

## Out of Scope

- 不做字节数组/long 重载（String 口径够用）；不做种子化变体（确定
  性即契约）。

## Further Notes

- 收敛轮模式：跨 spec 的同构拷贝提取公共件 + 既有测试同一性证明。
