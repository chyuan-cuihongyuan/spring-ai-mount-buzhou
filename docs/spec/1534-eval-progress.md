# 1534 — 评估 run 进度读面

> 来源：M 会话第 38 轮 = effort #1534（impl 1137）。进度条（tqdm）思想——同步 run 的跨线程轮询面。

## 目标

`EvalRunner.progress()` → `EvalRunProgress(runId, done, total, cancelled)`：串行每项后/波间/占位分支三处 volatile 更新；done 含占位项；无活跃 run 为最近终态。

## 兼容性

纯新增读面零行为变化。
