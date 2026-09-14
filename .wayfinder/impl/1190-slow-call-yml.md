# 1190 — 慢调用维度 yml 装配

**What to build:** Circuit 组扩参 + Module 传导 + 装配测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Circuit 12 参主构造 + 10/9 参兼容 + fail-fast + effectiveRate 读数
- [x] ResilienceModule withSlowCallPolicy 传导
- [x] 三断言 + resilience 396 用例零回归

## Done

验证：`mvn -pl buzhou-resilience test` 全绿。
