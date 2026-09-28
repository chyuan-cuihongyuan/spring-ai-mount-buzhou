# -*- coding: utf-8 -*-
import io

def w(path, content):
    io.open(path, 'w', encoding='utf-8', newline='').write(content)

# ============ spec 6045 ============
w('docs/spec/6045-wait-for-graph.md', '''# Spec 6045 — WaitForGraph 等待图死锁检测（effort #6045，T45）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6289–T6290，impl 2245）。
> 借鉴：DB2/SQL Server 锁管理器 lock-wait graph 思想。

## Problem Statement

死锁处置的病：事后全图扫描（死锁扩大化才被发现）与
全局超时轮询（无差别惩罚无辜等待者）——**增量加边
即时环检测 + 受难者裁决面**缺失。

## Solution

`WaitForGraph`（core/concurrent）：

- 节点=事务，有向边 waiter→holder 表示「等待」；环即
  死锁——加边即时回报规范环（起点/邻接按 id 升序 DFS +
  环内最小 id 旋转到首位——同图同环完全确定）；
- 受难者 = 环内最大 id（「最年轻者回滚代价最小」约定）；
  removeNode 打断（出入边全清、计数守恒）；
- addNode/removeNode/nodeCount/edgeCount/hasDeadlock/
  deadlockVictim 读数；未注册端点/自环/重复节点/缺席
  摘除 fail-fast。

## User Stories

1. 作为锁管理器作者，加边瞬间得知死锁并裁剪受难者。
2. 作为审计作者，同图同环——检测完全可复现。

## Testing Decisions

- 无环链零误报；二环/三环带入口路径规范环逐值钉住；
  打断后复原再扩展；fail-fast 四路。

## Out of Scope

- 不做代价模型受难者选择（固定最年轻约定）；不做并发安全。

## Further Notes

- 与 TarjanSccFinder（同包）同族不同面：离线全图强连通
  分量 vs 增量加边即时环检测+受难者裁决。
- 勘误：removeNode 初版漏减被摘节点出边计数 + 规范环未
  旋转双缺陷由合同钉住修正。
- 里程碑：T45/50（90%）。
''')

# ============ spec 6046 ============
w('docs/spec/6046-van-emde-boas.md', '''# Spec 6046 — VanEmdeBoas 有界宇宙树（effort #6046，T46）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6291–T6292，impl 2246）。
> 借鉴：van Emde Boas 1975 有界宇宙树思想。

## Problem Statement

有界小整数全域（会话 id、槽位号）高频后继查询的病：
有序结构 O(log n) 后继——**O(log log u) 递归分簇面**缺失。

## Solution

`VanEmdeBoas`（core/concurrent）：

- 宇宙 2^bits（bits∈[1,20]）按半位分簇递归 + summary
  摘要；min 只在节点镜像、max 每层冗余（CLRS 约定）；
- 簇与 summary 惰性创建（稀疏集不预支全域内存）；
- insert（集合语义重复幂等）/delete（缺席 fail-fast）/
  contains/successor（缺席 -1）/minimum/maximum（空集 -1
  诚实）/size/isEmpty/universeSize；越域与位宽越域 fail-fast。

## User Stories

1. 作为调度作者，小整数全域 O(log log u) 后继取下一个
   可用槽位。
2. 作为审计作者，400 随机操作与 TreeSet 圣像五面逐步
   全等——行为可证。

## Testing Decisions

- 种子化 400 随机操作 vs TreeSet 圣像（insert/delete/
  contains/successor/最值五面逐步全等）；2/4/1M 三档宇宙
  端到端；稀疏插入；fail-fast 四路。

## Out of Scope

- 不做 merge/split；不做并发安全；bits>20 超会话语义
  明确拒绝。

## Further Notes

- 与 IndexedHeap（6026）同族不同面：优先级队列
  decrease-key vs 有界宇宙后继查询。
- 里程碑：T46/50（92%）。
''')

# ============ spec 6047 ============
w('docs/spec/6047-power-of-two-choices.md', '''# Spec 6047 — PowerOfTwoChoices 二择一负载均衡（effort #6047，T47）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6293–T6294，impl 2247）。
> 借鉴：Mitzenmacher「The Power of Two Random Choices」思想（Wave 7 遗珠补位）。

## Problem Statement

负载放置的病：单次随机哈希长尾热桶（尾延迟放大）与
全局最小扫描（每次放置 O(n)）——**O(1) 二样本地比较面**缺失。

## Solution

`PowerOfTwoChoices`（core/policy）：

- 每次放置随机抽两个不同桶（同桶重抽——无放回二择），
  取装载较小者；并列取下标小者（完全确定无随机残余）；
- 最大装载从单次随机 O(ln n/ln ln n) 压到 O(ln ln n)
  （非对称性引理）；
- 种子化 Random（同种子同放置序列——确定性可回放）+
  place/loadOf/maxLoad/binCount/placed 读数；桶数越域/
  下标越域 fail-fast。

## User Stories

1. 作为网关作者，O(1) 放置即可压平长尾热桶。
2. 作为审计作者，1M 球 1M 桶 maxLoad≤4 可复现钉住。

## Testing Decisions

- 2 桶差距始终 ≤1（严格放轻者）；1M 球 1M 桶 maxLoad≤4
  （单次随机典型 ≥6）；放置守恒；同种子序列全等；fail-fast。

## Out of Scope

- 不做摘除与再均衡（不可变负载增长面）；不做权重。

## Further Notes

- 勘误：并列判定初版 loads[i]<=loads[j] 取 min(i,j) 可
  越过严格更轻桶——拆三支修正；同桶重抽缺失（i==j 等效
  单次随机）一并钉住修正。
- 与 ConsistentHashRing（同包）同族不同面：键映射粘滞 vs
  负载感知放置。
- 里程碑：T47/50（94%）。
''')

# ============ tickets + impl ============
def ticket(tid, title, q, res, blocked=''):
    w('.wayfinder/tickets/%s.md' % tid, u'''---
id: %s
title: %s
type: task
status: closed
assignee: zcode-t
blocked-by: [%s]
created: 2026-09-28
---

## Question

%s

## Resolution

%s
''' % (tid.split('-')[0], title, blocked, q, res))

ticket('T6289-waitforgraph-shape', 'T 会话 T45 WaitForGraph 等待图死锁检测的形状裁决',
  '死锁怎么在加边瞬间发现并裁决受难者？（spec 6045 / effort #6045 / T45）',
  '''**WaitForGraph（core/concurrent）**：节点=事务、边
waiter→holder，环即死锁——加边即回报规范环（升序 DFS +
最小 id 旋转）与受难者（环内最大 id）；removeNode 打断；
端点未注册/自环/重复 fail-fast。''')

ticket('T6290-waitforgraph-verify', 'T 会话 T45 WaitForGraph 等待图死锁检测的验证裁决',
  'T45 合同怎么逐一验绿？（spec 6045 / effort #6045 / T45）',
  '''**验证通过**：WaitForGraphTest 五测全绿——无环链零
误报；二环/三环带入口规范环逐值钉住；受难者打断后复原；
fail-fast 四路。（勘误：出边计数漏减+规范环未旋转已修）''',
  'T6289')

ticket('T6291-vanemdeboas-shape', 'T 会话 T46 VanEmdeBoas 有界宇宙树的形状裁决',
  '小整数全域后继怎么 O(log log u)？（spec 6046 / effort #6046 / T46）',
  '''**VanEmdeBoas（core/concurrent）**：2^bits 半位分簇
递归+summary 摘要；min 镜像/max 冗余（CLRS）；簇惰性创建；
insert 幂等/delete 缺席 fail-fast/successor 缺席 -1。''')

ticket('T6292-vanemdeboas-verify', 'T 会话 T46 VanEmdeBoas 有界宇宙树的验证裁决',
  'T46 合同怎么逐一验绿？（spec 6046 / effort #6046 / T46）',
  '''**验证通过**：VanEmdeBoasTest 四测全绿——400 随机
操作 vs TreeSet 圣像五面逐步全等；2/4/1M 宇宙端到端；重复
幂等+稀疏；fail-fast。''',
  'T6291')

ticket('T6293-p2c-shape', 'T 会话 T47 PowerOfTwoChoices 二择一负载均衡的形状裁决',
  '长尾热桶怎么 O(1) 放置压平？（spec 6047 / effort #6047 / T47）',
  '''**PowerOfTwoChoices（core/policy，Wave 7 遗珠）**：
随机抽两个不同桶（同桶重抽）取较轻（并列取小下标确定）；
seeded Random 可回放；placed/loadOf/maxLoad 读数；越域
fail-fast。''')

ticket('T6294-p2c-verify', 'T 会话 T47 PowerOfTwoChoices 二择一负载均衡的验证裁决',
  'T47 合同怎么逐一验绿？（spec 6047 / effort #6047 / T47）',
  '''**验证通过**：PowerOfTwoChoicesTest 四测全绿——2 桶
差距 ≤1；1M 球 maxLoad≤4+守恒；同种子序列全等；fail-fast。
（勘误：并列取 min(i,j) 越过严格轻桶+同桶未重抽已修）''',
  'T6293')

w('.wayfinder/impl/2245-wait-for-graph.md', '''# impl 2245 — T 会话 T45 WaitForGraph 等待图死锁检测（spec 6045 / T6289–T6290 / T45）

纵切片：WaitForGraph（core/concurrent）——增量加边即时
环检测 + 规范环 + 受难者裁决。

- 验证：`mvn -pl buzhou-core test -Dtest='WaitForGraphTest'` 五测全绿。
''')

w('.wayfinder/impl/2246-van-emde-boas.md', '''# impl 2246 — T 会话 T46 VanEmdeBoas 有界宇宙树（spec 6046 / T6291–T6292 / T46）

纵切片：VanEmdeBoas（core/concurrent）——半位分簇递归 +
summary 摘要的 O(log log u) 有界宇宙集合。

- 验证：`mvn -pl buzhou-core test -Dtest='VanEmdeBoasTest'` 四测全绿。
''')

w('.wayfinder/impl/2247-power-of-two-choices.md', '''# impl 2247 — T 会话 T47 PowerOfTwoChoices 二择一负载均衡（spec 6047 / T6293–T6294 / T47）

纵切片：PowerOfTwoChoices（core/policy）——无放回二择
取轻放置的负载均衡（Wave 7 遗珠）。

- 验证：`mvn -pl buzhou-core test -Dtest='PowerOfTwoChoicesTest'` 四测全绿。
''')

# ============ map rows ============
p = '.wayfinder/maps/effort-6000.md'
s = io.open(p, encoding='utf-8', newline='').read()
anchor = u'| T44 | #6044 | Lottery Scheduler 彩票调度（Waldspurger 彩票思想；源码对账补账批预入档） | T6287–T6288 | 2244 | ✅ |'
i = s.index(anchor)
j = s.index('\n', i) + 1
rows = (u'| T45 | #6045 | WaitForGraph 等待图死锁检测（DB2/SQL Server 锁表面思想） | T6289–T6290 | 2245 | ✅ |\n'
        u'| T46 | #6046 | VanEmdeBoas 有界宇宙树（van Emde Boas 1975 思想） | T6291–T6292 | 2246 | ✅ |\n'
        u'| T47 | #6047 | PowerOfTwoChoices 二择一负载均衡（Mitzenmacher 思想；Wave 7 遗珠补位） | T6293–T6294 | 2247 | ✅ |\n')
s = s[:j] + rows + s[j:]
io.open(p, 'w', encoding='utf-8', newline='').write(s)

# ============ README rows ============
p = 'README.md'
s = io.open(p, encoding='utf-8', newline='').read()
anchor = u'（spec 6044） | [spec 6044](docs/spec/6044-lottery-scheduler.md) |'
i = s.index(anchor)
j = s.index('\n', i) + 1
rows = (
 u'| 过程治理 | Wait For Graph 等待图死锁检测 | WaitForGraph——增量加边即时环检测+受难者裁决的锁等待图（DB2/SQL Server 锁管理器思想）：节点=事务、有向边 waiter→holder，环即死锁——加边即时回报规范环（起点/邻接按 id 升序 DFS+环内最小 id 旋转到首位——同图同环完全确定）与受难者（环内最大 id=最年轻者先回滚约定）+removeNode 打断（出入边全清计数守恒）+nodeCount/edgeCount/hasDeadlock/deadlockVictim 读数+未注册端点/自环/重复节点/缺席摘除 fail-fast——事后全图扫描（死锁扩大化才被发现）与全局超时轮询（无差别惩罚无辜等待者）的病解（无环链零误报+二环/三环带入口规范环逐值钉住+打断复原钉住）；勘误：removeNode 初版漏减被摘节点出边计数+规范环未旋转双缺陷由合同钉住修正；与 TarjanSccFinder 同族不同面：离线全图强连通分量 vs 增量加边即时环检测（spec 6045） | [spec 6045](docs/spec/6045-wait-for-graph.md) |\n'
 u'| 结构查询 | Van Emde Boas 有界宇宙树 | VanEmdeBoas——半位分簇递归+summary 摘要的 O(log log u) 有界宇宙集合（van Emde Boas 1975 思想）：insert/delete/contains/successor/最值全走递归分簇（min 只在节点镜像、max 每层冗余——CLRS 约定），簇与 summary 惰性创建（稀疏集不预支全域内存）+successor 缺席 -1/空集最值 -1 诚实+集合语义重复幂等+缺席删除/越域/位宽越域 fail-fast——有序结构 O(log n) 后继（有界小整数全域下 log log u 渐近优势被放弃）的病解（400 随机操作 vs TreeSet 圣像五面逐步全等+2/4/1M 三档宇宙端到端+稀疏插入钉住）；与 IndexedHeap（6026）同族不同面：优先级队列 decrease-key vs 有界宇宙后继查询（spec 6046） | [spec 6046](docs/spec/6046-van-emde-boas.md) |\n'
 u'| 负载治理 | Power Of Two Choices 二择一负载均衡 | PowerOfTwoChoices——随机二样取轻放置的负载均衡（Mitzenmacher power-of-two-choices 思想；Wave 7 遗珠补位）：每次放置随机抽两个不同桶（同桶重抽——无放回二择）取装载较小者（并列取下标小者完全确定），最大装载从单次随机 O(ln n/ln ln n) 压到 O(ln ln n)（非对称性引理）+种子化 Random（同种子同放置序列——确定性可回放）+place/loadOf/maxLoad/binCount/placed 读数+桶数越域/下标越域 fail-fast——单次随机哈希长尾热桶（尾延迟放大）与全局最小扫描（每次放置 O(n)）两种病的同解（2 桶差距始终 ≤1 严格放轻+1M 球 1M 桶 maxLoad≤4 vs 单次随机典型 ≥6+守恒钉住）；勘误：并列判定初版可越过严格更轻桶+同桶未重抽一并钉住修正；与 ConsistentHashRing 同族不同面：键映射粘滞 vs 负载感知放置（spec 6047） | [spec 6047](docs/spec/6047-power-of-two-choices.md) |\n')
s = s[:j] + rows + s[j:]
io.open(p, 'w', encoding='utf-8', newline='').write(s)

# ============ api-surface rows ============
p = 'docs/api-surface.md'
s = io.open(p, encoding='utf-8', newline='').read()
anchor = u'- `public final class LotteryScheduler`（spec 6044——票数比例加权随机调度）'
i = s.index(anchor)
j = s.index('\n', i) + 1
rows = (u'- `public final class WaitForGraph`（spec 6045——增量加边死锁环检测）\n'
        u'- `public final class VanEmdeBoas`（spec 6046——O(log log u) 有界宇宙集合）\n')
s = s[:j] + rows + s[j:]
anchor2 = u'- `public final class TopKSampler`（spec'
i2 = s.index(anchor2)
j2 = s.index('\n', i2) + 1
row = u'- `public final class PowerOfTwoChoices`（spec 6047——二择一取轻负载放置）\n'
s = s[:j2] + row + s[j2:]
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('artifacts done')
