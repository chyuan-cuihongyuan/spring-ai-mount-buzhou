package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * http_request 输入边界四护栏测试（spec 1629 / T2409–T2410 / impl 1182）：
 * body/URL/头数量/单头值超限各自拒绝入桶（Envoy HTTP/2 SETTINGS_MAX_* 思想），
 * 合规输入零影响。
 */
class HttpRequestInputBoundsTest {

    @AfterEach
    void reset() {
        HttpRequestTool.resetForTest();
    }

    @Test
    void oversizeBodyRejectedWithBodyPathHint() {
        HttpRequestTool tool = new HttpRequestTool(
                new SsrfGuard(true, List.of()), Duration.ofSeconds(2));
        String big = "x".repeat(HttpRequestTool.MAX_BODY_CHARS + 1);
        String out = tool.call("{\"method\":\"GET\",\"url\":\"https://example.com/a\",\"body\":\""
                + big + "\"}");
        assertThat(out).contains("body 长度").contains("bodyPath");
        assertThat(HttpRequestTool.stats().oversizeRejects()).isEqualTo(1);
    }

    @Test
    void oversizeUrlRejected() {
        HttpRequestTool tool = new HttpRequestTool(
                new SsrfGuard(true, List.of()), Duration.ofSeconds(2));
        String longUrl = "https://example.com/" + "a".repeat(HttpRequestTool.MAX_URL_CHARS);
        String out = tool.call("{\"method\":\"GET\",\"url\":\"" + longUrl + "\"}");
        assertThat(out).contains("url 长度").contains("超上限");
        assertThat(HttpRequestTool.stats().urlRejects()).isEqualTo(1);
    }

    @Test
    void tooManyHeadersRejected() {
        HttpRequestTool tool = new HttpRequestTool(
                new SsrfGuard(true, List.of()), Duration.ofSeconds(2));
        StringBuilder headers = new StringBuilder("{");
        for (int i = 0; i < HttpRequestTool.MAX_HEADERS + 1; i++) {
            if (i > 0) {
                headers.append(',');
            }
            headers.append("\"h").append(i).append("\":\"v\"");
        }
        headers.append('}');
        String out = tool.call("{\"method\":\"GET\",\"url\":\"https://example.com/a\",\"headers\":"
                + headers + "}");
        assertThat(out).contains("请求头数量").contains("超上限");
        assertThat(HttpRequestTool.stats().oversizeRejects()).isEqualTo(1);
    }

    @Test
    void oversizeHeaderValueRejectedNotCountedAsFailure() {
        HttpRequestTool tool = new HttpRequestTool(
                new SsrfGuard(true, List.of()), Duration.ofSeconds(2));
        String bigValue = "v".repeat(HttpRequestTool.MAX_HEADER_VALUE_CHARS + 1);
        String out = tool.call("{\"method\":\"GET\",\"url\":\"https://example.com/a\","
                + "\"headers\":{\"x-big\":\"" + bigValue + "\"}}");
        assertThat(out).contains("单头值长度").contains("超上限");
        assertThat(HttpRequestTool.stats().oversizeRejects()).isEqualTo(1);
        assertThat(HttpRequestTool.stats().failures()).isZero(); // 输入护栏不计失败
    }

    @Test
    void compliantInputUnaffected() {
        HttpRequestTool tool = new HttpRequestTool(
                new SsrfGuard(true, List.of()), Duration.ofSeconds(2));
        String out = tool.call("{\"method\":\"GET\",\"url\":\"https://example.com/a\","
                + "\"headers\":{\"x-ok\":\"fine\"}}");
        assertThat(out).doesNotContain("超上限").doesNotContain("失败：url");
        assertThat(HttpRequestTool.stats().totalRejects())
                .isLessThanOrEqualTo(HttpRequestTool.stats().failures());
    }
}
