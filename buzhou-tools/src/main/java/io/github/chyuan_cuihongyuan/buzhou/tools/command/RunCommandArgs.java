package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * run_command 入参解析（spec 17 / impl-60）：JSON 反序列化与默认值收口为不可变 record，
 * 供内置版与沙箱委托版共用同一入参契约。
 *
 * @param command       命令行（必填）
 * @param workdir       工作目录（空白 = 沙箱 root）
 * @param timeoutSeconds 超时秒数（缺省 = -1，由调用方按其默认值补齐）
 */
public record RunCommandArgs(String command, String workdir, long timeoutSeconds) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static RunCommandArgs parse(String toolInput) {
        try {
            JsonNode node = MAPPER.readTree(toolInput);
            return new RunCommandArgs(
                    node.path("command").asText(""),
                    node.path("workdir").asText(""),
                    node.path("timeoutSeconds").asLong(-1L));
        } catch (Exception e) {
            throw new IllegalArgumentException("入参不是合法 JSON：" + e.getMessage(), e);
        }
    }
}
