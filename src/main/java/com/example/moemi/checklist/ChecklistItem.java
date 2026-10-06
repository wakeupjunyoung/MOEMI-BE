package com.example.moemi.checklist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checklist_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistItem {

    public static final int NAME_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false)
    private boolean checked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemSource source;

    /** Why the app recommended this item, e.g. "자외선 높음"; {@code null} for the user's own items. */
    @Column(length = 50)
    private String reason;

    private ChecklistItem(Checklist checklist, String name, ItemSource source, String reason) {
        this.checklist = checklist;
        this.name = name;
        this.source = source;
        this.reason = reason;
    }

    static ChecklistItem recommended(Checklist checklist, Recommendation recommendation) {
        return new ChecklistItem(checklist, recommendation.name(), ItemSource.RECOMMENDED, recommendation.reason());
    }

    static ChecklistItem custom(Checklist checklist, String name) {
        return new ChecklistItem(checklist, name, ItemSource.CUSTOM, null);
    }

    public boolean isRecommended() {
        return source == ItemSource.RECOMMENDED;
    }

    public void check(boolean checked) {
        this.checked = checked;
    }

    /** A renamed item is the user's own from then on. */
    void rename(String name) {
        this.name = name;
        this.source = ItemSource.CUSTOM;
        this.reason = null;
    }

    void updateReason(String reason) {
        this.reason = reason;
    }
}
