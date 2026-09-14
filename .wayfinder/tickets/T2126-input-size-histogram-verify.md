---
id: T2126
title: 入参直方落桶与守恒的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2125
created: 2026-09-14
---

## Question

如何证明边界落桶、守恒与只读零裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

E2E（同 1401 测试模式：脚本模型驱动 toolCall，工具回执即终结）：三尺寸入参（小/中/大 JSON）各落对应桶+executed=3 守恒+totalBytes 精确；直调最小替身：空参 {} 计 2 字节；reset 归零。等待全仓 verify 后编码。
