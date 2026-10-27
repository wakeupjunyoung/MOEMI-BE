package com.example.moemi.checklist;

public enum ItemSource {
    /** Added by the app from life indices. Replaced when recommendations are refreshed. */
    RECOMMENDED,
    /** Added or renamed by the user. Never removed by a refresh. */
    CUSTOM
}
