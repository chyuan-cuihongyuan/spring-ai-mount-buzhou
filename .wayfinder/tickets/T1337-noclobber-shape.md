---
id: T1337
title: write_file noclobber 防误覆盖模式的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 50 轮：write_file 对已存在文件无条件覆盖——noclobber 防误覆盖模式（csh `set -C` / `cp -n` 思想）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 50 轮 = effort #950 / spec 950 前插冲突→spec 951 / impl 698）：缺口成立——模型侧「想追加/想新建」的意图误写为覆盖时，原文件内容不可恢复。落点 `WriteFileTool`：opt-in 实例开关 `setNoclobber(true)`（默认 false=既有覆盖语义逐位不变）：noclobber 下目标已存在 → 返回失败消息「已存在（noclobber）」不执行写入（沙箱 resolveForWrite 之后、写盘之前判定 Files.exists——判定与写入间无 TOCTOU 危害：失败路径不落盘）。工具描述同步追加 noclobber 说明（模型可感知）。既有测试全绿 + 新增三态测试（新建成功/存在拒绝/关闭态覆盖成功）。
