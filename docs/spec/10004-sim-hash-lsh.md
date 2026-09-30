# Spec 10004 — SimHashLsh 位指纹海明分段索引（effort #10004，X5）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10009–X10010，impl 2407）。
> 借鉴：SimHash LSH（Manku–Das–Motwani 2007——Google 网页近似去重同源）

## Problem Statement

十亿级指纹库两两海明比对不可承受——
抽屉原理分块：64 位指纹均分 4 块、
距离 ≤3 必有一块全等——查近邻只读
4 张倒排表。

## Solution

SimHashLsh（core/concurrent）：add/
query/hammingDistance——blocks ∈ {2,4,8}
均分块表；查询取并集后逐 id 验距（结果
为精确集 id 升序）；越界距离（≥blocks
——抽屉原理界契约明示）/坏块数/负 id/
重复 id fail-fast。

## Testing Decisions

零漏检圣像：3000 指纹 200 查询与全库
暴力比对逐一对拍全等；位翻转变体全
回放；blocks=2/8 档验距；fail-fast 六面；
确定性双跑。

## Out of Scope

不做指纹计算（SimHashFingerprint 已占
不同面：本件指纹外部注入）；不做多表
哈希放大（b 布位带另立）；不做删除。

## Further Notes

与 MinHashSketch/SimHashFingerprint
（已占）同域不同面：分块索引近邻 vs
签名/指纹计算；Wave 1 收束件。
