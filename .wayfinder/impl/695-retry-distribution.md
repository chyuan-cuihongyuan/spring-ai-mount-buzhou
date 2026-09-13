# 695 — outbox 重试次数分布读面（第 46 轮切片补写）

**What to build:** WebhookOutbox.retryDistribution（attempts 分桶 TreeMap 升序）+ appendRetry 包级退避落盘。

**Status:** done

- [x] retryDistribution + RetryDistributionTest 全绿（对账轮补写切片）
