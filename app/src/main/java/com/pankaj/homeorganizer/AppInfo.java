package com.pankaj.homeorganizer;

import android.graphics.drawable.Drawable;

/** One launchable app on the phone. */
public class AppInfo {
    public final String packageName;
    public final String activityName;
    public final String label;
    public final Drawable icon;
    public final long installTime;
    /** Category picked automatically. */
    public final String autoCategory;
    /** Category actually shown (auto, or the user's choice). */
    public String category;

    public AppInfo(String packageName, String activityName, String label, Drawable icon,
                   long installTime, String autoCategory) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.label = label;
        this.icon = icon;
        this.installTime = installTime;
        this.autoCategory = autoCategory;
        this.category = autoCategory;
    }

    /** Stable id used to store favourites, hidden state and category overrides. */
    public String key() {
        return packageName + "/" + activityName;
    }
}
