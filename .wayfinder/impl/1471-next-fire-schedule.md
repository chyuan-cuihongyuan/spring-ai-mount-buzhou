# impl 1471 — NextFireSchedule 固定间隔下次触发（R71 = effort #1870 / spec 1870 / T2941-T2942）

**What**：`NextFireSchedule`（core/exec 静态纯函数）——nextFireMillis
（≥now 最近网格点含上）+ missedFires（(lastAcked,now] 格点数）；间隔<1/
负时点/确认越界 fail-fast。

**Why**：crontab/systemd timer 网格语义——now+interval 手写让停机重启
后网格漂移；epoch 钉网格+补账数让「本该 10:00 的任务变 10:07」和
「停机漏三拍补不补」都有账。

**Verify**：`NextFireScheduleTest` 3 用例全绿（首跑编译红为局部方法
误用——Java 不支持，提私有静态修正）。

**Status**：done（2026-09-16）
