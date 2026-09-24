---
id: T6168
title: S 会话 S34 Content-Defined Chunking 内容定义分块的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6167]
created: 2026-09-24
---

## Question

S34 合同怎么逐一验绿？（spec 5033 / effort #5033 / S34）

## Resolution

**验证通过**：ContentDefinedChunkingTest 七测全绿——圣像边界
（304 块首八+末块钉住）；覆盖连续；min/max 约束；确定性；
单字节翻转前稳后变（CDC 局部性）；短数据单块；畸形
fail-fast。
