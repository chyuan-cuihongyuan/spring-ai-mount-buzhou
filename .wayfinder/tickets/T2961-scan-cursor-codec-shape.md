---
id: T2961
title: SCAN 游标编解码的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

键空间全周游游标的反转递增语义怎么计算面化？（spec 1880 / effort #1880 / R81）

## Resolution`

**Redis SCAN 位反转递增纯计算 `ScanCursorCodec`（core/cache）**：
nextCursor（v′=rev(rev(v)+1) 位反转空间 +1，绕满精确归 0）+
isComplete（0 判完成）+ cycleLength（2^bits 同尺寸全周游桶数）。
位宽 [1,63] 游标非负 fail-fast。落轮 grep 复核：仅 Lettuce 客户端
类型在用，白盒计算面无占坑。
