---
Type: task
Status: closed
---
## Question

提示前缀缓存：规范形键有界 LRU + 命中率四计数 + 惰性装载。

## Resolution

done（2026-08-30）：impl-276；`cache/PromptPrefixCache`（get/put/getOrLoad/
invalidate/stats/hitRate）+ 红队 5 例（同异前缀计数/LRU 逐出诚实/装载一次/
校验 fail-fast/空请求零比率）。
