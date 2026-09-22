# impl 1512 — ClientThrottleProbability 客户端自适应节流（R112 = effort #1911 / spec 1911 / T3023-T3024）

**What**：`ClientThrottleProbability`（core/ratelimit 静态纯函数）——
rejectProbability（max(0,(req−K×acc)/(req+1))，健康期恒 0 过载期
爬升）+ shouldDrop（dice 注入确定性回放）；计数非负/K≥1 fail-fast。

**Why**：Google SRE 自适应节流公式——后端过载时客户端照发不误是
重试风暴放大器；按历史接受比在客户端就地概率拒发，省一个网络
来回。与 RetryBudget（重试侧）互补：这是首发侧节流。

**Verify**：`ClientThrottleProbabilityTest` 4 用例全绿（健康 0/过载
0.198/极端 0.498/掷骰边界与畸形 fail-fast）。

**Status**：done（2026-09-23）
