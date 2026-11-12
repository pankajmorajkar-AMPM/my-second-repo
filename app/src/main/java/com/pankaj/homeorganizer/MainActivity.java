package com.pankaj.homeorganizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    // Colours
    private static final int BG = 0xFF0F1216;
    private static final int CARD = 0xFF1C222B;
    private static final int ACCENT = 0xFF4F8CFF;
    private static final int TEXT = 0xFFECEFF4;
    private static final int SUB = 0xFF8A93A3;

    // Special filters (not real categories)
    private static final String ALL = "All";
    private static final String FAV = "★ Favorites";
    private static final String HIDDEN = "Hidden";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private Prefs prefs;
    private List<AppInfo> apps = new ArrayList<>();
    private String selected = ALL;
    private String query = "";
    private AppInfo firstVisible;
    private boolean loadedOnce = false;

    private LinearLayout chipRow;
    private LinearLayout content;
    private ScrollView scroll;
    private HorizontalScrollView chipScroll;
    private EditText search;
    private TextView status;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reload();
        }
    };

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        setupEdgeToEdge();
        setContentView(buildUi());

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageReceiver, f, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(packageReceiver, f);
        }
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Usage counts change after launching apps, so refresh the order.
        if (loadedOnce && prefs.sortMode() == Prefs.SORT_MOST_USED) render();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Pressing Home while already here: go back to the top.
        resetView();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception ignored) {
        }
        worker.shutdown();
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (!query.isEmpty()) {
            search.setText("");
        } else if (!ALL.equals(selected)) {
            selected = ALL;
            render();
        } else if (!isDefaultHome()) {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------------ UI building

    @SuppressWarnings("deprecation")
    private void setupEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @SuppressWarnings("deprecation")
    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                top = i.top;
                bottom = i.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });

        // Header: title + status + menu button
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(14), dp(8), dp(4));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("My Apps");
        title.setTextColor(TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        status = new TextView(this);
        status.setTextColor(SUB);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        status.setText("Loading apps…");
        titles.addView(title);
        titles.addView(status);
        header.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView menuBtn = new TextView(this);
        menuBtn.setText("⋮");
        menuBtn.setTextColor(TEXT);
        menuBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        menuBtn.setGravity(Gravity.CENTER);
        menuBtn.setContentDescription("Menu");
        setRipple(menuBtn, true);
        menuBtn.setOnClickListener(this::showMainMenu);
        header.addView(menuBtn, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header);

        // Search box
        search = new EditText(this);
        search.setHint("Search apps");
        search.setHintTextColor(SUB);
        search.setTextColor(TEXT);
        search.setSingleLine(true);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.setBackground(rounded(CARD, dp(24)));
        search.setPadding(dp(18), dp(12), dp(18), dp(12));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.getDefault());
                render();
            }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (firstVisible != null) launch(firstVisible);
            return true;
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMargins(dp(16), dp(8), dp(16), dp(6));
        root.addView(search, sp);

        // Category chips
        chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(dp(12), dp(6), dp(12), dp(6));
        chipScroll.addView(chipRow);
        root.addView(chipScroll);

        // App grid area
        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), 0, dp(8), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    // ------------------------------------------------------------------ data

    private void reload() {
        worker.execute(() -> {
            final List<AppInfo> loaded = AppRepository.load(getApplicationContext(), prefs);
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                apps = loaded;
                loadedOnce = true;
                render();
            });
        });
    }

    private List<String> categoryOrder() {
        List<String> order = new ArrayList<>();
        for (String c : Categorizer.ORDER) {
            if (!c.equals(Categorizer.SYSTEM) && !c.equals(Categorizer.OTHER)) order.add(c);
        }
        for (String c : prefs.customCategories()) {
            if (!order.contains(c)) order.add(c);
        }
        order.add(Categorizer.SYSTEM);
        order.add(Categorizer.OTHER);
        return order;
    }

    private void sortApps(List<AppInfo> list) {
        final Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        final Comparator<AppInfo> byName = (a, b) -> collator.compare(a.label, b.label);
        int mode = prefs.sortMode();
        if (mode == Prefs.SORT_MOST_USED) {
            final Map<String, Integer> counts = new HashMap<>();
            for (AppInfo a : list) counts.put(a.key(), prefs.launches(a.key()));
            Collections.sort(list, (a, b) -> {
                int c = Integer.compare(counts.get(b.key()), counts.get(a.key()));
                return c != 0 ? c : byName.compare(a, b);
            });
        } else if (mode == Prefs.SORT_RECENT) {
            Collections.sort(list, (a, b) -> {
                int c = Long.compare(b.installTime, a.installTime);
                return c != 0 ? c : byName.compare(a, b);
            });
        } else {
            Collections.sort(list, byName);
        }
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        Set<String> hidden = prefs.hidden();
        Set<String> favs = prefs.favorites();

        // Counts for chips + status line
        Map<String, Integer> counts = new HashMap<>();
        int visibleTotal = 0;
        int favCount = 0;
        int hiddenCount = 0;
        for (AppInfo a : apps) {
            if (hidden.contains(a.key())) { hiddenCount++; continue; }
            visibleTotal++;
            Integer n = counts.get(a.category);
            counts.put(a.category, n == null ? 1 : n + 1);
            if (favs.contains(a.key())) favCount++;
        }
        status.setText(visibleTotal + " apps · " + counts.size() + " groups"
                + (hiddenCount > 0 ? " · " + hiddenCount + " hidden" : ""));

        renderChips(counts, favCount, visibleTotal);

        // Filter
        List<AppInfo> pool = new ArrayList<>();
        boolean showHidden = HIDDEN.equals(selected);
        for (AppInfo a : apps) {
            boolean isHidden = hidden.contains(a.key());
            if (showHidden != isHidden) continue;
            if (!query.isEmpty()
                    && !a.label.toLowerCase(Locale.getDefault()).contains(query)
                    && !a.packageName.toLowerCase(Locale.ROOT).contains(query)) continue;
            if (FAV.equals(selected) && !favs.contains(a.key())) continue;
            if (!ALL.equals(selected) && !FAV.equals(selected) && !showHidden
                    && !selected.equals(a.category)) continue;
            pool.add(a);
        }
        sortApps(pool);

        content.removeAllViews();
        firstVisible = pool.isEmpty() ? null : pool.get(0);

        if (!loadedOnce) return;
        if (pool.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setTextColor(SUB);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(48), dp(24), dp(24));
            if (!query.isEmpty()) empty.setText("No apps match \"" + query + "\"");
            else if (FAV.equals(selected)) empty.setText("No favourites yet.\nLong-press any app → Add to favourites.");
            else if (showHidden) empty.setText("No hidden apps.");
            else empty.setText("Nothing here.");
            content.addView(empty);
            return;
        }

        boolean grouped = prefs.grouped() && ALL.equals(selected) && query.isEmpty();
        if (grouped) {
            List<AppInfo> favList = new ArrayList<>();
            for (AppInfo a : pool) if (favs.contains(a.key())) favList.add(a);
            if (!favList.isEmpty()) addSection(FAV, favList, favs);

            for (String cat : categoryOrder()) {
                List<AppInfo> list = new ArrayList<>();
                for (AppInfo a : pool) if (cat.equals(a.category)) list.add(a);
                if (!list.isEmpty()) addSection(cat, list, favs);
            }
            // Apps whose saved category no longer exists
            List<AppInfo> orphans = new ArrayList<>();
            List<String> order = categoryOrder();
            for (AppInfo a : pool) if (!order.contains(a.category)) orphans.add(a);
            if (!orphans.isEmpty()) addSection(Categorizer.OTHER, orphans, favs);
        } else {
            String title = query.isEmpty() ? null : pool.size() + " result" + (pool.size() == 1 ? "" : "s");
            addSection(title, pool, favs);
        }
    }

    private void renderChips(Map<String, Integer> counts, int favCount, int total) {
        chipRow.removeAllViews();
        addChip(ALL, ALL + "  " + total);
        if (favCount > 0 || FAV.equals(selected)) addChip(FAV, FAV + "  " + favCount);
        for (String cat : categoryOrder()) {
            Integer n = counts.get(cat);
            if (n != null && n > 0) addChip(cat, cat + "  " + n);
        }
        if (HIDDEN.equals(selected)) addChip(HIDDEN, "Hidden");
    }

    private void addChip(final String key, String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        boolean on = key.equals(selected);
        chip.setTextColor(on ? 0xFFFFFFFF : TEXT);
        chip.setBackground(rounded(on ? ACCENT : CARD, dp(18)));
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));
        chip.setOnClickListener(v -> {
            selected = key;
            scroll.scrollTo(0, 0);
            render();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, dp(4), 0);
        chipRow.addView(chip, lp);
    }

    private void addSection(String title, List<AppInfo> list, Set<String> favs) {
        if (title != null) {
            TextView h = new TextView(this);
            h.setText(title.toUpperCase(Locale.getDefault()) + "   " + list.size());
            h.setTextColor(SUB);
            h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            h.setTypeface(Typeface.DEFAULT_BOLD);
            h.setLetterSpacing(0.08f);
            h.setPadding(dp(12), dp(18), dp(12), dp(4));
            content.addView(h);
        }

        int cols = prefs.columns();
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(cols);
        for (AppInfo a : list) grid.addView(makeCell(a, favs.contains(a.key())), cellParams());
        // Fill the last row so cells keep equal widths
        int rem = list.size() % cols;
        if (rem != 0) {
            for (int i = rem; i < cols; i++) grid.addView(new View(this), cellParams());
        }
        content.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        return lp;
    }

    private View makeCell(final AppInfo a, boolean fav) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(dp(4), dp(10), dp(4), dp(10));
        setRipple(cell, false);

        int size = prefs.columns() >= 6 ? 44 : prefs.columns() == 5 ? 48 : 54;
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(a.icon);
        cell.addView(iv, new LinearLayout.LayoutParams(dp(size), dp(size)));

        TextView tv = new TextView(this);
        tv.setText(fav ? "★ " + a.label : a.label);
        tv.setTextColor(TEXT);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setGravity(Gravity.CENTER);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(6);
        cell.addView(tv, tlp);

        cell.setContentDescription(a.label);
        cell.setOnClickListener(v -> launch(a));
        cell.setOnLongClickListener(v -> {
            showAppMenu(v, a);
            return true;
        });
        return cell;
    }

    // ------------------------------------------------------------------ actions

    private void launch(AppInfo a) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setComponent(new ComponentName(a.packageName, a.activityName));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
            prefs.incLaunch(a.key());
            hideKeyboard();
            if (!query.isEmpty()) search.setText("");
        } catch (Exception e) {
            toast("Couldn't open " + a.label);
            reload();
        }
    }

    private void showAppMenu(View anchor, final AppInfo a) {
        final boolean fav = prefs.favorites().contains(a.key());
        final boolean hid = prefs.hidden().contains(a.key());
        PopupMenu pm = new PopupMenu(this, anchor);
        Menu m = pm.getMenu();
        m.add(0, 1, 0, fav ? "Remove from favourites" : "Add to favourites");
        m.add(0, 2, 1, "Move to category…  (" + a.category + ")");
        m.add(0, 3, 2, hid ? "Unhide" : "Hide from list");
        m.add(0, 4, 3, "App info");
        m.add(0, 5, 4, "Uninstall");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    prefs.toggleFavorite(a.key());
                    toast(fav ? "Removed from favourites" : "Added to favourites");
                    render();
                    return true;
                case 2:
                    showMoveDialog(a);
                    return true;
                case 3:
                    prefs.toggleHidden(a.key());
                    toast(hid ? a.label + " is visible again" : a.label + " hidden");
                    render();
                    return true;
                case 4:
                    safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + a.packageName)));
                    return true;
                case 5:
                    safeStart(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.packageName)));
                    return true;
                default:
                    return false;
            }
        });
        pm.show();
    }

    private void showMoveDialog(final AppInfo a) {
        final List<String> cats = categoryOrder();
        final List<String> items = new ArrayList<>();
        for (String c : cats) items.add(c.equals(a.category) ? "✓  " + c : "     " + c);
        items.add("＋  New category…");
        items.add("↺  Automatic (" + a.autoCategory + ")");

        new AlertDialog.Builder(this)
                .setTitle("Move \"" + a.label + "\" to")
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    if (which < cats.size()) {
                        setCategory(a, cats.get(which));
                    } else if (which == cats.size()) {
                        askNewCategory(a);
                    } else {
                        prefs.setCategoryOverride(a.key(), null);
                        a.category = a.autoCategory;
                        render();
                    }
                })
                .show();
    }

    private void setCategory(AppInfo a, String cat) {
        prefs.setCategoryOverride(a.key(), cat.equals(a.autoCategory) ? null : cat);
        a.category = cat;
        toast(a.label + " → " + cat);
        render();
    }

    private void askNewCategory(final AppInfo a) {
        final EditText input = new EditText(this);
        input.setHint("e.g. Work, Kids, Daily");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("New category")
                .setView(box)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty() || name.equals(ALL) || name.equals(FAV) || name.equals(HIDDEN)) {
                        toast("Please enter a different name");
                        return;
                    }
                    if (name.length() > 24) name = name.substring(0, 24);
                    prefs.addCustomCategory(name);
                    setCategory(a, name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMainMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        Menu m = pm.getMenu();

        SubMenu sort = m.addSubMenu(0, 10, 0, "Sort apps");
        String[] sorts = {"A – Z", "Most used", "Recently installed"};
        for (int i = 0; i < sorts.length; i++) {
            sort.add(1, 100 + i, i, sorts[i]).setCheckable(true).setChecked(prefs.sortMode() == i);
        }
        sort.setGroupCheckable(1, true, true);

        m.add(0, 11, 1, "Group by category").setCheckable(true).setChecked(prefs.grouped());

        SubMenu cols = m.addSubMenu(0, 12, 2, "Icons per row");
        for (int c = 3; c <= 6; c++) {
            cols.add(2, 200 + c, c, c + " icons").setCheckable(true).setChecked(prefs.columns() == c);
        }
        cols.setGroupCheckable(2, true, true);

        m.add(0, 13, 3, "Show hidden apps (" + prefs.hidden().size() + ")");
        m.add(0, 14, 4, isDefaultHome() ? "Change home screen app" : "Set as home screen");
        m.add(0, 15, 5, "Refresh app list");
        m.add(0, 16, 6, "Reset organisation");

        pm.setOnMenuItemClickListener(item -> onMainMenuItem(item));
        pm.show();
    }

    private boolean onMainMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id >= 100 && id < 103) {
            prefs.setSortMode(id - 100);
            render();
        } else if (id >= 203 && id <= 206) {
            prefs.setColumns(id - 200);
            render();
        } else if (id == 11) {
            prefs.setGrouped(!prefs.grouped());
            render();
        } else if (id == 13) {
            selected = HIDDEN;
            scroll.scrollTo(0, 0);
            render();
        } else if (id == 14) {
            toast("Pick \"Home Organizer\" as your Home app");
            if (!safeStart(new Intent(Settings.ACTION_HOME_SETTINGS))) {
                safeStart(new Intent(Settings.ACTION_SETTINGS));
            }
        } else if (id == 15) {
            reload();
            toast("App list refreshed");
        } else if (id == 16) {
            new AlertDialog.Builder(this)
                    .setTitle("Reset organisation?")
                    .setMessage("Clears favourites, hidden apps and your custom categories. Apps go back to automatic categories.")
                    .setPositiveButton("Reset", (d, w) -> {
                        prefs.resetOrganization();
                        selected = ALL;
                        reload();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            return false; // sub-menu headers
        }
        return true;
    }

    // ------------------------------------------------------------------ helpers

    private void resetView() {
        if (search == null) return;
        selected = ALL;
        if (!query.isEmpty()) search.setText("");
        else render();
        hideKeyboard();
        scroll.scrollTo(0, 0);
        chipScroll.scrollTo(0, 0);
    }

    private boolean isDefaultHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, 0);
        return ri != null && ri.activityInfo != null && getPackageName().equals(ri.activityInfo.packageName);
    }

    private boolean safeStart(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && search != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        if (search != null) search.clearFocus();
    }

    private void setRipple(View v, boolean borderless) {
        TypedValue tv = new TypedValue();
        int attr = borderless ? android.R.attr.selectableItemBackgroundBorderless
                : android.R.attr.selectableItemBackground;
        if (getTheme().resolveAttribute(attr, tv, true)) v.setBackgroundResource(tv.resourceId);
        v.setClickable(true);
        v.setFocusable(true);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
