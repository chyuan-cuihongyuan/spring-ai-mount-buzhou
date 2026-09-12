# Spec 507 — 可逆 PII 代管库（effort #507）

> wayfinder map：`.wayfinder/maps/effort-507.md`（T765–T766）。E 会话第 8 轮。

## Problem Statement

PII 三缝（106 输入/86 工具输出/500 回复流）占位符化**不可逆**：
「展示层脱敏、服务端留原值」的可逆工作流（同一用户会话内授权回显）做
不了。Presidio Vault 提供 anonymize↔deanonymize 对称操作——Buzhou 缺
代管库原语。

## Solution

`guard.pii.PiiVault`（原语先行，hook 集成留扩散）：

- `vaultize(original)` → `[PII-VAULT:<16hex>]`：令牌 = sha256(original
  |salt) 前 16 hex——同原值同令牌（存储天然去重）；salt 构造注入防
  离线字典反查。
- `restore(text)` → 文本内全部令牌还原原值；未知令牌原样保留 + 计数。
- TTL 惰性过期（Clock 注入）+ maxEntries 有界（超限逐出最旧）；
  ConcurrentHashMap 线程安全。
- 统计：vaulted/restored/unknown 计数 + snapshot()。
- 装配：`buzhou.guard.pii.vault.{enabled, ttl, max-entries}` enabled=true
  才出 bean（默认关；Binder 预绑——409 同法）。

## User Stories

1. 作为宿主，我想在展示层脱敏的同时服务端留原值，so 授权场景（同一
   用户回显）能按需还原。
2. 作为安全运维，我想代管库有 TTL 与容量上界， so 原值不无限滞留内存。

## Implementation Decisions

- 令牌形态 `[PII-VAULT:...]` 与 `[PII:TYPE]` 族可视觉区分（可逆 vs 不可逆）。
- 代管库本身是敏感面：进程内、TTL、有界、salt 防 Dictionary 攻击。
- restore 对已过期令牌原样保留（fail-safe 不炸调用方）。

## Testing Decisions

- vaultize→restore 往返恒等；同原值同令牌；不同原值不同令牌。
- TTL 过期（Clock 注入）restore 保留令牌原样；maxEntries 逐出。
- salt 不同令牌不同；yml enabled 装配/缺席。

## Out of Scope

- 钩子自动接线；跨实例共享；加密落盘。

## Further Notes

- 新公共类型 `PiiVault` 随轮 regenerate 快照 + api-surface.md 加行。
