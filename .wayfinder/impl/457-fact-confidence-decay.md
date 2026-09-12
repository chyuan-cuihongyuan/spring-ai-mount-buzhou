# 457 — 事实置信度衰减

**What to build:** `Fact.confidence`（兼容构造默认 1.0、信封往返）+ `FactDecayPolicy`（半衰/下限）+ `DecayingFactStore` 读时过滤装饰器（opt-in，不写回）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Fact 扩 confidence + 信封兼容（旧格式读 1.0）
- [x] FactDecayPolicy 公式与校验；DecayingFactStore 装饰器（save/delete 直通）
- [x] DecayingFactStoreTest 5/5 + DefaultFactStoreTest 信封用例 7/7 绿
- [x] spec 604 + README 行

## Done

验证：`mvn -pl buzhou-memory -am test` 绿（memory 124/124 含新 5 用例；core DefaultFactStoreTest 7/7）。commit 见本轮 `feat(memory)` 提交。
