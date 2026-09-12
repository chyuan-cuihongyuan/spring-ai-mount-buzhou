# 621 — 导出打包落盘持久档

> 借鉴：[sqlite](https://github.com/sqlite/sqlite) WAL synchronous 档位（OFF/NORMAL/FULL）。
> 来源：F 会话第 22 轮 = effort #600 / [T892](../../.wayfinder/tickets/T892-export-durability-shape.md) / [T893](../../.wayfinder/tickets/T893-export-durability-verify.md) / impl 474。

## 背景

导出打包（spec 317）close 即返——数据在 OS 页缓存；审计/合规归档场景需要「函数返回时数据已在设备上」的 FULL 语义（崩溃/掉电后归档完整可对账）。

## 目标

`Durability { NONE, FILE, FILE_AND_DIR }` 档位 + `bundle(zip, sources, durability)` 重载。

## 非目标

- Writer 型导出面（调用方自理）。
- 不做 fsync 批量合并调度。

## 设计

FILE = zip 文件 FileChannel.force(true)；FILE_AND_DIR 另开父目录 channel force（目录项落盘）。默认 NONE 零变化。

## 测试

2 用例：FILE_AND_DIR 产出完整可读 / NONE 与 null 保持既有路径。

## 兼容性

两参重载保留；新枚举纯增量。
