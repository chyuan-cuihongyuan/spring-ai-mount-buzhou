# 672 — 加密导出×审计×指纹联动 e2e

**What to build:** EncryptedExportE2ETest 四场景（密文 fail-closed / 全链咬合 / nonce 稳定 / 零回归）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四场景编排测试
- [x] spec 919 + README 行（欠账累计 906–919 十四行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=EncryptedExportE2ETest` 全绿。commit 见本轮 `test(core)` 提交。
