package com.pankaj.homeorganizer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

/** Reads the list of installed apps that have a home-screen icon. */
public final class AppRepository {
    private AppRepository() {}

    public static List<AppInfo> load(Context ctx, Prefs prefs) {
        PackageManager pm = ctx.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved = pm.queryIntentActivities(main, 0);
        List<AppInfo> out = new ArrayList<>(resolved.size());
        String self = ctx.getPackageName();

        for (ResolveInfo ri : resolved) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || self.equals(ai.packageName)) continue;

            String label;
            Drawable icon;
            try {
                label = String.valueOf(ri.loadLabel(pm));
                icon = ri.loadIcon(pm);
            } catch (Exception e) {
                continue;
            }

            long installed = 0;
            try {
                installed = pm.getPackageInfo(ai.packageName, 0).firstInstallTime;
            } catch (Exception ignored) {
            }

            String auto = Categorizer.categorize(ai.applicationInfo, ai.packageName, label);
            AppInfo app = new AppInfo(ai.packageName, ai.name, label, icon, installed, auto);
            String override = prefs.categoryOverride(app.key());
            if (override != null) app.category = override;
            out.add(app);
        }
        return out;
    }
}
