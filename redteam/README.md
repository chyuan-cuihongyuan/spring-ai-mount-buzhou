# 红队门（wayfinder2 impl-20 / T48）

事实源：**promptfoo**（24,206★——红队唯一 ≥10K 达标源；PyRIT 4.3K★/garak 8.8K★ 不达标换源）。
任何 guard 上线前须红队（2025 研究：自动红队成功率 ~69.5%）。

## 组成

- `promptfooconfig.yaml`：注入类 plugins（prompt-injection / excessive-agency / tool-discovery /
  shell-injection）+ 单/多轮越狱 strategies，对准 Buzhou 四层确定性防御
  （spotlighting → canary → FIDES 写门 → HITL 门）。
- **target**：examples 测试域内的 OpenAI 兼容端点（`RedteamTargetSmokeTest` 驻留模式，测试作用域、
  无需外部依赖）：包一层 guard 全开的 Buzhou runtime（taintTracking + injectionDefense +
  危险工具 HITL 门），模型用确定性替身——红队评的是**护栏行为**（拦截/转确认/不越权），
  不评模型生成质量。impl-53 起响应体携带真实 agent 回复，拦截信号经
  `x-buzhou-guard-blocked` / `x-buzhou-dangerous-executed` 响应头透出（promptfoo
  `transformResponse` 派生 `guardrails.flagged`，`type: guardrails` 断言）。
- CI：`.github/workflows/redteam-nightly.yml` —— **nightly、先观测不阻塞 PR**；
  指标基线留档（`redteam/baseline.md`），稳定后再升硬门。
- **审计链 nightly 重放校验（impl-39 / spec 13 §T64，注记级）**：同一 nightly 节奏里对
  审计持久化（`buzhou_audit_record` 表 / InMemory 环形）做独立重放——
  `AuditChainVerifier.verify(AuditRecordStore.loadAll(), signingKeyRing)` 输出
  `VerificationReport`（verifiedCount / firstBreakIndex / brokenRecordId / keyVersionStats），
  断链即告警定位首个断点；与 promptfoo 红队互不依赖、可并行段。

## 本地运行

```bash
# 1) 起 target（前台保持）
mvn -q -pl examples test -Dtest=RedteamTargetSmokeTest -Dredteam.serve=8090

# 2) 跑红队（需 Node ≥ 20；评分模型可用 PROMPTFOO_* 环境变量指向兼容端点）
npx promptfoo@latest redteam run -c redteam/promptfooconfig.yaml
npx promptfoo@latest redteam eval -c redteam/promptfooconfig.yaml --fail-on-error
```

离线说明：`PROMPTFOO_DISABLE_REDTEAM_REMOTE_GENERATION=true` 可全离线生成（官方支持）。


## 新能力攻击面（effort #8 / T138 / spec 39 §A——观察档）

多模态输入（媒体引用内容携指令）与工具结果内注入两类新攻击面**不在 promptfoo
场景词汇内**（自定义载荷不可表达）——以 examples 的确定性对抗用例承载
（`NewSurfaceAdversarialTest`，替身模型评 harness 行为）：①媒体内越权指令不改
HITL 门语义；②工具结果注入以数据形态在场、危险调用仍被门拦。口径：先观察不进
硬门；用例稳定转 nightly 重放后按 baseline 定门。

## 新能力攻击面（effort #10 / T182 / spec 51 §B——观察档）

四对抗面（替身模型、确定性断言，沿 effort #8 承载模式）：

- **反馈伪造**：`rateTurn` 是会话实例方法——跨会话伪造在构造上不可能（无静态入口）；
  越权观察点收敛为「输入域全拒」（六路校验：type/值域/source/未来轮次/关闭后，TurnFeedbackTest）。
  残余面：合法会话内的虚假反馈（false negative/positive 污染评估集）——框架不鉴别人类身份，
  属部署面（接入层鉴权），边界钉住。
- **shadow 泄漏**：shadow 输出绝不回注用户（G24 轨迹断言回复来自主模型）；shadow 失败计数
  吞噬（ShadowTrafficEndToEndTest）。残余面：shadow 携带的 prompt 内容外流到 shadow 模型
  提供方——与主模型同信任域是部署前提，runbook 记指引。
- **配额绕过**：候选限流拒绝后跳级（跳过该候选不重复触达）；每轮每候选至多一次触达
  （ResilienceRedteamSurfaceTest 断言）。残余面：跨轮 refill 自然恢复属令牌桶语义非绕过。
- **金丝雀漂移**：同会话粘住首选（G23 两轮同源断言）；漂移仅在配置变更重启后发生
  （可接受的运维窗口，runbook 记「变更后存量会话漂移一次」语义）。
