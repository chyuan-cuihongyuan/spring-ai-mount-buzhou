# Spec 10002 — QuotientFilter 商指纹过滤器（effort #10002，X3）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10005–X10006，impl 2405）。
> 借鉴：Quotient Filter（Bender et al. 2012——Facebook/ScyllaDB 原型同源）

## Problem Statement

布隆无法删除、布谷鸟踢箭换巢复杂——
第三形态：指纹分家（商定槽+余定内容）
+三 meta 位（occupied/continuation/shifted）
支撑 run/cluster 移位结构。

## Solution

QuotientFilter（core/concurrent）：add/
mightContain/size/capacity——run 按 home 序
聚簇、rank 游走定位；指纹黄金比例混洗；
重复拒绝（集合语义）；满容/尾满 fail-fast。

## Testing Decisions

无误报否定圣像（1500 键全命中）；误报率
上界（r=12 位 <2% 实测 ~0.02%）；高载簇
压力 350 键全回放；集合真值单边对拍
（否定即必不在）；满容 fail-fast；构造
参数域 fail-fast；确定性双跑。

## Out of Scope

不做删除/扩容（Bender 全文另立）；不做
环绕移位（表尾簇右余量边界——文献已知
装载契约入档：建议水位 ≤50%）；不做计数
变体（Counting QF 另立）。

## Further Notes

开发勘误两处入档：①新 run 分支先置
occupied 再查 isTaken 自咬——改为先判
cont/shifted 自由位；②簇插入游走停 run
起点即 +1 插入——长 run 中段被劈开孤儿
化尾部——补推至 run 末槽。与 CuckooFilter
（已占）不同面：移位簇 vs 双桶踢箭。
