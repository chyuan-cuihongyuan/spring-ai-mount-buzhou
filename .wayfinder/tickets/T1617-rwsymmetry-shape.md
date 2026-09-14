---
id: T1617
title: 读写对称守恒组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1615
created: 2026-09-15
---

## Question

J 会话第 81 轮：读面谱系可信度的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R46 write_file bytesWritten 与 R47 read_file bytesRead 双口径并存——**同内容写入后读回，两侧字节恒等**是读面谱系可信度的组合语义基础（组合语义先例：M 系 spec 1540/I 系 R52 纯测试轮）。纯测试轮零生产改动。

形状裁决：新增 `ReadWriteSymmetryTest`（buzhou-tools）——同内容 write_file 写入 read_file 读回：①bytesWritten == bytesRead（UTF-8 字节口径一致）②各自五桶/四桶守恒恒等式保持 ③中文多字节内容恒等（UTF-8 编码口径钉住）。零生产改动。
