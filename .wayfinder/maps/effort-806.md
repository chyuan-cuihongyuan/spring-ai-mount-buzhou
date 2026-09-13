# effort #806 — 预算用量分位推荐

- 会话：H 会话 800 系第 7 轮 ｜ spec [806](../../../docs/spec/806-budget-recommendation.md) ｜ 票 [T1113](../tickets/T1113-budget-recommendation.md)/[T1114](../tickets/T1114-budget-recommendation-verify.md) ｜ impl559
- 借鉴：k8s VPA（kubernetes/kubernetes ≈115K star）——按观测用量分位推荐 resource requests

## 勘察（排重）

- TokenBudgetHook/PeriodBudget/ErrorBudget/ElasticBudgetPool：预算执行面（累计/硬顶/警告）——无「预算该设多少」推荐面。
- CostForecast：成本外推预测——时间序列外推非用量分位档位推荐。
- grep -i `recommend|percentile|p95`：评估分数分布 731 是评估域；预算域缺位。

## 决定

`BudgetRecommendation`（core.budget）纯函数 + Ring 收集器：recommend(samples, headroomPercent)——最近秩 P50/P95/P99（确定性）+ 推荐=⌈P95×(1+headroom/100)⌉；样本 <5 时 sufficient=false 且推荐位 -1 哨兵（样本不足不下结论）；null/负样本忽略；headroom 0..500 域外 fail-fast。Ring 容量 1024 FIFO 滑窗+dropped 计数（喂会话累计 tokens/单轮成本皆可——单位无关）。

## 测试

1..100 分位精确(50/95/99)+推荐 ⌈95×1.2⌉=114/headroom=0 即 P95/不足 5 样本哨兵+恰 5 边界 sufficient/负与 null 忽略/环 FIFO 挤出(2+1=3 dropped 断言修正后绿)+负样本忽略/fail-fast——6 例全绿。

## 诚实边界

P95 最近秩是工程口径（非插值）；推荐=单变量函数（不辨模型/时段维度——多维归调用方切片后喂）；-1 哨兵而非 Optional（record 紧凑性）；不自动改预算（VPA auto 模式留位——读数面不改行为）。
