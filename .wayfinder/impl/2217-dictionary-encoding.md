# impl 2217 — T 会话 T17 Dictionary Encoding 字典编码（spec 6016 / T6233–T6234 / T17）

纵切片：DictionaryEncoding（core/message）——首次出现序
字典+BitPacking 窄下标列组合 + 压缩对账。

- 验证：`mvn -pl buzhou-core test -Dtest='DictionaryEncodingTest'` 全绿（MVN_EXIT=0）。
