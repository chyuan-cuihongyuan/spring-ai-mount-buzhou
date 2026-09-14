# 904 — 低覆盖类批次 1（R2）

**What to build:** guard `PolicyGateHook` 与 memory `RecallSearchTool` 两个低覆盖类的直测套件——三态裁决映射 / taint label 组装 / 事件字段 / 指标分桶；四模输出格式 / 摘要截断 / 降级与失败文案 / 轮次窗。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] guard：PolicyGateHookTest（9 用例：三态映射 + Input 组装 + taint 映射 + policy.decided 事件 + 指标三桶 + 常量合同）
- [x] memory：RecallSearchToolTest（12 用例：四模格式 + 空白归一/160 截断 + 无命中/缺上下文/非法 mode 文案 + EMBEDDING/HYBRID 降级与可用 + time 倒序 + limit/轮次窗 + 单参委托 + 默认 mode）
- [x] spec 1201 + README 行
- [x] JaCoCo 复扫：PolicyGateHook 99%（cov 116 / mis 1）、RecallSearchTool 99%（cov 230 / mis 2）；guard 331 / memory 172 全量绿
- [x] 测试显形缺陷：T1808 指标注释 tag 值失真（注释更正，独立 commit，零行为变化）

## Done

验证：两靶点脱离低覆盖档（均 99%），模块全量绿，主代码零行为变化。commit 见本轮 `test(guard,memory)` 与 T1808 `docs(guard)` 提交。
