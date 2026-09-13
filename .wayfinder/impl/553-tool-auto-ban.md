# impl 553 — ToolAutoBanHook（effort #800）

## 切片

- `buzhou-guard/src/main/java/.../guard/hook/ToolAutoBanHook.java` — order 255；滑窗失败环（Deque，WINDOW_RING=64）+ banUntilMillis；(session,tool) 键 `sid\0tool`，ConcurrentHashMap 封顶 256 + truncated；beforeTool 拦截（Block 带剩余秒）/afterTool 失败累计（封禁期内不累计）；snapshot() 排序不可变；Clock 注入（默认 systemUTC）；空 watch 集/未列名/null ctx 全 CONTINUE。
- `buzhou-guard/src/test/java/.../guard/hook/ToolAutoBanHookTest.java` — 可拨 MutableClock；8 例。

## 口径

- 失败 = `ctx.error() != null`（执行异常；beforeTool 被 block 不算失败——调用未发生）。
- 封禁触发后清空滑窗（封禁期内失败不累计）；到期惰性解除（无定时器）。
- 成功调用不重置滑窗（fail2ban 忠实；误封顾虑由阈值≥1 可配 + (session,tool) 粒度兜底）。

## 验证

mvn -pl buzhou-guard -am test -Dtest='ToolAutoBanHookTest' → 8/8 绿；
快照再生 `-Dbuzhou.api-snapshot.regenerate=true` → 恰 1 新公共类型（ToolAutoBanHook）入档。
