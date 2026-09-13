# 813 — 技能发布通道解析

> 来源：H 会话第 14 轮 = effort #813 / [T1127](../../.wayfinder/tickets/T1127-skill-channel-resolver.md) / [T1128](../../.wayfinder/tickets/T1128-skill-channel-resolver-verify.md) / impl 566。
> 借鉴：pnpm/yarn dist-tag（≈32K star）。

## Problem

技能灰度发布只有生命周期开关（DRAFT/PUBLISHED）：beta 用户与稳定用户看同一版本——「stable 停在 1.2、beta 尝鲜 2.0」的通道化缺位，回滚=全员回滚。

## Solution

`SkillChannelResolver`（skills，纯函数）：

- **注册表**：Entry(技能名, 版本, 通道)；同通道重复标定收敛最高版本。
- **解析**：显式通道 → 标定版本；未标定 → 回退 latest；latest 缺失 → 全表最高。
- **版本比较**：点分数字段逐段（短补 0）；prerelease 后缀段低于同基段（0-beta < 0，semver 语义）；免疫「10 < 9」字典序陷阱。
- **归一**：空通道→latest；脏条目跳过。

## 兼容性

纯新增解析器（喂 SkillStore 条目即可用）；SkillStore 零变更。

## 诚实边界

只读解析（发布/tag 写面归 store）；不解析 semver 范围表达式；通道名自由无白名单。
