# impl 1510 — GossipConvergence 闲谈收敛估算（R110 = effort #1909 / spec 1909 / T3019-T3020）

**What**：`GossipConvergence`（core/concurrent 静态纯函数）——
roundsToConverge（⌈log_{f+1} N⌉ 全量知情轮数）+ informedAfter
（min(N,(1+f)^r) 知情数封顶估计）+ fanoutFor（⌈N^{1/R}−1⌉ 反解）；
nodes/fanout/rounds 正值 fail-fast。

**Why**：SWIM/memberlist gossip 语义——传播轮数与心跳流量配比拍
脑袋（fanout 太小分钟级传播、太大挤占业务）；指数模型三函数互逆
可交叉验证，近似下界诚实入档。与 Phi 检测器互补。

**Verify**：`GossipConvergenceTest` 4 用例全绿（经典 5 轮/封顶/
反解 fanout=2/N=1 零轮与畸形三型 fail-fast）。

**Status**：done（2026-09-23）
