# 1431 — 指标命名校验器

> 来源：L 会话第 32 轮 = effort #1431（票 T2163 / T2164 / impl 1085）。借鉴：Prometheus metric naming 规范（命名规则机器化——命名漂移在写入时显形而非看板对不上）。

## Problem Statement

仓内指标族命名规则（`buzhou.` 前缀 + 小写段）只存在于 starter 的测试门（MetricNamingGuardTest）：机制作者新写 counter/gauge 时无自查工具——命名漂移要到 starter 全量测试才发现，第三方/新机制作者无即时反馈。

## 目标

- `MetricNameAudit`（core/metrics，纯函数静态面，private 构造）：
  - `validate(String name)` → `record NameVerdict(name, violations)`；
  - 与 MetricNamingGuardTest 门规则**同源**（段规则 `^[a-z][a-z0-9-]*$` + `buzhou.` 前缀族）；
  - 违规类别闭集：EMPTY / WHITESPACE / PREFIX / SEGMENT_EMPTY / SEGMENT_CASE:段 / SEGMENT_CHARS:段——**首违不短路**一次看全；
  - `compliant()` 派生（violations 空）。
- 纯函数零状态：校验器不接 BuzhouMetrics 写入路径（运行期埋点接驳另轮）。

## 兼容性

纯函数零 IO；规则与 starter 门同源（改门须同步——单源化另轮）。

## Out of Scope

- starter 门测试的单源化重构（test 常量引用公共类——动 starter 测试域另轮）。
- tag key 命名规则（本轴只管指标名）。
- 运行期写入拦截（BuzhouMetrics 装饰另轮）。
