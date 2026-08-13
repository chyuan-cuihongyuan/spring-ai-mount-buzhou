# 红队门（wayfinder2 impl-20 / T48）

事实源：**promptfoo**（24,206★——红队唯一 ≥10K 达标源；PyRIT 4.3K★/garak 8.8K★ 不达标换源）。
任何 guard 上线前须红队（2025 研究：自动红队成功率 ~69.5%）。

## 组成

- `promptfooconfig.yaml`：注入类 plugins（prompt-injection / excessive-agency / tool-discovery /
  shell-injection）+ 单/多轮越狱 strategies，对准 Buzhou 四层确定性防御
  （spotlighting → canary → FIDES 写门 → HITL 门）。
- **target**：examples 测试域内的 OpenAI 兼容端点（`RedteamTargetServer`，测试作用域、
  无需外部依赖）：包一层 guard 全开的 Buzhou runtime（taintTracking + injectionDefense +
  危险工具 HITL 门），模型用确定性替身——红队评的是**护栏行为**（拦截/转确认/不越权），
  不评模型生成质量。
- CI：`.github/workflows/redteam-nightly.yml` —— **nightly、先观测不阻塞 PR**；
  指标基线留档（`redteam/baseline.md`），稳定后再升硬门。

## 本地运行

```bash
# 1) 起 target（前台保持）
mvn -q -pl examples test -Dtest=RedteamTargetSmokeTest -Dredteam.serve=8090

# 2) 跑红队（需 Node ≥ 20；评分模型可用 PROMPTFOO_* 环境变量指向兼容端点）
npx promptfoo@latest redteam run -c redteam/promptfooconfig.yaml
npx promptfoo@latest redteam eval -c redteam/promptfooconfig.yaml --fail-on-error
```

离线说明：`PROMPTFOO_DISABLE_REDTEAM_REMOTE_GENERATION=true` 可全离线生成（官方支持）。
