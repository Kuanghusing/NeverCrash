package nnnn.aaaaa.nevercrash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private ListView lvApps;
    private List<App> mApps;
    private AppAdapter mAppAdapter;
    private SharedPreferences mSharedPreferences;
    static final String PREF_ON = "on";
    static final String PREF_NAME = "x";
    static final String PREF_TOAST = "show_toast";
    private static final String PREF_SHOW_SYSTEM = "show_system";
    private static final String PREF_DONT_SHOW_AGAIN = "warning_showed";
    private boolean filter_on = false;
    private boolean filter_off = false;
    private static final String WARNING = "1.本模块需要Xposed支持\n\n" +
            "2.模块旨在忽略目标应用运行时的错误，并非处理错误！应用后可能发生不可预料的问题，请谨慎使用！\n\n" +
            "3.如非必须勿应用于系统应用\n\n" +
            "4.因为各种不可描述的原因，Android应用在使用中难免会发生异常，异常不被处理将会导致闪退，而某些异常只会影响部分非核心功能的使用，这时就是这个模块的使用场景";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_main);
        mSharedPreferences = getSharedPreferences(PREF_NAME, MODE_WORLD_READABLE);
        lvApps = (ListView) findViewById(R.id.lvApps);
        mApps = new ArrayList<>();


        mAppAdapter = new AppAdapter();
        lvApps.setAdapter(mAppAdapter);
        resetList();

        warning();

    }

    private void warning() {
        if (mSharedPreferences.getBoolean(PREF_DONT_SHOW_AGAIN, false)) return;
        new AlertDialog.Builder(this)
                .setTitle("使用前请认真阅读")
                .setMessage(WARNING)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("不再提示", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSharedPreferences
                                .edit()
                                .putBoolean(PREF_DONT_SHOW_AGAIN, true)
                                .apply();
                    }
                })
                .show();
    }

    void updatePref() {
        Set<String> stringSet = new HashSet<>();
        for (App app : mApps) {
            if (app.get_it) {
                stringSet.add(app.packageName);
            }
        }
        mSharedPreferences.edit()
                .putStringSet(PREF_ON, stringSet)
                .apply();
    }

    interface Filter {
        boolean filter(App app);
    }

    void filter(Filter filter) {
        List<App> apps = new ArrayList<>();
        for (App app : mApps) {
            if (filter.filter(app)) {
                apps.add(app);
            }
        }
        mApps = apps;
//        mAppAdapter.notifyDataSetChanged();
    }

    void resetList() {
        Set<String> stringSet = mSharedPreferences.getStringSet(PREF_ON, null);
        mApps.clear();
        List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(0);
        for (PackageInfo packageInfo : installedPackages) {
            if (!mSharedPreferences.getBoolean(PREF_SHOW_SYSTEM, false))
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                    continue;


            if (packageInfo.packageName.equals("nnnn.aaaaa.nevercrash")) continue;
            boolean get_it = stringSet != null && stringSet.contains(packageInfo.packageName);
//            if (filter_on && !get_it) continue;
//            if (filter_off && get_it) continue;
            mApps.add(new App(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString(),
                    packageInfo.applicationInfo.loadIcon(getPackageManager()), get_it, packageInfo.packageName));
        }
//        Collections.sort(mApps);
//        mAppAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.item_show_system).setChecked(mSharedPreferences.getBoolean(PREF_SHOW_SYSTEM, false));
        menu.findItem(R.id.item_show_toast).setChecked(mSharedPreferences.getBoolean(PREF_TOAST, true));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_show_system:
                item.setChecked(!item.isChecked());
                mSharedPreferences.edit()
                        .putBoolean(PREF_SHOW_SYSTEM, item.isChecked())
                        .apply();
                resetList();
                if (filter_on) filter_on();
                if (filter_off) filter_off();
                mAppAdapter.notifyDataSetChanged();
                break;
            case R.id.item_show_toast:
                item.setChecked(!item.isChecked());
                mSharedPreferences.edit()
                        .putBoolean(PREF_TOAST, item.isChecked())
                        .apply();
                break;
            case R.id.item_cancel_all:
                for (int i = 0; i < mApps.size(); i++) {
                    mApps.get(i).get_it = false;
                }
                updatePref();
                resetList();
                mAppAdapter.notifyDataSetChanged();
                break;
            case R.id.item_on:
                item.setChecked(true);
                filter_on();
                mAppAdapter.notifyDataSetChanged();
                break;
            case R.id.item_off:
                item.setChecked(true);
                filter_off();
                mAppAdapter.notifyDataSetChanged();
                break;
            case R.id.item_all:
                filter_on = filter_off = false;
                item.setChecked(true);
                resetList();

                filter(new Filter() {
                    @Override
                    public boolean filter(App app) {
                        return true;
                    }
                });
                mAppAdapter.notifyDataSetChanged();
                break;
            case R.id.item_help:
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("使用须知")
                        .setMessage(WARNING)
                        .show();
                break;
        }
        return true;
    }

    private void filter_on() {
        filter_on = true;
        filter_off = false;
        resetList();
        filter(new Filter() {
            @Override
            public boolean filter(App app) {
                return app.get_it;
            }
        });
    }

    private void filter_off() {
        filter_off = true;
        filter_on = false;
        resetList();
        filter(new Filter() {
            @Override
            public boolean filter(App app) {
                return !app.get_it;
            }
        });
    }

    class AppAdapter extends BaseAdapter {

        public AppAdapter() {
        }

        @Override
        public int getCount() {
            return mApps != null ? mApps.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mApps != null ? mApps.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            Holder holder;
            ImageView ivIcon;
            TextView tvName;
            final Switch mSwitch;
            if (convertView != null) {
                holder = (Holder) convertView.getTag();
                ivIcon = holder.ivIcon;
                tvName = holder.tvName;
                mSwitch = holder.mSwitch;
                view = convertView;
            } else {
                view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_app, parent, false);
                ivIcon = (ImageView) view.findViewById(R.id.ivIcon);
                tvName = (TextView) view.findViewById(R.id.tvName);
                mSwitch = (Switch) view.findViewById(R.id.sw);
                holder = new Holder(ivIcon, tvName, mSwitch);
                view.setTag(holder);

/*                mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mApps.get(position).get_it = isChecked;
                    }
                });*/

            }
            view.setOnClickListener(new View.OnClickListener() {
                int p = position;

                @Override
                public void onClick(View v) {
                    boolean get_it = mApps.get(p).get_it;
                    mApps.get(p).get_it = !get_it;
                    mSwitch.setChecked(!get_it);
                    updatePref();
                }
            });
            mSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean get_it = mApps.get(position).get_it;
                    mApps.get(position).get_it = !get_it;
                    updatePref();
                }
            });

            ivIcon.setImageDrawable(mApps.get(position).icon);
            tvName.setText(mApps.get(position).name);
//            mSwitch.setSelected(mApps.get(position).get_it);
            mSwitch.setChecked(mApps.get(position).get_it);

//            Log.d("", mApps.get(position).toString());

            return view;
        }


        class Holder {
            ImageView ivIcon;
            TextView tvName;
            Switch mSwitch;

            Holder(ImageView ivIcon, TextView tvName, Switch aSwitch) {
                this.ivIcon = ivIcon;
                this.tvName = tvName;
                this.mSwitch = aSwitch;
            }
        }
    }

    interface OnSwitchListener {
        void onSwitch(int position);
    }

    class App implements Comparable<App> {
        String name;
        Drawable icon;
        boolean get_it;
        String packageName;

        App(String name, Drawable icon, boolean get_it, String packageName) {
            this.name = name;
            this.icon = icon;
            this.get_it = get_it;
            this.packageName = packageName;
        }

        @Override
        public String toString() {
            return "name:" + name + " get_it:" + get_it;
        }

        @Override
        public int compareTo(App o) {

            if (this.get_it = o.get_it) return this.name.compareTo(o.name);
            else if (this.get_it && !o.get_it) return 1;
            else if (!this.get_it && o.get_it) return -1;
            else return this.name.compareTo(o.name);
        }
    }

}
