# 1194 — 负缓存 yml 装配

**What to build:** buzhou.core.negative-cache 适配 bean + 上下文 runner 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] BuzhouCoreAutoConfiguration 适配 bean（DisposableBean 关闭停用）
- [x] 三断言全绿

## Done

验证：`mvn -pl buzhou-core test -Dtest=NegativeCacheYmlAssemblyTest`。
