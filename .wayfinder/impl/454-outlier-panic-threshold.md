# 454 — 模型离群驱逐恐慌阈值

**What to build:** `filter` 剔除后健康候选跌破占比阈值（向上取整、严格低于）时忽略驱逐返回全量候选 + `buzhou.outlier.panic` 计数 + WARN；默认 0 = 关闭零行为变化；两参构造兼容保留。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Config 第三参 panicThresholdPercent（[0,100] 校验）+ 两参兼容构造 + withPanicAll 预设
- [x] filter 恐慌路径（ceil 取整、严格低于、空候选不触发、计数 + WARN）
- [x] 8 用例边界测试（ModelOutlierEjectionPanicTest）全绿
- [x] spec 601 + README 行

## Done

验证：`mvn -pl buzhou-core,buzhou-resilience -am test` 绿（resilience 222/222 含新 8 用例；既有 ModelOutlierEjectionTest 零回归）。commit 见本轮 `feat(resilience)` 提交。
