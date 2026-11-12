package com.pankaj.homeorganizer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Everything the user has organised, saved on the phone. */
public class Prefs {
    public static final int SORT_AZ = 0;
    public static final int SORT_MOST_USED = 1;
    public static final int SORT_RECENT = 2;

    private static final String FAVORITES = "favorites";
    private static final String HIDDEN = "hidden";
    private static final String CUSTOM_CATS = "custom_categories";
    private static final String CAT_PREFIX = "cat:";
    private static final String COUNT_PREFIX = "count:";

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences("organizer", Context.MODE_PRIVATE);
    }

    private Set<String> getSet(String name) {
        return new HashSet<>(sp.getStringSet(name, Collections.<String>emptySet()));
    }

    private void toggleIn(String name, String key) {
        Set<String> s = getSet(name);
        if (!s.remove(key)) s.add(key);
        sp.edit().putStringSet(name, s).apply();
    }

    public Set<String> favorites() { return getSet(FAVORITES); }
    public Set<String> hidden() { return getSet(HIDDEN); }
    public void toggleFavorite(String key) { toggleIn(FAVORITES, key); }
    public void toggleHidden(String key) { toggleIn(HIDDEN, key); }

    public String categoryOverride(String key) { return sp.getString(CAT_PREFIX + key, null); }

    public void setCategoryOverride(String key, String category) {
        if (category == null) sp.edit().remove(CAT_PREFIX + key).apply();
        else sp.edit().putString(CAT_PREFIX + key, category).apply();
    }

    public List<String> customCategories() {
        List<String> l = new ArrayList<>(getSet(CUSTOM_CATS));
        Collections.sort(l, String.CASE_INSENSITIVE_ORDER);
        return l;
    }

    public void addCustomCategory(String name) {
        Set<String> s = getSet(CUSTOM_CATS);
        s.add(name);
        sp.edit().putStringSet(CUSTOM_CATS, s).apply();
    }

    public int launches(String key) { return sp.getInt(COUNT_PREFIX + key, 0); }
    public void incLaunch(String key) { sp.edit().putInt(COUNT_PREFIX + key, launches(key) + 1).apply(); }

    public int sortMode() { return sp.getInt("sort", SORT_AZ); }
    public void setSortMode(int m) { sp.edit().putInt("sort", m).apply(); }

    public boolean grouped() { return sp.getBoolean("grouped", true); }
    public void setGrouped(boolean g) { sp.edit().putBoolean("grouped", g).apply(); }

    public int columns() { return sp.getInt("columns", 4); }
    public void setColumns(int c) { sp.edit().putInt("columns", c).apply(); }

    /** Clears favourites, hidden apps and custom categories (keeps usage counts and view settings). */
    public void resetOrganization() {
        SharedPreferences.Editor e = sp.edit();
        for (String k : sp.getAll().keySet()) {
            if (k.startsWith(CAT_PREFIX)) e.remove(k);
        }
        e.remove(FAVORITES).remove(HIDDEN).remove(CUSTOM_CATS).apply();
    }
}
