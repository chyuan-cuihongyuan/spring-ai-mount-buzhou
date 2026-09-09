# Wayfinder Map — Buzhou 凭证租约（effort #105，B 会话第 17 轮）

> B 会话第 17 轮。主题池「凭证租约」：工具用的下游密钥常年驻留内存且永不过期
> ——泄漏面无限大。借鉴 Vault dynamic secrets（租约式凭证：TTL 到期自动失效，
> 用时签发/续租/吊销）。

## Destination

SecretLeases（core/exec）：per-name 租约——issue(name, value, ttl) 签发 /
resolve 惰性过期 / renew 续租（过期即拒）/ revoke 即吊销；计数可观测。
工具侧经它取密钥，泄漏面从「永久」缩到「TTL 内」。

## Notes

- 号段：B=奇数 spec（本轮 153）。
- 明文 value 在内存（进程内密钥仓域）；外部 KMS/Vault 对接留档。

## Decisions so far

- 过期续租拒绝（需重新签发——Vault 同语义，防无限复活旧凭证）。

## Not yet specified

- 外部 Vault/KMS 后端；自动续租回调；多版本凭证轮换。

## Out of scope

- 沿用 #7–#104；加密存储；按请求方隔离。

## Tickets

- [x] [T511 SecretLeases 签发/续租/吊销/惰性过期](../tickets/T511-secret-leases.md)（impl-289）
- [x] [T512 租约回归（TTL 内可用/过期失效/续租/吊销/隔离）](../tickets/T512-secret-leases-tests.md)（impl-289）
