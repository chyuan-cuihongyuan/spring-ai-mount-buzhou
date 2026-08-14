---
Type: task
Status: open
blocked-by:
---
## Question

redteam F1 数值化与场景扩充怎么做？现状：promptfoo guardrails 断言拦截=pass，无 F1/数值汇总；baseline.md 无阈值。决策点：指标口径（拦截率/误拦率/F1 的定义与计算脚本）、场景扩充（新增 2-3 插件：pii-leak？CompetionCheck？沿用 promptfoo 内置插件清单核实）、阈值卡门（nightly 从观测升硬门的具体数值线）、baseline.md 格式升级。产出 spec 22 + impl 69。
