---
id: T6016
title: R 会话 R8 CRC-32C 的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6015]
created: 2026-09-23
---

## Question

R8 合同怎么逐一验绿？（spec 4007 / effort #4007 / R8）

## Resolution

**验证通过**：Crc32CTest 四测全绿——六标准检验向量
（0xE3069283/0xC1D04330/0x364B3FB7/空 0/0x8A9136AA/0x62A8AB43）；
区间=全量、子区=拷贝区间一致；单比特翻转必变 + verify 真假
两向；畸形三型 fail-fast。
