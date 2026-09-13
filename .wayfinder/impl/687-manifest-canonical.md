# 687 — ExportManifest 规范化摘要

**What to build:** ExportManifest.addCanonical（复用 SessionExportChecksum 单点规范化）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] canonicalJson 单点提级 + addCanonical
- [x] ManifestCanonicalTest（键序漂移不变/verify 通过/旧 add 零变化）
- [x] spec 935 + README 行（欠账累计 926–935）

## Done

验证：`mvn -pl buzhou-core test -Dtest=ManifestCanonicalTest` 全绿。commit 见本轮 `feat(core)` 提交。
