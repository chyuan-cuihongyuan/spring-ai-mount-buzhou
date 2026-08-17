# impl-131 — 观测纪律收口

**What to build:** 模型名 tag 截断全落点统一；三方依赖版本分歧 CI 硬门禁。

**Blocked by:** None

**Status:** done

- [x] MetricTags.bound 公用 + circuit gauge/计数 + fallback from/to 落点
- [x] enforcer dependencyConvergence + banDuplicatePomDependencyVersions
- [x] json-schema-validator 钉 3.0.1（Spring AI 双路传递分歧收口）——resilience 87 绿 + 全仓 enforcer 通过
