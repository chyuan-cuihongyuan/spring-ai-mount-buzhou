---
Type: task
Status: open
---
## Question

stream() 返回后未订阅则 in-flight 计数残留 +1（DefaultAgentSession:82 自认）——改为订阅时才计数（doOnSubscribe）或返回包装流惰性占位；close() 收口语义保留。验证：未订阅场景计数归零单测。
