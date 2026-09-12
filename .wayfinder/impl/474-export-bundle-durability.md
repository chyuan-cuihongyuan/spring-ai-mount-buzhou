# 474 — 导出打包落盘持久档

**What to build:** ExportBundle.Durability 三档 + bundle 三参重载（FILE/FILE_AND_DIR force 语义）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 档位枚举 + force 实现（文件/父目录）
- [x] 2 用例绿 + core 全模块零回归
- [x] spec 621 + README 行

## Done

验证：`mvn -pl buzhou-core test` 绿。commit 见本轮 `feat(core)` 提交。
