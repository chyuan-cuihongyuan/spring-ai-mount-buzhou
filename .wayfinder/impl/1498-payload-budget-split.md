# impl 1498 — PayloadBudgetSplit 多类型载荷预算分账（R98 = effort #1897 / spec 1897 / T2995-T2996）

**What**：`PayloadBudgetSplit`（core/prompt 静态纯函数 + 嵌套
Allocation）——allocate 加权水填（公平份额迭代、小需求完整退出、
余量回流、零头诚实披露）；负预算/不等长/零权重/负需求 fail-fast。

**Why**：多模态消息共享载荷预算——类型上限硬切浪费、按需全给爆
总预算；水填让小需求完整、大需求按权均摊。与批级回喂预算（截断
策略）互补：那是截断这是分配。落轮 grep 复核抑制规则（330）占坑
换静脉。

**Verify**：`PayloadBudgetSplitTest` 4 用例全绿（经典 {30,35,35}/
宽松全额/权重 3:1→{30,10}/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
