package nnnn.aaaaa.nevercrash;

import android.content.Context;
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
    static String APPLICATION_CLASS = "android.app.Application";
    static String ONCREATE_METHOD = "onCreate";
    XSharedPreferences mXSharedPreferences = new XSharedPreferences("nnnn.aaaaa.nevercrash", MainActivity.PREF_NAME);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        mXSharedPreferences.reload();
        Set<String> stringSet = mXSharedPreferences.getStringSet(MainActivity.PREF_ON, null);
//        if (!lpparam.packageName.equals(TARGET)) return;
        if (stringSet == null) return;
        if (!stringSet.contains(lpparam.packageName)) return;


        XposedBridge.log("catch package: " + lpparam.packageName);
        XposedHelpers.findAndHookMethod(APPLICATION_CLASS, lpparam.classLoader, ONCREATE_METHOD, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
//                Toast.makeText((Context)param.thisObject, "onCreate!", Toast.LENGTH_LONG).show();
                XposedBridge.log("onCreate");
                CrashHandler.insert(new CrashHandler.ExceptionHandler() {
                    @Override
                    public void handlerException(final Throwable throwable) {
                        new Handler(((Context) param.thisObject).getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    XposedBridge.log(throwable.toString());
                                    mXSharedPreferences.reload();
                                    if (mXSharedPreferences.getBoolean(MainActivity.PREF_TOAST, true))
                                        Toast.makeText((Context) param.thisObject, throwable.toString(), Toast.LENGTH_LONG).show();
                                } catch (Throwable e) {
                                    XposedBridge.log(e.toString());
                                }
                            }
                        });
                    }
                });
            }
        });
    }
}
