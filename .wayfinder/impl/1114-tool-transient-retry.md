# 1114 — 工具调用瞬断重试（M 系 R13）

**What to build:** RetryingToolCallback 装饰器 + transient 分类器 + 幂等门 + Holder 装配。

**Blocked by:** T2273 / T2274（同轮 shape+verify）。

**Status:** done

- [x] RetryingToolCallback（attempt 循环 + 指数退避 + 幂等门）
- [x] ToolTransientRetryPolicy + Holder + autoconfig 键
- [x] HarnessAssembler wrap 链外层装配
- [x] 静态计数读面 + ToolTransientRetryTest 五断言

## Done

验证：core 定向测试绿。commit 见本轮 feat 提交。
