---
id: T2411
title: R31 Wilson 置信区间的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2410
created: 2026-09-15
---

## Question

N 会话第 31 轮：区间报告用正态近似还是 Wilson score？

## Resolution

选 **Wilson score**（无连续性校正）。正态近似在 p̂ 接近 0/1 或 n 小时出负值
越界（w = p̂±z√(p̂(1-p̂)/n) 在 p̂=0 时 w<0）；Wilson 数学上夹在 [0,1]。
挂事件面不改落盘 summary record（旧记录 decode 零损）。
