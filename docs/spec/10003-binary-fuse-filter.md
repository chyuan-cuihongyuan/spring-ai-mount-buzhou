# Spec 10003 — BinaryFuseFilter 二进制熔合过滤器（effort #10003，X4）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10007–X10008，impl 2406）。
> 借鉴：Binary Fuse Filters（Lim–Graf–Lemire 2024——xorfilter 项目思想）

## Problem Statement

布隆逐键 10 位且位阵列随机访存——
静态键集场景需更省更快的第三形态：
三段熔合窗口+逆剥离构造，查询只读
三位点一次异或。

## Solution

BinaryFuseFilter（core/concurrent）：静态
构造（键集入构造器）——每键散列出同段
三窗口位点（段起点+slot×段长+段内偏移，
窗口带重叠=熔合）；8 位指纹=三位点异或；
构造走 3-均匀超图剥离（度 1 位点入栈
逆序反解），失败换种子重试 ≤100。

## Testing Decisions

零假阴性圣像（500/3000/70000 三档段长
全命中）；假阳性 <1%（8 位指纹理论
~0.39%）；重复/空集 fail-fast；确定性。

## Out of Scope

不做动态加键（静态构造契约——动态加
选 CuckooFilter 已占）；不做 16/32 位
指纹变体；不做压缩段（论文压缩形态
另立）。

## Further Notes

开发勘误入档：段密度系数首写 0.77（剥
离不可解下界之下——3-XORSAT 需 ≥1 槽/
键）百试不结，勘正 1.13 槽/键（论文值）
全档过。与 QuotientFilter（10002）同域
不同面：静态反解 vs 动态移位簇。
