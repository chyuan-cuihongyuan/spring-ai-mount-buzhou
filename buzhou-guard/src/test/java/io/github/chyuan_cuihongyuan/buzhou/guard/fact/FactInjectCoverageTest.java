package io.github.chyuan_cuihongyuan.buzhou.guard.fact;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FactInjectCoverageTest {

    /** 三事实桩仓（内容定长便于触发 maxChars 省略）。 */
    private static FactStore threeFacts() {
        return new FactStore() {
            @Override
            public void save(String sessionId, Fact fact) {
            }

            @Override
            public List<Fact> activeFacts(String sessionId, int currentTurn) {
                return List.of(
                        new Fact("fact.a", "v-aaaaaaaaaaaaaaaaaa", "prod-a", 1, 5),
                        new Fact("fact.b", "v-bbbbbbbbbbbbbbbbbb", "prod-a", 1, 5),
                        new Fact("fact.c", "v-cccccccccccccccccc", "prod-b", 1, 5));
            }

            @Override
            public void delete(String sessionId, String key) {
            }
        };
    }

    @Test
    void unboundedRenderInjectsAllAndCounts() {
        FactAttachmentRenderer renderer = new FactAttachmentRenderer(threeFacts(), List.of());

        Optional<String> out = renderer.render("s1", 1);

        assertThat(out).isPresent();
        assertThat(renderer.stats()).isEqualTo(new FactAttachmentRenderer.FactInjectStats(1, 3, 0));
    }

    @Test
    void maxCharsOmitsFactsAndCountsThem() {
        FactAttachmentRenderer renderer = new FactAttachmentRenderer(threeFacts(), List.of());

        Optional<String> out = renderer.render("s1", 1, 30);

        assertThat(out).isPresent();
        assertThat(out.get()).contains("更多事实未注入");
        assertThat(renderer.stats().factsOmitted()).isEqualTo(3);
        assertThat(renderer.stats().factsInjected()).isZero();
    }

    @Test
    void emptyStoreRendersNothingAndNotCounted() {
        FactAttachmentRenderer renderer = new FactAttachmentRenderer(new FactStore() {
            @Override
            public void save(String sessionId, Fact fact) {
            }

            @Override
            public List<Fact> activeFacts(String sessionId, int currentTurn) {
                return List.of();
            }

            @Override
            public void delete(String sessionId, String key) {
            }
        }, List.of());

        assertThat(renderer.render("nobody", 1)).isEmpty();
        assertThat(renderer.stats().renders()).isZero();
    }
}
