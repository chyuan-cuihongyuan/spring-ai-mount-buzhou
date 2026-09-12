# 741 — 共享事实足迹读数

> 来源：G 会话第 42 轮 = effort #741（410 事实库 owner 治理）/ [T1084](../../.wayfinder/tickets/T1084-fact-footprint.md) / [T1085](../../.wayfinder/tickets/T1085-fact-footprint-verify.md) / impl 641。

## Problem

共享事实按 owner 写入（410 所有权模型），但 owner 维度无分布读数——「哪个 agent 的事实库在无限膨胀、多少事实永生不过期」没有归因面。717 管冲突、本面管堆积。

## Solution

`SharedFactFootprint.analyze(List<SharedFact>)` 纯函数：rows(owner, facts, eternal)——facts 降序+同数 owner 字典序稳定；eternalFacts 总计（永生事实——724 TTL 治理在事实域的对偶）。值不读取（隐私口径：只看 key/owner/ttl 元数据）。

## Out of Scope
值内容审计（隐私）；自动清理（归宿主）。
