# 20 — guard · CI 自动红队门（promptfoo）

**What to build:** guard 配置在 CI 里被批量攻击验证：examples 起 OpenAI 兼容测试 target，promptfoo 红队 nightly 跑（先观测不阻塞 PR），注入类攻击对准 spotlighting/canary/HITL 门。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] examples 暴露测试用 HTTP target（OpenAI 兼容端点包 agent loop，stub 模型驱动）
- [ ] 仓库 `redteam/` promptfoo 配置：注入类 plugins（prompt-injection/excessive-agency/tool-discovery/shell-injection 等）+ strategies（jailbreak 单/多轮）
- [ ] nightly GitHub workflow 跑红队、**不阻塞 PR**（观测期）；本地可离线跑说明
- [ ] 基线指标留档（攻击成功率/拦截率）供后续对比
- [ ] spec 07（Hook 护栏）或新增红队章节同步

> spec 12 §guard-20；[T48](../tickets/T48-guard-promptfoo-redteam-gate.md)。源：promptfoo 24,206★（红队唯一达标源）。
