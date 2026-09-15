package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Jcs（RFC 8785 自实现子集）规范化全分支补测（K 会话 R30 / spec 1229 / T1873——
 * 此前零直接测试）。纯函数：签名输入的规范化即合同——排序、转义、整数约束、
 * 非法输入 fail-fast。审计签名的输入规范化回归 = 签名验证全线失效。
 */
class JcsCanonicalizationTest {

    @Test
    void scalarsCanonicalForms() {
        assertThat(Jcs.canonicalize(null)).isEqualTo("null");
        assertThat(Jcs.canonicalize("文本")).isEqualTo("\"文本\"");
        assertThat(Jcs.canonicalize(Boolean.TRUE)).isEqualTo("true");
        assertThat(Jcs.canonicalize(Boolean.FALSE)).isEqualTo("false");
        assertThat(Jcs.canonicalize(7)).isEqualTo("7");
        assertThat(Jcs.canonicalize(7L)).isEqualTo("7");
        assertThat(Jcs.canonicalize((short) 7)).isEqualTo("7");
        assertThat(Jcs.canonicalize(BigInteger.valueOf(7))).isEqualTo("7");
    }

    @Test
    void nonIntegerNumbersAreRejected() {
        assertThatThrownBy(() -> Jcs.canonicalize(1.5d))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("仅接受整数");
        assertThatThrownBy(() -> Jcs.canonicalize(1.5f))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Jcs.canonicalize(new java.math.BigDecimal("1.5")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unsupportedValueTypeIsRejected() {
        assertThatThrownBy(() -> Jcs.canonicalize(new Object()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("不支持的值类型");
    }

    @Test
    void objectKeysAreSortedByCodeUnitOrder() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("z", 1);
        nested.put("a", 2);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("zebra", nested);
        input.put("apple", "第一");
        input.put("middle", List.of(1, "两", true));

        // 插入序 z→a→middle，输出按键字典序 a→middle→zebra；嵌套对象同样排序
        assertThat(Jcs.canonicalize(input)).isEqualTo(
                "{\"apple\":\"第一\",\"middle\":[1,\"两\",true],\"zebra\":{\"a\":2,\"z\":1}}");
    }

    @Test
    void stringEscapingCoversJsonMandatoryAndControlChars() {
        StringBuilder s = new StringBuilder();
        s.append('"').append('\\').append('\b').append('\f')
                .append('\n').append('\r').append('\t').append('\u0001');

        assertThat(Jcs.canonicalize(s.toString())).isEqualTo(
                '"' + "\\\"\\\\\\b\\f\\n\\r\\t\\u0001" + '"');
    }

    @Test
    void emptyContainersAndNullsInCollections() {
        assertThat(Jcs.canonicalize(Map.of())).isEqualTo("{}");
        assertThat(Jcs.canonicalize(List.of())).isEqualTo("[]");
        List<Object> withNull = new ArrayList<>();
        withNull.add(null);
        withNull.add("x");
        assertThat(Jcs.canonicalize(withNull)).isEqualTo("[null,\"x\"]");
        Map<String, Object> withNullValue = new LinkedHashMap<>();
        withNullValue.put("k", null);
        assertThat(Jcs.canonicalize(withNullValue)).isEqualTo("{\"k\":null}");
    }

    @Test
    void canonicalizeJsonReordersAndNormalizes() {
        // 输入乱序 + 冗余空白 → 输出排序紧凑
        assertThat(Jcs.canonicalizeJson("{ \"b\" : 2 , \"a\" : \"x\" }"))
                .isEqualTo("{\"a\":\"x\",\"b\":2}");
        assertThat(Jcs.canonicalizeJson("[{\"y\":1,\"x\":2},{\"b\":true,\"a\":null}]"))
                .isEqualTo("[{\"x\":2,\"y\":1},{\"a\":null,\"b\":true}]");
    }

    @Test
    void canonicalizeJsonArrayAndLiterals() {
        assertThat(Jcs.canonicalizeJson("{\"arr\":[null,true,false,3]}"))
                .isEqualTo("{\"arr\":[null,true,false,3]}");
    }

    @Test
    void invalidJsonIsRejectedAsIllegalArgument() {
        assertThatThrownBy(() -> Jcs.canonicalizeJson("{不是JSON}"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("不是合法 JSON");
    }

    @Test
    void nonIntegerNumberNodeIsRejected() {
        // canonicalizeJson 统一包装 writeNode 抛出的整数约束异常（原始消息进 cause）
        assertThatThrownBy(() -> Jcs.canonicalizeJson("{\"price\":1.5}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不是合法 JSON");
    }
}
