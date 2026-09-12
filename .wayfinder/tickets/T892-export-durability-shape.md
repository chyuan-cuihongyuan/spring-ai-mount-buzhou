---
id: T892
title: 导出打包落盘持久档的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

sqlite WAL 的同步档位（OFF/NORMAL/FULL）是「性能 vs 崩溃后已写数据真的在盘上」的旋钮。导出打包（spec 317）close 即返——审计/合规归档需要 FULL 语义。档位怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 22 轮 = effort #600 / spec 621 / impl 474）：

1. `ExportBundle.Durability` 三档：NONE（默认零变化——页缓存语义）/ FILE（zip 数据+元数据 force）/ FILE_AND_DIR（另 force 父目录——崩溃后目录项可见，真 FULL）。
2. `bundle(zip, sources, durability)` 重载；两参重载 NONE 直通；null 档按 NONE。
3. 只对打包落盘面开档（Writer 型导出面由调用方自理——层次边界）。
