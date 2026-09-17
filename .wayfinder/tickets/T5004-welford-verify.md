---
id: T5004
title: Q 会话 R2 Welford 在线方差的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5003]
created: 2026-09-18
---

## Question

R2 合同怎么逐一验绿？（spec 3001 / effort #3001 / R2）

## Resolution

**验证通过**：WelfordAccumulatorTest 七测全绿——教材集
{2,4,4,4,5,5,7,9} 手算（mean=5/样本 32/7/总体 4）、空态三 NaN、
单点 sampleVariance NaN、分片 merge 三对账等价全量、merge 交换律、
merge 空向双恒等、1e9 偏移+{1,2,3} 抖动紧公差（1e-9——朴素公式
在此量级崩塌的对照证据）。**教训入档**：Σ(x−mean)² 首算误 28
（实为 32，9+1+1+1+0+0+4+16）——对照组期望手算二次复核再落笔
（P 系 BH 轮同款教训复演）。
