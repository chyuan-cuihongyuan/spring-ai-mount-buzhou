# effort #844 — 摘要降级原因分布

- 会话：H 会话 800 系第 45 轮 ｜ spec [844](../../../docs/spec/844-summary-degrade-reasons.md) ｜ 票 [T1189](../tickets/T1189-summary-degrade-reasons.md)/[T1190](../tickets/T1190-summary-degrade-reasons-verify.md) ｜ impl597
- 借鉴：Envoy degraded 健康语义扩散（R45 计划题；SummaryDegrader 管线统计面）

## 勘察（排重）

- SummaryDegrader 接口：degradeToFit 执行——无原因统计。
- CompactionRatioStats：压实率——降级原因缺位。
- grep -i `degrade.*reason`：无命中。

## 决定

`SummaryDegradeReasons`（memory，synchronized 记账）：record(Reason)——五态闭集 OVER_LIMIT/GENERATION_FAILED/EMPTY_CONTENT/POLICY_FORCED/UNKNOWN 计数+占比；snapshot 占比降序平局声明序；null 忽略；空真。喂点=降级管线装配侧。

## 测试

计数+占比 0.6 降序/null 忽略+空真——2 例全绿。

## 诚实边界

喂点手动（不改 degrader）；五态语义归调用方归类；进程内存有界。
