# Spec 121 — 错误签名窗口化清零（effort #83）

> wayfinder map：`.wayfinder/maps/effort-83.md`（T435–T436）。spec 112 fog 项收口。

## Problem Statement

错误签名表（spec 83）只增不减：长期运行进程内表趋近 256 封顶（overflow 吞新族），
趋势分析无法按窗口切分（今日 vs 昨日）。

## Solution

`ErrorSignatures.reset()`：counts 清零。export → reset 循环 = 每窗口一份 JSONL
（OLAP 侧时间列定窗口）、进程内表永有界；清零后新窗口照常计数。运维 cron 驱动
（不自装调度——fsck/audit 同纪律）。

## User Stories

1. 作为 SRE，我按窗口导出趋势对比，所以「本窗口新增错误族」可辨。

## Testing Decisions

- 计数/快照/top 三面清零 + 新窗口照常计数。

## Out of Scope

- reset 审计事件；自动窗口。

## Further Notes

- 与 spec 112 组合：export（全量）+ reset（切窗）= 错误族时序数据管线。
