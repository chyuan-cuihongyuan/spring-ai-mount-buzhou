# 466 — gzip 导出压缩档位

**What to build:** ObservabilityJsonlExporter 三个 gzip 导出的 compressionLevel 重载（-1 默认/[0,9] 校验、匿名子类 def.setLevel）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三重载 + 校验 + 缺省兼容
- [x] 3 用例绿 + core 全模块 1761/1761 零回归
- [x] spec 613 + README 行

## Done

验证：`mvn -pl buzhou-core test` 绿。commit 见本轮 `feat(core)` 提交。
