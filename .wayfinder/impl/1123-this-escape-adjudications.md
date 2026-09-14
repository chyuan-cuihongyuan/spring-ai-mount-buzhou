# 1123 — this 逃逸修复 + 三裁定（M 系 R23）

**What to build:** AsyncObservabilityPipeline 惰性启动 + CLAUDE/指纹双裁定注记。

**Blocked by:** T2291 / T2292（同轮 shape+verify）。

**Status:** done

- [x] ensureDrainStarted CAS 惰性启动 + close isAlive 防御
- [x] CLAUDE 五-2 追认边界 + 指纹双轨裁定（Javadoc 交叉注记随下次触碰两文件时补）
- [x] observability 105 用例零回归

## Done

验证：定向测试绿。commit 见本轮 fix 提交。
