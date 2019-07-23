package com.acentria.benslist;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;

import java.util.Locale;

public class Lang {

    /**
     * get lang phrase by lang key
     *
     * @param key - phrase key
     * @return prhase value
     */
    public static String get(String key){
        if ( Config.cacheLang.containsKey(key) )
            return Config.cacheLang.get(key);

        if ( Config.cacheLang.containsKey("android_"+key) )
            return Config.cacheLang.get("android_"+key);

        return "No phrase value found";
    }

    /**
     * get system language code, detecting between website default lang and device lang
     *
     * @return String - system lang code
     */
    public static String getSystemLang(){
        String systemLang;
        String deviceLang =  Locale.getDefault().getLanguage();
        String userLang = Utils.getSPConfig("select_lang", null);

        if ( userLang != null && !userLang.isEmpty() /*&& Config.cacheLangCodes.indexOf(userLang) >= 0*/ ) {
            systemLang = userLang;
        }
        else if ( Config.cacheLangCodes.indexOf(deviceLang) >= 0 ) {
            systemLang = deviceLang;
        }
        else if ( !Config.cacheConfig.isEmpty() ) {
            systemLang = Config.cacheConfig.get("system_lang");
        }
        else {
            systemLang = deviceLang;
        }

        return systemLang;
    }

    /**
     * checks is the user device supports rtl and current lang is also rtl
     * */
    public static Boolean isRtl() {
        return isLangRtl() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? true : false;
    }

    /**
     * checks is the user device supports legacy rtl (before 4.4) and current lang is also rtl
     * */
    public static Boolean isLegacyRtl() {
        return isLangRtl()
               && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
               && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? true : false;
    }

    /**
     * checks is the user language is rtl
     * */
    public static Boolean isLangRtl() {
        Boolean isRtl = false;
        if (Config.cacheLanguages.containsKey(getSystemLang())) {
            if (Config.cacheLanguages.get(getSystemLang()).get("dir").equals("rtl")) {
                isRtl = true;
            }
        }

        return isRtl;
    }

    @TargetApi(17)
    /**
     * enable rtl if it's Android 4.2 (SDK 17) device and current language is RTL
     */
    public static void setDirection(Activity parent) {
        // switch parent view to rtl
        parent.getWindow().getDecorView().setLayoutDirection(isRtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

        // set app to rtl
        Locale locale = new Locale(getSystemLang());
        Locale.setDefault(locale);

        Configuration configs = new Configuration();
        configs.locale = locale;
        Config.context.getResources().updateConfiguration(configs, Config.context.getResources().getDisplayMetrics());
    }
}