# effort #741 — 共享事实足迹读数

- 会话：G 会话 700 系第 42 轮 ｜ spec [741](../../../docs/spec/741-shared-fact-footprint.md) ｜ 票 [T1084](../tickets/T1084-fact-footprint.md)/[T1085](../tickets/T1085-fact-footprint-verify.md) ｜ impl641
- 借鉴：—（717/724 治理思想 owner 维度合流）

## 勘察（排重）

- InMemorySharedFactStore 有 deniedReads 拒绝计数——owner 维度分布（谁的事实在堆积、多少永生）无面；grep -i Footprint 零命中。

## 决定

`SharedFactFootprint.analyze(List<SharedFact>)` 纯函数——rows(owner, facts, eternal) facts 降序+字典序稳定+totalFacts/eternalFacts；值不读取（隐私口径只看元数据）。

## 测试
owner 归因降序+同数字典序/永生计数/空表 null。
