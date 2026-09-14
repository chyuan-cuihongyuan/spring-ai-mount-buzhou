# 1163 — 离群驱逐生产接线 + 分类感知

**What to build:** 孤类救活——advisor 全路径喂入 + 候选过滤 + outlier.enabled 装配 + 分类感知。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ModelOutlierEjection：Config.failureCategories + recordError(name, category) 分类过滤
- [x] ResilienceAdvisor：withOutlier 注入 + 10 处喂入（5 成功 5 终态失败）+ 两处候选过滤
- [x] ResilienceProperties.Outlier 组 + ResilienceModule 进程级装配
- [x] ModelOutlierEjectionCategoryTest 四断言 + resilience 377 用例零回归

## Done

验证：`mvn -pl buzhou-resilience test` 全绿。
