# 716 — 告警规则 dry-run

> 来源：G 会话第 17 轮 = effort #716（借鉴 k8s admission `dryRun` / argo `sync --dry-run`）/ [T983](../../.wayfinder/tickets/T983-alert-dryrun-shape.md) / [T984](../../.wayfinder/tickets/T984-alert-dryrun-verify.md) / impl 519。

## 背景

告警规则集变更（新规则/改阈值/换 mechanism）只能上线实弹验证——误配（mechanism 拼错、for 窗不合理、规则风暴）直接打进通知通道。演练需要一个「推演一轮但不扣扳机」的读面。

## 目标

- `AlertRuleEngine.dryRun(Instant now)`：对每条规则读当前健康面，返回 `DryRunReport`：
  - `wouldFire`：DOWN 且（for=0 或窗口已满）且未在 firing——实弹将触发；
  - `wouldRecover`：firing 中但已 UP——实弹将发恢复；
  - `pending`：DOWN 且窗口未满——`PendingRule{ruleName, mechanism, downFor, remaining}`。
- **三不承诺**：不改 downSince/firing 状态机、不通知 listener、不发指标（dry-run 零副作用——演练不污染实弹状态）。
- 复用 `AlertFiring` 形态——通知通道消费逻辑可原样复用做演练展示。

## 非目标

不做 dry-run 规则子集选择（全量推演）；不做装配面/端点暴露（后续轮顺延）；不模拟静默窗/抑制门（gate 语义只属实弹路径——dry-run 报告「通知前会被门裁决」的交互动留 fog）。

## 测试

三分类正确；三不承诺（firingView 不变 / listener 未调 / 指标零事件）；dryRun 后 evaluate 实弹行为与无 dryRun 一致。

## 兼容性

additive 方法；实弹路径零变化。
