# 821 — 目录 lint 严重度分级

> 来源：H 会话第 22 轮 = effort #821 / [T1143](../../.wayfinder/tickets/T1143-lint-severity-grader.md) / [T1144](../../.wayfinder/tickets/T1144-lint-severity-grader-verify.md) / impl 574。
> 借鉴：rust-clippy 分级体系（≈13K star）。

## Problem

ToolCatalogLinter 的发现平铺无轻重：「描述太长」与「工具重名」同列——运维无法按严重度决定阻断/关注/忽略。

## Solution

`LintSeverityGrader`（core.exec，纯函数）：

- **三档**：DENY（破坏性——重名破坏分发）/ WARN（可疑——命名反直觉）/ HINT（提示——描述质量）。
- **默认映射**：三内置规则各归其档；withRule 定制（新规则随轮登记，不可变风格）。
- **报告**：严重→轻排序（同档典序破平）+ denyCount/warnCount/hintCount。
- **未知规则**：归 HINT（不猜测严重性）。

## 兼容性

纯新增（ToolCatalogLinter 零变更——lint 输出喂 grade 即可）。

## 诚实边界

分级不阻断（决策面归调用方）；未知规则保守 HINT；规则映射运行期不可变。
