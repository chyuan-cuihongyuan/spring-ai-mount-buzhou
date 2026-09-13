# effort #821 — 目录 lint 严重度分级

- 会话：H 会话 800 系第 22 轮 ｜ spec [821](../../../docs/spec/821-lint-severity-grader.md) ｜ 票 [T1143](../tickets/T1143-lint-severity-grader.md)/[T1144](../tickets/T1144-lint-severity-grader-verify.md) ｜ impl574
- 借鉴：rust-clippy 分级体系（rust-lang/rust-clippy ≈13K star）——correctness/suspicious/style 三档语义

## 勘察（排重）

- ToolCatalogLinter：Finding(tool, rule, detail) 平铺——无严重度维。
- 746 图环（T985-986）：graph cycles 规则族不同面。
- grep -i `severity`：无 lint 域命中。

## 决定

`LintSeverityGrader`（core.exec，纯函数）：Severity{DENY/WARN/HINT}（rank 0/1/2，clippy correctness/suspicious/style 语义对齐）——默认映射 DUP→DENY（破坏分发）/NAME→WARN/DESC→HINT；grade 排序严重→轻（同档 tool/rule 典序）+三档计数；withRule 定制返回新实例（不可变风格，脏入参返回 this）；未知规则归 HINT 不猜测。阻断与否归调用方（按 denyCount 决策——分级只读）。

## 测试

默认映射+严重序三档/定制规则覆盖+未知归 HINT/withRule 脏入参返 this+不可变隔离（原实例 HINT、定制实例 DENY）/同档典序破平+null 跳过/空真——5 例全绿。

## 诚实边界

分级与 lint 行为解耦（不阻断——决策面）；未知规则 HINT 是保守默认；规则集运行期不可变（withRule 每次新实例）。
