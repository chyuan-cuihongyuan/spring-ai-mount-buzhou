# impl-108 — 压缩梯子事件化

**What to build:** 梯子每级折入都发 memory.compacted（payload 带 evictRatio）。

**Blocked by:** None

**Status:** done

- [x] CompactionListener 函数式接口（sessionId+result+evictRatio）
- [x] ivp 主路径与梯子每级都 notify；MemoryModule/测试随迁（payload 增 evictRatio）
- [x] memory 90/90 绿；spec 38 §A

## Done

commit：见 git log（impl-108）。
