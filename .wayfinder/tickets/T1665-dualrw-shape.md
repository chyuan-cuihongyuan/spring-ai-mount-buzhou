---
id: T1665
title: 双档读写四象限对照组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1643
created: 2026-09-15
---

## Question

J 会话第 103 轮：读写双档对照的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R82 双档对照只钉住执行档差异——**读写对（write_file/read_file 无档位之分）跨直执行/沙箱两 RuntimeConfig 组装的对称恒等**未验证。纯测试轮第十四弹。

形状裁决：新增 `DualModeRwSymmetryTest`（buzhou-tools）——同内容分别经两组装装的 write→read 往返，bytesWritten/bytesRead 对称恒等在两组装下皆成立 + 双守恒。零生产改动。
