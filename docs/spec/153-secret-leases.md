# Spec 153 — 凭证租约（effort #105）

> wayfinder map：`.wayfinder/maps/effort-105.md`（T511–T512）。借鉴：HashiCorp Vault
> dynamic secrets（租约式凭证——TTL 内有效、到期自动失效、可续租可吊销）。

## Problem Statement

工具（HTTP 调用/命令执行/DB 查询）用的下游密钥通常在装配期注入后永久驻留：
凭证泄漏面无限大、轮换要重启、撤销无从谈起。Vault 的解法是给凭证加租约——
到期自动失效，把「永久凭证」缩成「短命凭证」。

## Solution

`SecretLeases`（core/exec，进程内租约仓）：

- **签发**：`issue(name, value, ttl)` → leaseId（同名重签覆盖旧租约——旧值立即失效）。
- **解析**：`resolve(name)` → Optional\<value\>（过期惰性剔除 + expired 计数）。
- **续租**：`renew(name, ttl)`——仅在租期内有效（已过期 = IllegalStateException，
  需重新签发；防旧凭证无限复活——Vault 同语义）。
- **吊销**：`revoke(name)` 即刻移除（泄漏应急面）。
- 观测：issued / expired / revoked 计数 + `activeLeases()` 名单（不含值）。

## User Stories

1. 作为宿主，工具凭证改为每小时签发——泄漏的密钥最多活一个 TTL，轮换零重启。
2. 作为运维，发现泄漏 revoke 即断（不用等 TTL）。
3. 作为审计，issued/expired/revoked 三个数就是凭证周转率。

## Implementation Decisions

- Clock 注入；per-name 单锁；明文 value 仅内存（外部 KMS/Vault 对接留档）。
- leaseId = UUID（观测引用，不参与判定）。

## Testing Decisions

- TTL 内 resolve 得值；时钟推进过期 → empty + expired 计数；renew 续期后仍可用；
  过期后 renew 拒；revoke 即刻 empty；同名重签旧失效；隔离与 activeLeases。

## Out of Scope

- 外部 Vault/KMS；自动续租回调；加密静态存储。

## Further Notes

- 密钥生命周期：装配期常驻（既有）→ 租约（本轮）→ 外部仓（留档）。
