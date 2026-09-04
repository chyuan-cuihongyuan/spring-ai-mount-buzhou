# Spec 334 — 成本归因台账（effort #334）

> wayfinder map：`.wayfinder335/MAP.md`（T659–T660）。C 会话第 35 轮。

## Problem Statement

成本可观测已有两轴：per-model 全局账（哪个模型在烧钱）与 per-session
会话闸（这个会话花了多少）。chargeback 场景缺第三轴：**按虚拟 key 归因**
——「哪个租户/哪条 key 在烧钱」没有任何账面可答，多租户成本分摊只能
靠事后猜测。

## Solution

`CostAttributionLedger`（Kubecost/OpenCost 按标签归因思想）：

- **双维归因**：同一笔 microUsd 同时记入 MODEL 维与 VIRTUAL_KEY 维
  （维度枚举开放扩展）——一笔账多把刀。
- **诚实桶**：无虚拟 key 时归 `__unattributed__`（未接入 key 的部署
  成本不蒸发）；维值封顶 64 折 `__overflow__`（ModelCostLedger 同纪律）。
- **报表面**：`rollup(dimension)` → 归因行（microUsd 整数精确口径 +
  usd 6 位小数人读口径 + sharePercent 万分比整数——无浮点漂移），
  microUsd 降序 + 名字典序稳定排序；`totalMicroUsd()`。
- **窗口纪律**：`reset()` 清零（export → reset 循环——导出族同纪律）；
  全局旋钮模式（global/install——与 ModelCostLedger 一致）。
- **JSONL 导出**（导出族新员）：一行一归因行
  `{dimension, value, microUsd, usd, sharePercent}`；空账零行诚实。
- **打点**：TokenBudgetHook afterModel 记账点（model + virtualKey +
  costMicroUsd 三元组已在手）——与 ModelCostLedger 全局账同点双记。

## User Stories

1. 作为平台运营，我想按虚拟 key 归因模型成本，所以 多租户 chargeback
   有账面依据而非事后分摊。
2. 作为平台运营，我想同一笔账同时有模型维与 key 维视图，所以 「换模型
   省钱」与「谁在花钱」两个问题一张台账都能答。
3. 作为财务，我想 usd 与 share 都是精确口径（整数/万分比），所以 对账
   不受浮点漂移之害。
4. 作为运维，我想未接 key 的成本进诚实桶而非蒸发，所以 总账对得上
   全局模型账。
5. 作为运维，我想导出后 reset 开新窗口，所以 每窗口一份归因报表
   （导出族同纪律）。
6. 作为使用者，我不想配虚拟 key 时归因面照常工作（全归
   __unattributed__），所以 零配置升级零风险。

## Implementation Decisions

- 新公共类型 `CostAttributionLedger`（嵌套 `Dimension` 枚举与
  `Attribution` record）+ `CostAttributionJsonl`，均落 core.budget。
- sharePercent = microUsd × 10000 / total（long 整除——万分比）；总数
  为零时 share 全零。
- 打点零参数侵入：hook 在 ModelCostLedger.record 同点追记归因全局账。

## Testing Decisions

- 台账：双维同笔入账 / 无 key 归桶 / 封顶折溢出 / share 万分比与
  排序稳定 / reset / 零成本也记。
- 导出：行形状（usd/share 列）/ 空账零行 / 两维导出。
- 接线：TokenBudgetHookEndToEndTest 既有 FakeChatModel 路径追加归因
  断言（打点入账）。

## Out of Scope

- skill/tool 维归因（喂点缺上下文——后续轮）；chargeback 拦截（315 已有
  限额）；账仓持久化（export→reset 即窗口答案）；健康面（ModelCostHealth
  同法可扩散）。

## Further Notes

- 新公共类型随轮 regenerate API 快照。
