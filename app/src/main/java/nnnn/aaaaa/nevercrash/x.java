package nnnn.aaaaa.nevercrash;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.widget.Toast;

import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class x implements IXposedHookLoadPackage {
    private static final String APPLICATION_CLASS = "android.app.Application";
    private static final String METHOD_ON_CREATE = "onCreate";
    private static final String PACKAGE_NAME = "nnnn.aaaaa.nevercrash";
    private static XSharedPreferences sXSharedPreferences = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("reach package:" + lpparam.packageName);

        if (sXSharedPreferences == null) {
            sXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, MainActivity.PREF_NAME);
            sXSharedPreferences.makeWorldReadable();
        }
        sXSharedPreferences.reload();
        Set<String> packagesEnabled = sXSharedPreferences.getStringSet(MainActivity.PREF_ON, null);
        if (packagesEnabled == null) return;
        if (!packagesEnabled.contains(lpparam.packageName)) return;


        XposedBridge.log("catch package: " + lpparam.packageName);
        XposedHelpers.findAndHookMethod(APPLICATION_CLASS, lpparam.classLoader, METHOD_ON_CREATE, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("onCreate");
                CrashHandler.insert(throwable -> new Handler(((Context) param.thisObject).getMainLooper()).post(() -> {
                    try {
                        XposedBridge.log(throwable.toString());
                        sXSharedPreferences.reload();
                        if (sXSharedPreferences.getBoolean(MainActivity.PREF_TOAST, true))
                            Toast.makeText((Context) param.thisObject, getCrashTip() + throwable.toString(), Toast.LENGTH_LONG).show();
                    } catch (Throwable e) {
                        XposedBridge.log(e.toString());
                    }
                }));
            }
        });
    }

    private static String getCrashTip() {

        Context context = null;
        try {
            context = AndroidAppHelper.currentApplication().createPackageContext(PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            return context.getString(R.string.crash_tip) + ": ";
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
