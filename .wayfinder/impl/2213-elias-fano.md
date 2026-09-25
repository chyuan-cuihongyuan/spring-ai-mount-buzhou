# impl 2213 — T 会话 T13 Elias-Fano 单调序列编码（spec 6013 / T6225–T6226 / T13）

纵切片：EliasFano（core/message）——高低位联合位图编码 +
O(1) select 访问 + 八组密度 oracle（初版整数除法致宽度
判错，改 ceilDiv+严格 ceil-log2）。

- 验证：`mvn -pl buzhou-core test -Dtest='EliasFanoTest'` 全绿（MVN_EXIT=0）。
