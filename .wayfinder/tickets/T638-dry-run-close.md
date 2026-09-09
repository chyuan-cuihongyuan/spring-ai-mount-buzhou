---
Type: task
Status: closed
---
## Question

回归与收口：hook 用例（拦入计划/全量 vs 清单/开关/有界溢出/afterTool
不动）+ 装配用例（enabled 装/默认不装/tools 绑定）；README 纵深 IV 加行
（spec 323）；PROGRESS 台账。

## Resolution

done（2026-09-02）：impl-346；hook 七 + 装配三用例绿；全模块 1238 中
1237 绿 + UnsubscribedStreamTest 存量时序 flaky 单跑绿（MVN_EXIT=0，
本地已知清单成员非新引入）。
