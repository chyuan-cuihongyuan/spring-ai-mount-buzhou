# impl 2191 — S 会话 S41 Blocked Sliding Counter 分块滑窗计数器（spec 5040 / T6181–T6182 / S41）

纵切片：BlockedSlidingCounter（core/metrics）——分块进出 +
双界估计（真值恒落界）+ O(m) 内存读数。（勘误：原拟 DG
指数直方图误差界不可自洽，换分块面。）

- 验证：`mvn -pl buzhou-core test -Dtest='BlockedSlidingCounterTest'` 全绿（MVN_EXIT=0）。
