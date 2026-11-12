package com.pankaj.homeorganizer;

import android.content.pm.ApplicationInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Sorts apps into categories using their name, package and Android's own category tag. */
public final class Categorizer {

    public static final String SOCIAL = "Social";
    public static final String PRODUCTIVITY = "Productivity";
    public static final String FINANCE = "Finance";
    public static final String SHOPPING = "Shopping";
    public static final String FOOD = "Food & Delivery";
    public static final String MEDIA = "Music & Video";
    public static final String PHOTOS = "Photos";
    public static final String TRAVEL = "Travel & Maps";
    public static final String NEWS = "News & Reading";
    public static final String HEALTH = "Health & Fitness";
    public static final String GAMES = "Games";
    public static final String TOOLS = "Tools";
    public static final String SYSTEM = "System";
    public static final String OTHER = "Other";

    /** Display order of built-in categories. */
    public static final List<String> ORDER = Collections.unmodifiableList(Arrays.asList(
            SOCIAL, PRODUCTIVITY, FINANCE, SHOPPING, FOOD, MEDIA, PHOTOS, TRAVEL,
            NEWS, HEALTH, GAMES, TOOLS, SYSTEM, OTHER));

    /** Checked in this order; the first match wins. */
    private static final Object[][] RULES = {
            {FINANCE, new String[]{"pay", "upi", "bank", "money", "wallet", "invest", "groww", "zerodha",
                    "kite", "upstox", "paytm", "phonepe", "bhim", "cred", "finance", "credit", "loan",
                    "sbi", "hdfc", "icici", "axis", "kotak", "mutual", "stock", "trading", "insurance"}},
            {FOOD, new String[]{"swiggy", "zomato", "blinkit", "zepto", "bigbasket", "instamart", "dunzo",
                    "domino", "pizza", "food", "eats", "mcdonald", "kfc", "starbucks", "grocer"}},
            {MEDIA, new String[]{"youtube", "spotify", "music", "video", "netflix", "hotstar", "jiocinema",
                    "mxplayer", "mx player", "gaana", "wynk", "saavn", "podcast", "sonyliv", "zee5", "vlc",
                    "player", "audible", "shazam", "radio", "movie", "cinema", "stream"}},
            {GAMES, new String[]{"game", "puzzle", "ludo", "chess", "pubg", "bgmi", "freefire", "free fire",
                    "candy", "clash", "subway", "roblox", "minecraft", "dream11", "rummy", "carrom",
                    "solitaire", "sudoku", "battle", "racing"}},
            {PRODUCTIVITY, new String[]{"docs", "sheets", "slides", "office", "word", "excel", "powerpoint",
                    "drive", "gmail", "mail", "outlook", "calendar", "notes", "keep", "notion", "todo",
                    "to do", "task", "teams", "zoom", "meet", "slack", "pdf", "scanner", "translate",
                    "claude", "chatgpt", "gemini", "copilot", "perplexity", "onenote", "evernote", "trello",
                    "jira", "dropbox", "adobe", "canva", "linkedin", "naukri", "job"}},
            {SOCIAL, new String[]{"whatsapp", "instagram", "facebook", "telegram", "snapchat", "twitter",
                    "sharechat", "signal", "discord", "reddit", "threads", "barcelona", "messenger",
                    "messag", "chat", "pinterest", "tinder", "bumble", "quora", "tiktok", "truecaller",
                    "social", "sms", "dating"}},
            {PHOTOS, new String[]{"photo", "gallery", "camera", "snapseed", "lightroom", "picsart", "album",
                    "selfie", "collage", "wallpaper"}},
            {SHOPPING, new String[]{"amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "shop",
                    "snapdeal", "tatacliq", "ebay", "aliexpress", "jiomart", "dmart", "lenskart", "croma",
                    "olx", "deal", "coupon"}},
            {TRAVEL, new String[]{"maps", "uber", "olacabs", "rapido", "irctc", "trip", "goibibo", "airbnb",
                    "booking", "travel", "railyatri", "redbus", "flight", "cab", "metro", "indigo", "ixigo",
                    "waze", "navigation", "gps", "transit", "hotel", "fastag", "parking"}},
            {NEWS, new String[]{"news", "inshorts", "kindle", "book", "dailyhunt", "medium", "magazine",
                    "pocket", "wattpad", "pratilipi", "comic", "manga", "times", "learn", "duolingo",
                    "byju", "unacademy", "school", "course", "dictionary"}},
            {HEALTH, new String[]{"fit", "health", "workout", "yoga", "step", "cult", "healthify", "medic",
                    "pharm", "1mg", "practo", "sleep", "meditat", "calm", "headspace", "period", "diet",
                    "water", "running", "strava", "doctor", "hospital", "wear"}},
            {TOOLS, new String[]{"calculator", "calc", "clock", "alarm", "settings", "file", "contacts",
                    "dialer", "phone", "weather", "compass", "flashlight", "torch", "recorder", "vpn",
                    "cleaner", "launcher", "keyboard", "browser", "chrome", "firefox", "brave", "opera",
                    "authenticator", "vending", "play store", "backup", "security", "antivirus", "wifi",
                    "bluetooth", "screen", "remote", "sim toolkit", "download", "magnifier", "battery",
                    "smartthings", "home", "password", "qr", "scan", "voice", "assistant", "search"}},
    };

    private Categorizer() {}

    public static String categorize(ApplicationInfo ai, String pkg, String label) {
        String hay = (pkg + " " + label).toLowerCase(Locale.ROOT);

        for (Object[] rule : RULES) {
            for (String kw : (String[]) rule[1]) {
                if (hay.contains(kw)) return (String) rule[0];
            }
        }

        if (ai != null) {
            switch (ai.category) {
                case ApplicationInfo.CATEGORY_GAME: return GAMES;
                case ApplicationInfo.CATEGORY_AUDIO:
                case ApplicationInfo.CATEGORY_VIDEO: return MEDIA;
                case ApplicationInfo.CATEGORY_IMAGE: return PHOTOS;
                case ApplicationInfo.CATEGORY_SOCIAL: return SOCIAL;
                case ApplicationInfo.CATEGORY_NEWS: return NEWS;
                case ApplicationInfo.CATEGORY_MAPS: return TRAVEL;
                case ApplicationInfo.CATEGORY_PRODUCTIVITY: return PRODUCTIVITY;
                default: break;
            }
            if ((ai.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
                return SYSTEM;
            }
        }
        return OTHER;
    }
}
