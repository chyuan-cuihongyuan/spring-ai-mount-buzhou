# 664 — JCS 规范化内容指纹

**What to build:** canonicalContentFingerprint（递归键排序规范化 + sha256-j: 前缀）+ 键序漂移不变性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] canonicalContentFingerprint + CANONICAL_PREFIX
- [x] JcsFingerprintTest（键序漂移不变/嵌套递归/前缀区分/既有零变化）
- [x] spec 911 + README 行（欠账累计 906–911 六行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=JcsFingerprintTest` 全绿。commit 见本轮 `feat(core)` 提交。
