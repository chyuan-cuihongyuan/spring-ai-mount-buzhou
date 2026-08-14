package io.github.chyuan_cuihongyuan.buzhou.skill.store;

/** impl-51：内存实现过同一契约（既有实现回归护栏）。 */
class InMemorySkillStoreContractTest extends AbstractSkillStoreContractTest {
    @Override
    protected SkillStore store() {
        return new InMemorySkillStore();
    }
}
