---
Type: task
Status: closed
---
## Question

stream() 返回后未订阅则 in-flight 计数残留 +1（DefaultAgentSession:82 自认）——改为订阅时才计数（doOnSubscribe）或返回包装流惰性占位；close() 收口语义保留。验证：未订阅场景计数归零单测。

## Resolution

spec 50 §C / impl-149 落地：stream() 轮次占用惰性化（Flux.defer）——会话级校验保持调用时
fail-fast，acquireTurnSlot/onTurnStart/beforeTurn 移入订阅时；未订阅流零占用（既往残留 +1
锁死单飞闸至 close 的诚实边界消除）。顺序复订阅=重新开轮、在途并发订阅=单飞闸拒绝两语义钉住。
3 新测试绿 + 全仓 verify BUILD SUCCESS（core 334 / 全仓回归零破坏）。
