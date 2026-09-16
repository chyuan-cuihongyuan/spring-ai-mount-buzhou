# Spec 2019 — QoS 资源声明分级（effort #2019，R20）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3139–T3140，impl 1570）。
> 借鉴：K8s QoS Classes——按声明三分（Guaranteed/Burstable/BestEffort），
> 驱逐与保护由分级驱动。

## Problem Statement

会话/租户的资源声明（并发下限上限 / token 预算下限上限）分级缺位：
资源紧张时驱逐谁、保护谁逐实例拍脑袋；「保额保量」「有保底可突发」
「纯尽力而为」三类契约混在一起无从施策。

## Solution

`QosClassifier`（core/policy，纯函数零状态）：

- `ResourceRequest(request, limit)` record：单维声明（request=声明
  下限 0 未声明、limit=声明上限；request>limit 矛盾 fail-fast、
  负数 fail-fast——compact constructor 守约）；
- `classify(List)`：K8s 合成语义——空/全零声明 = BEST_EFFORT；全维
  request==limit>0 = GUARANTEED；其余（含混维：任一零声明或任一
  request<limit 拉低整体）= BURSTABLE；
- `evictionRank(QosClass)`：驱逐次序值（BEST_EFFORT=0 先让位 <
  BURSTABLE=1 < GUARANTEED=2 受保护）；
- `shouldYield(qos, protectedClass)`：紧张时让位判定（rank 低于保护
  线即让）。

## User Stories

1. 作为背压作者，驱逐按 evictionRank 升序——BestEffort 先让位，
   Guaranteed 受保护，不再逐实例拍脑袋。
2. 作为租户，声明即契约：保额保量（G）背压时不受挤，纯尽力（BE）
   自知先让。

## Testing Decisions

- 三态各例；混维拉低（G+零声明→B、G+B→B、纯上限→B）；驱逐序 0/1/2；
  让位判定四象限；矛盾/负声明/null fail-fast。

## Out of Scope

- 不接 SpawnGate/背压驱逐链（接线归后续轮）；不做运行时实测偏离
  分级（声明口径）。

## Further Notes

- 与 SpawnPriority（优先级）正交：优先级定执行次序，QoS 定保护层级。
