# 1104 — HookChain 事件通知面逐 hook 隔离（M 系 R2）

**What to build:** fireEvent 链内逐 hook try/catch 隔离（通知面/裁决面分离），计时 try/finally 仍入账。

**Blocked by:** T2253 / T2254（同轮 shape+verify）。

**Status:** done

- [x] fireEvent 逐 hook 隔离（ERROR 日志：hook 名 + 事件类型 + 栈）
- [x] 计时 try/finally 入账（失败调用耗时不丢）
- [x] run() 裁决面 fail-fast 语义零变化
- [x] HookEventNotifyIsolationTest 三断言 + HookChainTest 零回归

## Done

验证：定向 9 用例绿。commit 见本轮 fix 提交。
