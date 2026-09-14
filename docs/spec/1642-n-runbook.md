# 1642 · N 系运维手册段（ops-runbook 第 23 节）

> 来源：N 会话 R43（effort #1642 / T2435–T2436 / impl 1195）。

## Solution

ops-runbook 追加第 23 节，四族覆盖 spec 1600-1641 的可运维机制：
- 缓存与限流族（LFU 采样/stale-if-error/梯度并发/spill 限速）；
- 熔断与降级族（warmup/慢调用/抖动模式/离群驱逐/影子对照/旁路遥测）；
- 护栏豁免族（危险工具/PII 双粒度/泄漏金丝雀）；
- 工具与观测族（http 护栏/负缓存/目录漂移/空闲水位/新鲜度/dashboard gzip）。
每条含配置键、观测读数、失控信号（运维三要素）。

## Out of Scope

- 每机制的详细排障流程（runbook 是入口索引——深排障链各 spec）。
