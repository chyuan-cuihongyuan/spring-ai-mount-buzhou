# 513 — 会话导出 unchanged 协商

**What to build:** contentFingerprint（内容投影 sha256，剔除 exportedAt；sha256-c: 前缀）+ SessionExportConditional.exportIfChanged（UNCHANGED 不外发 payload / EXPORTED 携新指纹；fail-open）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] contentFingerprint（canonical 投影 + sha256-c: 前缀）
- [x] SessionExportConditional（exportIfChanged + fail-open）
- [x] 异时戳 UNCHANGED / 内容变 EXPORTED / 垃圾头 fail-open / 指纹可区分用例
- [x] spec 710 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
