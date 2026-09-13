---
id: T1545
title: fs 沙箱判定计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 45 轮：fs 沙箱判定计数读面（chroot escape detection 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 45 轮 = effort #1045 / spec 1045 / impl 795）：缺口成立——FileSandbox（spec 06 安全边界默认：read/write/copy/str_replace/run_command 共用的路径沙箱）resolve/resolveForWrite 判定全程零计数：**路径逃逸尝试**（.. 越界/软链逃逸/空路径）只有逐次 WARN 日志，无累计水位——逃逸尝试的频次与趋势（探测行为 vs 偶发笔误）不可判。落点 core/fs：实例级 resolutions/violations 两 AtomicLong（violation() 单点即计数点——resolve/resolveForWrite/absolutize/realpath 全部拒绝路径汇于此）+ 嵌套 record `SandboxVerdictStats(resolutions, violations)` + `stats()`。实例级；嵌套类型不动 API 快照；判定与异常语义逐位不变。
