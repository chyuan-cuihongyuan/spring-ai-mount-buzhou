# 19 — Redis 存储实现

**What to build:** buzhou-store-redis 过同一套 SPI 契约测试（Testcontainers Redis）；Lua/MULTI 原子批实现 unit-of-work；key 布局与 TTL 策略符合 08 spec；轻量 KV 部署场景可用。

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] 契约套件在 Redis 实现上全绿
- [ ] unit-of-work 经 Lua 原子提交/回滚有测试
- [ ] 文档写明 Redis 语义边界（过期/持久化注意项）
