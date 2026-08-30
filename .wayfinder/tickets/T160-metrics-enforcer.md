---
Type: task
Status: closed
---
## Question

model tag 截断纪律只落一半（RateLimitAdvisor 截 32；ModelCircuitBreaker gauge 与 fallback from/to 未截）；enforcer 仅内对齐、无 dependencyConvergence：观测基数纪律与依赖治理收口如何定？

## Resolution

AFK 自决：(a) tag 截断抽 MetricTags.bound 公用（32/null→unknown），circuit 两 gauge + tripped/rejected
计数 + fallback from/to 全落点统一，RateLimitAdvisor 私有法委派；(b) enforcer 增 dependencyConvergence +
banDuplicatePomDependencyVersions；既有唯一分歧 json-schema-validator（Spring AI 双路 3.0.1/3.0.0）
经 dependencyManagement 钉 3.0.1 收口——全仓门禁通过。Prometheus 侧基数上限出界（部署域）。
产 spec 44 §B + impl-131。
