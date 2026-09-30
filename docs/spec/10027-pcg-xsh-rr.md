# Spec 10027 — PcgXshRr（effort #10027，X28）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10055–X10056，impl 2430）。
> 借鉴：PCG 家族（O'Neill 2014——pcg-random 同源；SplitMix64/XorShift64 已占家族互补）

## Problem Statement

朴素 LCG 低位短周期统计不过关——LCG 状态+输出置换的小状态高统计面。

## Solution

PcgXshRr（core/metrics，实例面）：64 位 LCG 状态（标准乘子+流奇增量）+XSH-RR 输出置换（异或移位+右旋）+流 ID 分离序列+种子预热一步；nextInt/nextDouble/nextLong 三读数面。

## Testing Decisions

确定性复现+16 桶 16 万抽卡方上界（15 自由度 <45）+异流序列分歧+双精度域+long 拼合。

## Out of Scope

不做 PCG-DXSM 变体；不做跳跃/_advance 状态面；不做种子派生 KDF。

## Further Notes

与 SplitMix64/XorShift64（已占）同域不同面：LCG+输出置换 vs 纯混合函数。
