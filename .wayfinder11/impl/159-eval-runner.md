# 159 — 批次评估 runner

**Parent:** spec 52 §D / [T193](../tickets/T193-eval-runner.md)

**Status:** done

- [x] EvalRunner.run(datasetName, evaluator)：逐项 spawn 独立评估会话
  （`eval-<runId>-i<itemId>` 命名空间隔离）→ chat → 打分 → run 记录落合成会话
  `eval.run.<runId>`（与数据集同会话不同前缀段）
- [x] 单项执行异常记 error 不断批；评估器返回 null 按违约 error 记录；actual 预览 2048 截断
- [x] runId = `r<epochMillis>-<4位随机>`；passRate = passed/total（空集约定 0.0）
- [x] 空数据集零项 run 合法；未建 dataset fail-fast 挂 EVAL_OPERATION_INVALID
- [x] 3 测试绿（pass/fail/error 混合 + 记录往返同构 + 空集/未建 + null 评估器违约）
- [x] 勘察纠偏：ScriptedChatModel.throwOnCall 每次 call 优先消费（时序脚本陷阱）——
  测试改按调用计数替身；runner 本体无改动
