# 1169 — 工具失败负缓存

**What to build:** NegativeCachingToolCallback 装饰器（失败短 TTL 记忆）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] NegativeCachingToolCallback（LRU 封顶 + 异常同缓存 + 观测 Stats）
- [x] 测试暴露「成功清除不可达」缺陷后简化为纯 DNS 语义（恢复窗口=TTL）
- [x] 四断言全绿

## Done

验证：`mvn -pl buzhou-core test -Dtest=NegativeCachingToolCallbackTest`。
