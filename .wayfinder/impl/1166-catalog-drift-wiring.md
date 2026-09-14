# 1166 — 工具目录漂移看门狗接线

**What to build:** CatalogDriftHolder + HarnessAssembler 会话构造节拍接线 + 集成测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] CatalogDriftHolder（进程级基线 + WARN emitter + 测试替换面）
- [x] HarnessAssembler.assemble 目录组装处拍指纹
- [x] CatalogDriftWiringTest 三会话序列断言 + 既有 5 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest=CatalogDrift*` 全绿。
