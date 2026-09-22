# impl 1511 — RandomEarlyDrop 随机早期丢弃（R111 = effort #1910 / spec 1910 / T3021-T3022）

**What**：`RandomEarlyDrop`（core/backpressure 静态纯函数 + Zone
枚举）——dropProbability 三段曲线（minTh 下 0/线性/maxTh 上 maxP）
+ zone 三区读数；minTh<maxTh/maxP∈(0,1]/队列非负 fail-fast。

**Why**：RED 经典 AQM（Sally Floyd/Van Jacobson）——尾丢把同时到
达的成批请求一起丢、TCP 重试同步回涌；早期概率丢摊开丢弃打破全局
同步。与负载脱落阶梯互补（过载后档位 vs 过载前概率）。

**Verify**：`RandomEarlyDropTest` 4 用例全绿（三区曲线/边界含下/
分区读数/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
