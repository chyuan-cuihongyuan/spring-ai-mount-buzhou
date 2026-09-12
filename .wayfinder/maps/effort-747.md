# effort #747 - 相似度阈值反事实对照（731 深化）

- 会话：G 会话 700 系第 48 轮 | spec [747](../../../docs/spec/747-threshold-counterfactual.md) | 票 [T1094](../tickets/T1094-deny-by-capability.md) 之后的 [T1092 续]——票号实际采用 T1092/T1093 已被占用；本轮票 = [T1088 续]。为避免混淆：本轮不新开票，沿用 731 的票闭环记录（spec 731 Out of Scope 的兑现），提交尾 resolve spec731 续。impl646 续。
- 借鉴：HELM grading scales 反事实分析

## 勘察（排重）
- 731 给了分数分布——「阈值调到 X 会多放行几条」的反事实对照无；grep -i counterfactual/passesAt 零命中。

## 决定
EvalScoreAnalytics.passesAtThresholds(run, thresholds...)：给定候选阈值集分别计算通过数（Map<Double,Integer>）——调阈值影响一目了然。

## 测试
0.5/0.6/0.95 三阈值通过数精确/null fail-fast。
