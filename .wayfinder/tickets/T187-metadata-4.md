---
Type: task
Status: closed
blocked-by: T175-weighted-canary.md, T176-shadow-fork.md, T177-model-pool-quota.md, T179-backoff-jitter.md
---
## Question

新键入档（流上限/shadow 开关与预算/池配额/权重等）+ 绑定验证。

## Resolution

impl-155 落地：元数据 7 键（fallback.canary-enabled/weights、shadow 四键、core.stream-total-timeout）
+ 绑定验证 4 测试（含 shadow.models 同名 bean fail-fast 口径）。**勘察纠偏**：绑定验证暴露
高严重度缺陷——Fallback/Circuit 多构造器嵌套 record 缺 @ConstructorBinding，`buzhou.resilience.
fallback.*`/`circuit.*` 全部 yml 键自 impl-57 起静默不生效（既有测试全编程式构造从未暴露）；
canonical 构造器补注解修复 + circuit 绑定回归测试。resilience 104 测试绿。T187 关闭。
