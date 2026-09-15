# impl 1426 — SessionHibernationPolicy 会话休眠分级（R26 = effort #1825 / spec 1825 / T2851-T2852）

**What**：`SessionHibernationPolicy`（core/session 静态纯函数）——band 三档
（ACTIVE/DROWSY/HIBERNATED 边界含上）+ profile 画像（唤醒税 0/50/2000ms +
足迹比 1.0/0.5/0.1 公开常量）+ census 普查（三档计数 + footprintReduction
-1 哨兵）；负闲置/倒挂/null fail-fast。

**Why**：k8s scale-to-zero/duty-cycling 思想——空闲会话全热常驻是浪费，
销毁又不可逆；预降级（轻税）与降冷（重税）分级让「省多少 vs 醒多慢」
可算，混合负载节省率直接读数。

**Verify**：`SessionHibernationPolicyTest` 4 用例全绿（节省率浮点直等假红
一次，容差断言修正——本系第二次浮点病理，模式已识别）。

**Status**：done（2026-09-16）
