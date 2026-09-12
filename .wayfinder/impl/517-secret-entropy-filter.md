# 517 — 秘密扫描熵阈值过滤

**What to build:** SecretScanner 可选熵阈值——命中文本 Shannon 熵 < 阈值丢弃（DEFAULT 4.0，PRIVATE_KEY_BLOCK 豁免），默认关零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 熵计算（字符频率 bits/char）+ 可选阈值构造
- [x] 高熵过/示例键滤/低熵滤/KEY_BLOCK 豁免/默认零回归用例
- [x] spec 714 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-guard -am test` 绿。commit 见本轮 `feat(guard)` 提交。
