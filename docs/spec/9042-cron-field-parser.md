# Spec 9042 — Cron Field Parser 五域解析（effort #9042，W43）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9085–W9086，impl 2395）。
> 借鉴：Vixie cron（BSD cron——Quartz/Spring @Scheduled 同源的 50 年事实标准）

## Problem Statement

手写 if-else 时刻判断五维组合爆炸不可维护——
**cron 文法**：五域位集展开，全命中即触发。

## Solution

CronFieldParser（core/policy，实例类）：parse(expr)→
matches(五分量)；fieldValues 读数；*/区间/
步进/列表语法；周 7 归一。

## Testing Decisions

经典表达式锚（*/15/工作日组合/区间步进）；
fieldValues 确定性；fail-fast 十二面。

## Out of Scope

不做月份/星期名（纯数字域明示）；不做
6/7 域扩展（秒/年）；不做 next-fire 计算
（触发判定面）。

## Further Notes

与 HashedWheelTimers 同根不同面。
开发勘误：javadoc */n 提前终结注释+
final 初始化器与构造器赋值冲突。
Wave 8 第一件。
