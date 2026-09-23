---
id: T6014
title: R 会话 R7 Huffman 前缀码的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6013]
created: 2026-09-23
---

## Question

R7 合同怎么逐一验绿？（spec 4006 / effort #4006 / R7）

## Resolution

**验证通过**：HuffmanCodecTest 五测全绿——{100,50,20,10} 频率分层
码长 1/2/3/3；均匀四符号退化全 2 位；千字节三层倾斜流 roundtrip
+ bitCount<2000（均匀 8000 位对照）；单符号退化 1 位码 roundtrip
（5 位恰）；畸形八型 fail-fast。首版局部 record 作用域与测试频率
落位（0..n 而非 'a'+i）两处手滑已修正。
