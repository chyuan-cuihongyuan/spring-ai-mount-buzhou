# Wayfinder Map — Buzhou 可逆 PII 代管库（effort #507，E 会话第 8 轮）

> E 会话第 8 轮。勘察：PII 三缝（106/86/500）占位符化**不可逆**——
> 「展示层脱敏、服务端留原值」的可逆工作流（如同一用户会话内模型需要
> 完整号码回显给本人）空白。Presidio Vault（anon↔deanonymize）思想。
> 原语先行（411/413/505 同族），hook 集成留扩散。

## Destination

`guard.pii.PiiVault`：`vaultize(original)` → 稳定占位令牌
`[PII-VAULT:<16hex>]`（sha256(original|salt) 前 16 hex——同原值同令牌
天然去重存储）；`restore(text)` → 文本内令牌还原原值；TTL 惰性过期
（Clock 注入）+ maxEntries 有界逐出；线程安全（ConcurrentHashMap）。
统计：vaulted/restored/hit-unknown 计数 + snapshot()。salt 构造注入
（防离线字典反查令牌→原值）。装配：guard autoconfig `buzhou.guard.
pii.vault.{enabled, ttl, max-entries}` Binder 预绑 enabled=true 才出
bean（默认关）。

## Notes

- 号段：spec 507 / T765–T766 / impl-410。
- 借鉴源：Presidio Vault（anonymize↔deanonymize 对称操作）。
- 诚实边界：代管库本身是敏感面（内存持有原值）——TTL/有界/不出进程；
  hook 自动接线留扩散（宿主显式 vaultize/restore 自己控制时点）。

## Out of scope

- 106/86/500 钩子自动 vault 化（扩散轮）；跨实例共享代管库；
  加密落盘。

## Tickets

- [x] [T765 PiiVault 原语](../tickets/T765-pii-vault.md)
- [x] [T766 yml 装配与统计](../tickets/T766-pii-vault-assembly.md)
