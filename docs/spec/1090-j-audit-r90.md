# 1090 — J 系阶段对账审计轮（R90 周期预检）

> 来源：J 会话第 90 轮 = effort #1090（[T1635](../../.wayfinder/tickets/T1635-r90-audit-shape.md) / [T1636](../../.wayfinder/tickets/T1636-r90-audit-verify.md) / impl 842）。每 10 轮周期纪律第七轮执行。

## 范围

J 系 R81–R89 全量工件对账 + 隔离 worktree 全仓 verify + 双门复跑 + 发现就近处置。

## 发现与处置

1. **README 1085 行吞噬复发（三型欠账之 README 死链型再现）**：首轮 verify SpecCoverageTest 红（1085 spec 无 README 行）——幂等补行脚本再修。根因不变：共享 README 各自基线重写；根治仍待 R44 拆分立项。
2. **台账 87/88 行黏连修复**：python 插入换行符缺失致两行同行——格式化修复。
3. **组合测试轮系列成型**：R81 读写对称/R82 双档对账/R84 fs 链路/R86 双守卫/R87 双工具/R88 双台账六轮组合语义验证，读面谱系可信度基础建立。
4. **元验证轮（R83）**：ReadoutContractSmokeTest 13 读面清单反射冒烟（非负/归零/稳定）——读面谱系的谱系。

## 验收

隔离 worktree（HEAD=R89 后干净基线）全仓 `mvn verify`：BUILD SUCCESS + 全模块 SUCCESS + 双文档门绿。

## Out of Scope

- 各并行会话（H/I/K/L/M/N）台账域对账。
- README 拆分根治（R44 立项维持）。
