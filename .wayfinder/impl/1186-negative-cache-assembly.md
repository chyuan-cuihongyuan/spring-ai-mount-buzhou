# 1186 — 负缓存装配面

**What to build:** NegativeCachingHolder + HarnessAssembler 包装链接入。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] NegativeCachingHolder（enabled/ttl/wrap）
- [x] HarnessAssembler 包装链末段接入
- [x] 两断言 + exec 包 249 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest='io.github.chyuan_cuihongyuan.buzhou.core.exec.**'`。
