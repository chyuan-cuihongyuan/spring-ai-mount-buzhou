---
id: T914
title: 停机排水取消原因的真路径验证裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

Loop 7（spec 606）的 SHUTDOWN_DRAIN 接线只有「签名兼容」级回归（既有 shutdown 测试不断）——事件真带 cause 无 E2E 钉住。怎么补？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 33 轮 = effort #600 / spec 632 / impl 485）：

1. E2E：挂死模型在途 Turn + 优雅停机（shutdownGracefully ②步对在途会话发 AFTER_CURRENT_TURN）→ hook 链捕获 session.cancelled 断言 {cancelMode: AFTER_CURRENT_TURN, cause: SHUTDOWN_DRAIN}。
2. ④步硬截断（IMMEDIATE + SHUTDOWN_DRAIN）同一 cause 词汇——本用例钉②步即可覆盖事件面（④仅 mode 不同，词汇已闭集）。
