package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4034 / T6070：JSON Patch 合同——六操作正反例、指针转义、
 * move 下标记账、test 前置、原子性、畸形 fail-fast。
 */
class JsonPatchApplierTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String raw) {
        try {
            return MAPPER.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("测试夹具非法：" + raw, e);
        }
    }

    @Test
    void addShouldInsertShiftAndAppend() {
        JsonNode doc = json("{\"a\":{\"b\":[1,2]}}");
        JsonNode patched = JsonPatchApplier.apply(doc, json("["
                + "{\"op\":\"add\",\"path\":\"/c\",\"value\":3},"
                + "{\"op\":\"add\",\"path\":\"/a/b/0\",\"value\":0},"
                + "{\"op\":\"add\",\"path\":\"/a/b/-\",\"value\":9}"
                + "]"));
        assertThat(patched.get("c").asInt()).isEqualTo(3);
        assertThat(patched.at("/a/b").toString()).isEqualTo("[0,1,2,9]");
    }

    @Test
    void removeAndReplaceShouldEditInPlace() {
        JsonNode doc = json("{\"a\":[1,2,3],\"keep\":true}");
        JsonNode patched = JsonPatchApplier.apply(doc, json("["
                + "{\"op\":\"remove\",\"path\":\"/a/1\"},"
                + "{\"op\":\"replace\",\"path\":\"/a/1\",\"value\":99},"
                + "{\"op\":\"replace\",\"path\":\"/keep\",\"value\":false}"
                + "]"));
        assertThat(patched.at("/a").toString()).isEqualTo("[1,99]");
        assertThat(patched.get("keep").isBoolean()).isTrue();
        assertThat(patched.get("keep").asBoolean()).isFalse();
    }

    @Test
    void replaceMissingShouldReject() {
        JsonNode doc = json("{\"a\":1}");
        assertThatThrownBy(() -> JsonPatchApplier.apply(doc,
                json("[{\"op\":\"replace\",\"path\":\"/ghost\",\"value\":2}]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("replace");
    }

    @Test
    void moveShouldAccountForArrayShift() {
        JsonNode doc = json("{\"list\":[\"a\",\"b\",\"c\"],\"hold\":{}}");
        JsonNode patched = JsonPatchApplier.apply(doc, json(
                "[{\"op\":\"move\",\"from\":\"/list/0\",\"path\":\"/list/2\"}]"));
        assertThat(patched.at("/list").toString()).isEqualTo("[\"b\",\"c\",\"a\"]");   // 先删后插
    }

    @Test
    void copyShouldDuplicateValue() {
        JsonNode doc = json("{\"src\":{\"x\":1},\"dst\":{}}");
        JsonNode patched = JsonPatchApplier.apply(doc, json(
                "[{\"op\":\"copy\",\"from\":\"/src\",\"path\":\"/dst/mirror\"}]"));
        assertThat(patched.at("/dst/mirror/x").asInt()).isEqualTo(1);
        assertThat(patched.at("/src/x").asInt()).isEqualTo(1);
    }

    @Test
    void testFailureShouldRejectWholePatch() {
        JsonNode doc = json("{\"a\":1,\"b\":2}");
        JsonNode patch = json("["
                + "{\"op\":\"test\",\"path\":\"/a\",\"value\":1},"
                + "{\"op\":\"test\",\"path\":\"/b\",\"value\":999},"
                + "{\"op\":\"add\",\"path\":\"/c\",\"value\":3}"
                + "]");
        assertThatThrownBy(() -> JsonPatchApplier.apply(doc, patch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test");
        assertThat(doc.has("c")).isFalse();   // 原子性：原文档不变
    }

    @Test
    void pointerEscapesShouldRoundTrip() {
        JsonNode doc = json("{\"a/b\":1,\"m~n\":2}");
        JsonNode patched = JsonPatchApplier.apply(doc, json("["
                + "{\"op\":\"replace\",\"path\":\"/a~1b\",\"value\":10},"
                + "{\"op\":\"replace\",\"path\":\"/m~0n\",\"value\":20}"
                + "]"));
        assertThat(patched.get("a/b").asInt()).isEqualTo(10);
        assertThat(patched.get("m~n").asInt()).isEqualTo(20);
    }

    @Test
    void malformedPointersAndOpsShouldFailFast() {
        JsonNode doc = json("{\"a\":[1]}");
        assertThatThrownBy(() -> JsonPatchApplier.apply(doc,
                json("[{\"op\":\"remove\",\"path\":\"a\"}]")))   // 缺 / 起
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonPatchApplier.apply(doc,
                json("[{\"op\":\"remove\",\"path\":\"/a/-\"}]")))   // remove 禁 "-"
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonPatchApplier.apply(doc,
                json("[{\"op\":\"add\",\"path\":\"/a/01\",\"value\":2}]")))   // 前导零下标
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonPatchApplier.apply(doc,
                json("[{\"op\":\"trick\",\"path\":\"/x\",\"value\":2}]")))   // 未知 op
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonPatchApplier.apply(doc,
                json("[{\"op\":\"add\",\"path\":\"/x\"}]")))   // 缺 value
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonPatchApplier.apply(doc, json("{}")))   // patch 非数组
                .isInstanceOf(IllegalArgumentException.class);
    }
}
