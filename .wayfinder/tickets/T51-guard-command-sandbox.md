---
id: T51
title: guard · CommandSandbox 三档选型与默认
type: grilling
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

`run_command` 的爆炸半径隔离上哪档？事实源（三档全达标）：**Deno**（108,248★：deny-by-default、`--allow-read/net/env(变量名白名单)/run` 精细授权、跨平台、`--no-prompt` 未授权即抛）；**Firecracker**（36,040★：jailer 全参 `--uid/--gid/--cgroup/--chroot/--netns/--resource-limit/--new-pid-ns`、关 fd+清环境+降权、**需 root + Linux-only（KVM）**、启动约 125ms）；**E2B**（13,383★：Firecracker 云沙箱、OpenAPI REST、出网 allowOut/denyOut CIDR/域名、可自托管）。

## 待定决策（研究推荐 + 取舍）

1. `CommandSandbox` SPI + 三实现，**全部 optional 探测 + Linux gating**（`os.name` + `/dev/kvm` 探测）——推荐采纳。
2. 档位取舍（工作量/ROI）：**Deno 档 3–4 天（高 ROI，跨平台，开发环境默认指引）**；Firecracker 档 7–10 天（rootfs 准备大中，生产 Linux 场景）；E2B 档 4–5 天（无本地 KVM 的托管场景）——本轮是否三档全做、还是 Deno 先行其余按需？推荐：**SPI + Deno 档必做，Firecracker/E2B 档接口预留、实现按部署需求**（避免为无人使用的档位付 rootfs/云凭据成本）。
3. secret 脱敏：环境变量白名单透传（Deno `--allow-env` 语义对齐）——随 Deno 档做。
4. 与既有 FileSandbox/黑名单关系：SPI 化后旧机制降为「无沙箱依赖时的内联档」——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §5.4。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §guard-23**（用户常设授权 2026-08-14 ratify、可推翻）。CommandSandbox SPI+Deno 档必做；Firecracker/E2B 重载档接口预留按部署需求。
