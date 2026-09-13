# effort #737 — 响应缓存权重预算（701 姊妹扩散）

- 会话：G 会话 700 系第 38 轮 ｜ spec [737](../../../docs/spec/737-response-cache-weight-budget.md) ｜ 票 [T1074](../tickets/T1074-response-cache-weight.md)/[T1075](../tickets/T1075-response-cache-weight-verify.md) ｜ impl637
- 借鉴：Caffeine weigher（701 同思想在 ResponseCacheStore 的对称落地）

## 勘察（排重）

- ResponseCacheStore（53§D）同为按条数 LRU——大响应挤占问题与语义缓存同构；701 只做了 SemanticCacheStore。

## 决定

构造器扩 `maxWeightChars`（默认 0=关）：条目权重=SemanticCacheStore.estimateChars 同口径；put 替换同键先回收旧权重；写入后腾挪 eldest 至预算内；超预算单条拒存；weightEvictions 独立口径；TTL 过期即弃时回收权重。readouts：maxWeightChars/totalWeightChars/weightEvictionCount。

## 测试

腾挪 eldest/超预算拒存/同键替换回收/默认关零行为。
