package nnnn.aaaaa.nevercrash;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
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

        if (sXSharedPreferences == null) {
            sXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, MainActivity.PREF_NAME);
        }
        sXSharedPreferences.reload();
        Set<String> packagesDisabled = sXSharedPreferences.getStringSet(MainActivity.PREF_OFF, null);
        boolean ignore = lpparam.packageName.equals("android");
        boolean isXposedInstaller = lpparam.packageName.equals("de.robv.android.xposed.installer");
        boolean isSystemApp = (lpparam.appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean handleSystemApp = sXSharedPreferences.getBoolean(MainActivity.PREF_SHOW_SYSTEM, false);
        if ((isSystemApp && !handleSystemApp) || ignore || isXposedInstaller || (packagesDisabled != null && packagesDisabled.contains(lpparam.packageName)))
            return;


        XposedBridge.log("catch package: " + lpparam.packageName);
        XposedHelpers.findAndHookMethod(APPLICATION_CLASS, lpparam.classLoader, METHOD_ON_CREATE, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
//                XposedBridge.log("onCreate");
                Application currentApplicationContext = (Application) param.thisObject;
                CrashHandler.insert(throwable -> new Handler(currentApplicationContext.getMainLooper()).post(() -> {
                    try {
                        XposedBridge.log(throwable);
                        Toast.makeText((Context) param.thisObject, getCrashTip() + lpparam.appInfo.loadLabel(currentApplicationContext.getPackageManager()) + " " + throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        x.this.notify(currentApplicationContext, lpparam.appInfo, throwable);
                    } catch (Throwable e) {
                        XposedBridge.log(e.toString());
                    }
                }));


            }
        });
        if (lpparam.packageName.equals(PACKAGE_NAME)) {
            try {
                XposedHelpers.findAndHookMethod(PACKAGE_NAME + ".Check", lpparam.classLoader, "isXposedActived", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        return true;
                    }
                });
            } catch (Exception e) {
                XposedBridge.log(e);
            }
        }

    }

    private void notify(Application application, ApplicationInfo applicationInfo, Throwable throwable) {
        try {
            String appName = applicationInfo.loadLabel(application.getPackageManager()).toString();
            StringBuilder stackTrace = new StringBuilder();
            Throwable p = throwable.getCause() != null ? throwable.getCause() : throwable;
            for (StackTraceElement stackTraceElement : p.getStackTrace()) {
                stackTrace.append("  at ")
                        .append(stackTraceElement)
                        .append("\n");
            }
            NotificationManager notificationManager = (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
            Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            boolean targetO = applicationInfo.targetSdkVersion >= Build.VERSION_CODES.O;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && targetO) {
                NotificationChannel notificationChannel = new NotificationChannel("catch_exception", "catch exception[Xposed]", NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(notificationChannel);
                Notification notification = new Notification.Builder(application, "catch_exception")
                        .setContentTitle(moduleContext.getString(R.string.crash_tip) + " - " + appName)
                        .setContentText(throwable.getLocalizedMessage())
                        .setStyle(new Notification.BigTextStyle()
                                .bigText(throwable.getLocalizedMessage() + "\n-----\n" +
                                        stackTrace.toString()))
                        .setSmallIcon(applicationInfo.icon)
                        .build();
                notificationManager.notify(new Random().nextInt(), notification);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Notification notification = new Notification.Builder(application)
                        .setContentTitle(moduleContext.getString(R.string.crash_tip) + " - " + appName)
                        .setContentText(throwable.getLocalizedMessage())
                        .setStyle(new Notification.BigTextStyle()
                                .bigText(throwable.getLocalizedMessage() + "\n-----\n" +
                                        stackTrace.toString()))
                        .setSmallIcon(applicationInfo.icon)
                        .build();
                notificationManager.notify(new Random().nextInt(), notification);
            }
        } catch (Exception e) {
            XposedBridge.log(e);
        }
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
