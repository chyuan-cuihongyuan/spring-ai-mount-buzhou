---
id: T1547
title: write_file 写入量水位与拒绝分桶读面（WriteFileStats）的形状裁决
type: task
status: closed
assignee: zcode-j
created: 2026-09-14
---

## Question

J 会话第 46 轮：tools/file 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块批量扫描法命中）：WriteFileTool 全部拒绝路径（noclobber 拒绝 / 超 8MB / 缺 content / 异常兜底）当前静默返回字符串——失败不可见，Sentry discarded events 拒绝分桶 + Dropwizard Meter 字节吞吐思想。

形状裁决：`WriteFileTool` 内静态 `AtomicLong` 七计数——attempts（call 入口）/ writes（成功）/ bytesWritten（UTF-8 字节累计）/ paramRejects（缺 content）/ oversizeRejects（超 8MB）/ noclobberRejects（noclobber 拒绝）/ failures（catch 兜底，含沙箱拒绝）；嵌套 `record WriteFileStats`（byXxx 派生 total）+ `stats()` 只读快照 + `resetForTest()`。守恒恒等式 `attempts = writes + paramRejects + oversizeRejects + noclobberRejects + failures`。静态面理由：工具实例由装配层新建，测试与宿主读面需绕开实例引用（FileSandbox.stats 同族先例）。

Out of scope：不改 call() 返回语义与既有字符串；不引入实例级 Meter 依赖；ReadFileTool 读侧留后续轮。
