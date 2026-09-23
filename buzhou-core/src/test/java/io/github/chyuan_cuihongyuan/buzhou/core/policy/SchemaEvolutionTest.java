package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.SchemaEvolution.Field;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.SchemaEvolution.FieldType;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.SchemaEvolution.Plan;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.SchemaEvolution.Schema;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4045 / T6092：Avro 模式解析合同——同型直读、加宽链、
 * 不兼容 fail-fast、默认补位、alias 配对、dropped 显形、确定性。
 */
class SchemaEvolutionTest {

    private static Schema schema(String name, Field... fields) {
        return new Schema(name, List.of(fields));
    }

    @Test
    void identicalTypesShouldMapDirectly() {
        Plan plan = SchemaEvolution.resolve(
                schema("w", Field.of("a", FieldType.STRING, false)),
                schema("r", Field.of("a", FieldType.STRING, false)));
        assertThat(plan.fields()).hasSize(1);
        assertThat(plan.fields().get(0).writerField()).isEqualTo("a");
        assertThat(plan.fields().get(0).promoted()).isFalse();
        assertThat(plan.dropped()).isEmpty();
    }

    @Test
    void wideningChainsShouldBeAccepted() {
        Plan intToLong = SchemaEvolution.resolve(
                schema("w", Field.of("n", FieldType.INT, false)),
                schema("r", Field.of("n", FieldType.LONG, false)));
        assertThat(intToLong.fields().get(0).promoted()).isTrue();
        Plan longToDouble = SchemaEvolution.resolve(
                schema("w", Field.of("n", FieldType.LONG, false)),
                schema("r", Field.of("n", FieldType.DOUBLE, false)));
        assertThat(longToDouble.fields().get(0).promoted()).isTrue();
        Plan floatToDouble = SchemaEvolution.resolve(
                schema("w", Field.of("n", FieldType.FLOAT, false)),
                schema("r", Field.of("n", FieldType.DOUBLE, false)));
        assertThat(floatToDouble.fields().get(0).promoted()).isTrue();
    }

    @Test
    void incompatibleTypesShouldFailFastWithFieldName() {
        assertThatThrownBy(() -> SchemaEvolution.resolve(
                schema("w", Field.of("label", FieldType.STRING, false)),
                schema("r", Field.of("label", FieldType.INT, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label");
        assertThatThrownBy(() -> SchemaEvolution.resolve(
                schema("w", Field.of("n", FieldType.DOUBLE, false)),
                schema("r", Field.of("n", FieldType.INT, false))))
                .isInstanceOf(IllegalArgumentException.class);   // 收窄非法
    }

    @Test
    void missingWriterFieldShouldRequireReaderDefault() {
        Plan withDefault = SchemaEvolution.resolve(
                schema("w", Field.of("a", FieldType.STRING, false)),
                schema("r", Field.of("a", FieldType.STRING, false),
                        Field.of("extra", FieldType.INT, true)));
        assertThat(withDefault.fields().get(1).usesDefault()).isTrue();
        assertThatThrownBy(() -> SchemaEvolution.resolve(
                schema("w", Field.of("a", FieldType.STRING, false)),
                schema("r", Field.of("a", FieldType.STRING, false),
                        Field.of("mustHave", FieldType.INT, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mustHave");
    }

    @Test
    void aliasShouldMatchWriterField() {
        Plan plan = SchemaEvolution.resolve(
                schema("w", Field.of("oldName", FieldType.STRING, false)),
                schema("r", new Field("newName", FieldType.STRING, List.of("oldName"), false)));
        assertThat(plan.fields().get(0).writerField()).isEqualTo("oldName");
        assertThat(plan.fields().get(0).readerField()).isEqualTo("newName");
    }

    @Test
    void extraWriterFieldsShouldBeHonestlyDropped() {
        Plan plan = SchemaEvolution.resolve(
                schema("w", Field.of("kept", FieldType.STRING, false),
                        Field.of("discarded", FieldType.INT, false)),
                schema("r", Field.of("kept", FieldType.STRING, false)));
        assertThat(plan.fields()).hasSize(1);
        assertThat(plan.dropped()).containsExactly("discarded");
    }

    @Test
    void sameInputShouldReplaySamePlan() {
        Schema writer = schema("w", Field.of("a", FieldType.INT, false), Field.of("gone", FieldType.STRING, false));
        Schema reader = schema("r", Field.of("a", FieldType.LONG, false), Field.of("b", FieldType.INT, true));
        Plan first = SchemaEvolution.resolve(writer, reader);
        Plan second = SchemaEvolution.resolve(writer, reader);
        assertThat(first).isEqualTo(second);
    }
}
