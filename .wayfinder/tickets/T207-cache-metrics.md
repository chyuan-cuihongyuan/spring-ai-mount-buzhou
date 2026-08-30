---
Type: task
Status: closed
blocked-by: T203-cache-advisor-key.md
---
## Question

buzhou.resilience.response-cache.*（enabled 默认 false / max-entries / ttl）配置组 +
装配（@ConstructorBinding 预防）；hit/miss/evicted Function counter 注册；默认关零行为变化。

## Resolution

impl-172 落地：ResponseCache 第 14 组件（单构造器防 T187 盲区）；默认关零注入；hit/miss/
evicted 计数可读；metadata 3 键 + 绑定验证三态。resilience 113 绿。T207 关闭。
