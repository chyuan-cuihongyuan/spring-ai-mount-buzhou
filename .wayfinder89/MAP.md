# Wayfinder Map — Buzhou 租户隔离沙箱（effort #89，A 会话）

> fog 种子⑧「多租户 tenant scope」首片收口（存储/文件面）。B 会话并行轮次
> 互斥协议同 #86 MAP 登记。

## Destination

每租户独立文件沙箱：`tenants/<tenant>` 子根 + 严格收窄白名单——跨租户遍历
（相对/绝对）一律结构化拒绝；租户 id 白名单 fail-fast。

## Notes

- 借鉴 Milvus partition-key / OS chroot-per-tenant；隔离是「面收窄」不是新
  检查器——复用 FileSandbox 既有边界检查（含 realpath 软链面）。

## Decisions so far

- [租户沙箱 forTenant](tickets/T451-tenant-sandbox.md) — id 白名单
  `[a-z0-9][a-z0-9-]{0,31}`（无分隔符/无 ../无大小写歧义）+ 零追加白名单。

## Not yet specified

- 存储层 tenant 前缀（SessionStateStore/归档键）；租户维度的配额联动
  （VirtualKeys per-tenant 键约定）。

## Out of scope

- 租户认证/鉴权（调用方持有 tenant id 的信任边界）；quota 强隔离。

## Tickets

- [x] [T451 租户隔离沙箱](tickets/T451-tenant-sandbox.md)（impl-275）
- [x] [T452 收口提交](tickets/T452-tenant-sandbox-close.md)（impl-275）
