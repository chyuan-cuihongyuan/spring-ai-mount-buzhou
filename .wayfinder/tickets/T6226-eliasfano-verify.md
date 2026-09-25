---
id: T6226
title: T 会话 T13 Elias-Fano 单调序列编码的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6225]
created: 2026-09-26
---

## Question

T13 合同怎么逐一验绿？（spec 6012 / effort #6013 / T13）

## Resolution

**验证通过**：EliasFanoTest 五测全绿——八组密度用例往返
全等；低位宽公式逐例（1/20/6/0）钉住；密集 storedBits<
32×n；防御副本；fail-fast（初版整数除法算宽由密度例钉住
改 ceilDiv+严格 ceil-log2）。
