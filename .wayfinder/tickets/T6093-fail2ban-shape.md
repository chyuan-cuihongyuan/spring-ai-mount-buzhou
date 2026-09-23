---
id: T6093
title: R 会话 R47 fail2ban 封禁递升的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

反复滥用者怎么滑窗判禁并按前科递升？（spec 4046 /
effort #4046 / R47）

## Resolution

**BanEscalation（core/policy）**：fail2ban maxretry/findtime/
bantime.increment 思想——滑窗失败满限即禁（陈账过期洗白），
禁期 = base × multiplier^(banCount−1) 封顶 maxBanTime（惯犯
成本递增）；禁期封锁即足够、解禁重计；时钟注入确定性。
