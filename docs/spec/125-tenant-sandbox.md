# Spec 125 — 租户隔离沙箱（effort #89）

> wayfinder map：`.wayfinder89/MAP.md`（T451–T452）。#85 fog 种子⑧「多租户
> tenant scope」文件面首片。借鉴：Milvus partition-key / OS chroot-per-tenant。

## Problem Statement

多租户宿主共用一个文件沙箱根时，任一租户的会话可以触达全根路径——租户 A 的
read_file 能拼出 `../tenants/B/...` 读走租户 B 的落盘数据。

## Solution

`FileSandbox.forTenant(root, tenant)`：root 下 `tenants/<tenant>` 子根的沙箱，
<b>不带任何追加白名单</b>——租户面严格窄于宿主面。跨租户遍历（相对 `../` 或
绝对他租户路径）被既有边界检查拒绝（含 realpath 软链面，零新检查器）。
租户 id 白名单 `[a-z0-9][a-z0-9-]{0,31}` fail-fast：租户维度进路径，分隔符/
`..`/大小写歧义不校验就是穿越漏洞。

## User Stories

1. 作为多租户平台运维，我给每个租户一个独立沙箱，所以租户 A 的会话在文件面
   无法读写租户 B 的任何落盘数据。
2. 作为宿主开发者，非法租户 id（大写/分隔符/超长）在构造期即被拒，所以路径
   拼接面不引入新攻击向量。

## Testing Decisions

- 红队：相对/绝对跨租户穿越 + 写面拒绝；id 白名单 fail-fast 全象限（含首字符
  与 32 位封顶合法面）；零追加白名单（宿主根直连路径也拒）。

## Out of Scope

- 存储层 tenant 键前缀（SessionStateStore/归档——后续 fog）；租户认证/鉴权；
  per-tenant 配额强隔离。

## Further Notes

- 与 spec 124（VirtualKeys）组合：`<tenant>:<key>` 键约定即得 per-tenant
  配额分账，无需新机制。
