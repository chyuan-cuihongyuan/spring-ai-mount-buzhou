---
id: T48
title: guard · CI 自动红队门（promptfoo）
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

任何 guard 上线前须红队（2025 研究：自动红队成功率 ~69.5%）——红队门用什么建？事实源：**promptfoo（24,206★，红队唯一达标源**；PyRIT 4,291★/garak 8,792★ 均不达标换源）：`redteam:` 段 plugins（prompt-injection/pii/excessive-agency/tool-discovery/shell-injection/sql-injection）+ strategies（jailbreak:meta 单轮 / jailbreak:hydra 多轮自适应）；target=任意 HTTP/OpenAI 兼容端点；`--fail-on-error` 控门禁；官方 GitHub Action；可本地 Ollama 全离线。

## 待定决策（研究推荐已备）

1. Buzhou harness 暴露测试用 HTTP target（examples 内起 OpenAI 兼容端点包一层 agent loop）——采纳。
2. 仓库内 `redteam/promptfooconfig.yaml`（注入类 plugins 优先，对准 spotlighting/canary/HITL 门）——采纳。
3. **nightly 流水线跑、先不阻塞 PR（观测期）**，指标基线稳定后再升硬门——采纳。
4. 攻击/评分模型：CI 用 stub/离线，本地开发可接真模型——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §5.1（2–3 天，ROI 高：性价比全榜第一）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §guard-20**（用户常设授权 2026-08-14 ratify、可推翻）。promptfoo nightly 先观测不阻塞 PR；examples 起 OpenAI 兼容 HTTP target。
