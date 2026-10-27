package com.example.moemi.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.moemi.lifeindex.Region;

class ChecklistTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 8, 0);
    private static final Recommendation SUNSCREEN = new Recommendation("선크림", "자외선 높음");
    private static final Recommendation HAT = new Recommendation("모자", "자외선 높음");
    private static final Recommendation MASK = new Recommendation("마스크", "대기정체 높음");

    @Test
    void createAddsRecommendedItems() {
        Checklist checklist = create(RecommendationResult.ok(List.of(SUNSCREEN, HAT)));

        assertThat(checklist.getRecommendationStatus()).isEqualTo(RecommendationStatus.OK);
        assertThat(checklist.getRecommendedAt()).isEqualTo(NOW);
        assertThat(checklist.getItems())
                .extracting(ChecklistItem::getName, ChecklistItem::getSource, ChecklistItem::getReason)
                .containsExactly(
                        tuple("선크림", ItemSource.RECOMMENDED, "자외선 높음"),
                        tuple("모자", ItemSource.RECOMMENDED, "자외선 높음"));
    }

    @Test
    void refreshReplacesRecommendationsButKeepsUserItems() {
        Checklist checklist = create(RecommendationResult.ok(List.of(SUNSCREEN, HAT)));
        checklist.addCustomItem("  교통카드 ");

        checklist.applyRecommendations(RecommendationResult.ok(List.of(MASK)), NOW.plusHours(1));

        assertThat(checklist.getItems()).extracting(ChecklistItem::getName).containsExactly("교통카드", "마스크");
        assertThat(checklist.getRecommendedAt()).isEqualTo(NOW.plusHours(1));
    }

    @Test
    void deletedRecommendationIsNotAddedAgain() {
        Checklist checklist = create(RecommendationResult.ok(List.of(SUNSCREEN, HAT)));
        checklist.removeItem(itemNamed(checklist, "모자"));

        checklist.applyRecommendations(RecommendationResult.ok(List.of(SUNSCREEN, HAT)), NOW);

        assertThat(checklist.getItems()).extracting(ChecklistItem::getName).containsExactly("선크림");
    }

    @Test
    void renamedRecommendationBecomesUserItemAndSurvivesRefresh() {
        Checklist checklist = create(RecommendationResult.ok(List.of(SUNSCREEN, HAT)));
        checklist.renameItem(itemNamed(checklist, "모자"), "양산");

        checklist.applyRecommendations(RecommendationResult.ok(List.of(MASK)), NOW);

        assertThat(checklist.getItems())
                .extracting(ChecklistItem::getName, ChecklistItem::getSource)
                .containsExactly(tuple("양산", ItemSource.CUSTOM), tuple("마스크", ItemSource.RECOMMENDED));

        checklist.applyRecommendations(RecommendationResult.ok(List.of(HAT)), NOW);
        assertThat(checklist.getItems()).extracting(ChecklistItem::getName).containsExactly("양산");
    }

    @Test
    void userItemWithSameNameIsNotDuplicated() {
        Checklist checklist = create(RecommendationResult.ok(List.of()));
        checklist.addCustomItem("마스크");

        checklist.applyRecommendations(RecommendationResult.ok(List.of(MASK)), NOW);

        assertThat(checklist.getItems()).singleElement().extracting(ChecklistItem::getSource).isEqualTo(ItemSource.CUSTOM);
    }

    @Test
    void failedLookupLeavesItemsAlone() {
        Checklist checklist = create(RecommendationResult.ok(List.of(SUNSCREEN)));
        itemNamed(checklist, "선크림").check(true);

        checklist.applyRecommendations(RecommendationResult.failed(), NOW.plusHours(1));

        assertThat(checklist.getRecommendationStatus()).isEqualTo(RecommendationStatus.FAILED);
        assertThat(checklist.getRecommendedAt()).isEqualTo(NOW);
        assertThat(checklist.getItems()).singleElement().extracting(ChecklistItem::isChecked).isEqualTo(true);
        assertThat(checklist.checkedCount()).isEqualTo(1);
    }

    private static Checklist create(RecommendationResult recommendations) {
        return Checklist.create("device-1", LocalDate.of(2026, 10, 6), null, Region.GWANGJU_GWANGSAN, recommendations, NOW);
    }

    private static ChecklistItem itemNamed(Checklist checklist, String name) {
        return checklist.getItems().stream().filter(item -> item.getName().equals(name)).findFirst().orElseThrow();
    }
}
