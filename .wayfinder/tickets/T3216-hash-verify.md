---
id: T3216
title: 确定性散列公共件的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3215]
created: 2026-09-17
---

## Question

收敛后散列同一性怎么证明？（spec 2057 / effort #2057 / R58）

## Resolution

**同一性证明通过**：五件既有测试零改动 36/36 全绿（HLL 7+频率素描
6+布谷鸟 8+哈希环 7+SimHash 8——含确定性回放/分布界/环归属不变）；
首跑三连教训入档：①正则删内联未匹配 Hll 的 Javadoc 变体致声明被
调用点替换弄坏；②跨包（session/policy→metrics）缺 import；③
surefire 报告缓存可致旧绿误判——一律真实退出码判定。
