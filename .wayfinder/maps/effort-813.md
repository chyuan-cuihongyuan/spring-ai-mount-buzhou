# effort #813 — 技能发布通道解析（dist-tag）

- 会话：H 会话 800 系第 14 轮 ｜ spec [813](../../../docs/spec/813-skill-channel-resolver.md) ｜ 票 [T1127](../tickets/T1127-skill-channel-resolver.md)/[T1128](../tickets/T1128-skill-channel-resolver-verify.md) ｜ impl566
- 借鉴：pnpm/yarn dist-tag（pnpm/pnpm ≈32K star）——同一包多通道标定并存、latest 指最高版本

## 勘察（排重）

- SkillStatus：DRAFT/PUBLISHED/DISABLED 生命周期——无通道维。
- SkillVersionConflictException：写冲突检测——解析读数不同面。
- grep -i `dist.?tag|channel`：无命中。

## 决定

`SkillChannelResolver`（skills，纯函数）：Entry(name, version, channel) 注册表——resolve(name, channel) 显式标定优先、未标定回退 latest、latest 缺失取全表最高；版本比较点分数字段逐段（短补 0）+prerelease 后缀段低于同基段（semver）+同通道重复标定收敛高版本；空通道归一 latest、脏条目（null/空名/空版本）跳过；names() 典序只读。解析只读——写入/审核归 SkillStore 写面。

## 测试

显式通道优先/未标定回退 latest/0.10.0>0.9.0 数值陷阱+短段补 0+prerelease 低于 release+latest 缺失取最高/同通道收敛高版本/空通道归一+脏条目跳过+unknown 空——5 例全绿（首跑抓两 bug：prerelease 段排序反了+List.of 拒 null——semver 语义修正）。

## 诚实边界

纯解析不发布（dist-tag 写面归 store 域）；版本比较支持点分数字+prerelease（完整 semver 范围表达式 out-of-scope）；通道名自由（不预设 stable/beta 白名单——dist-tag 原生语义）。
