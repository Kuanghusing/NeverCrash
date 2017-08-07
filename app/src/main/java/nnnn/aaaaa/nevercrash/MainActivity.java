package nnnn.aaaaa.nevercrash;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nnnn.aaaaa.nevercrash.ArrayUtil.Filter;
import static nnnn.aaaaa.nevercrash.ArrayUtil.filter;
import static nnnn.aaaaa.nevercrash.ArrayUtil.map;

public class MainActivity extends Activity {
    private ListView lvApps;
    private List<App> mApps;
    private SearchView mSearchView;
    private AppAdapter mAppAdapter;
    private SharedPreferences mSharedPreferences;
    static final String PREF_ON = "on";
    static final String PREF_NAME = "x";
    static final String PREF_TOAST = "show_toast";
    private static final String PREF_SHOW_SYSTEM = "show_system";
    private static final String PREF_DONT_SHOW_AGAIN = "warning_showed";
    private boolean filter_on = false;
    private boolean filter_off = false;

    private List<App> mInitApps = new ArrayList<>();
    private List<App> mFilteredApps = new ArrayList<>();

    private boolean mIsSystemEnable = false;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ITSELF = MainActivity.class.getPackage().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        mSharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        lvApps = (ListView) findViewById(R.id.lvApps);
        mApps = new ArrayList<>();


        mAppAdapter = new AppAdapter();
        lvApps.setAdapter(mAppAdapter);

        mIsSystemEnable = mSharedPreferences.getBoolean(PREF_SHOW_SYSTEM, false);
        initApps();


    }

    private void initApps() {
        final Set<String> pref = mSharedPreferences.getStringSet(PREF_ON, null);
        List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(R.string.loading);
        progressDialog.setMessage(getString(R.string.loading_message));
        progressDialog.setCancelable(false);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                mInitApps = filter(map(installedPackages, packageInfo ->
                                new App(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString(),
                                        packageInfo.applicationInfo.loadIcon(getPackageManager()), pref != null &&
                                        pref.contains(packageInfo.packageName), packageInfo.packageName,
                                        (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0,
                                        packageInfo.lastUpdateTime, packageInfo.firstInstallTime)),
                        app -> !app.packageName.equals(ITSELF));
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                progressDialog.dismiss();
                initFilterApps();
            }
        }.execute();

    }

    private void initFilterApps() {
        mFilteredApps.clear();
        boolean show_system_app = mSharedPreferences.getBoolean(PREF_SHOW_SYSTEM, true);
        if (!show_system_app)
            mFilteredApps.addAll(filter(mInitApps, app -> !app.is_system_app));
        else
            mFilteredApps.addAll(mInitApps);
        mAppAdapter.update(mInitApps);
    }


    void updatePref() {
        Set<String> stringSet = new HashSet<>();
        for (App app : mInitApps) {
            if (app.get_it) {
                stringSet.add(app.packageName);
            }
        }
        mSharedPreferences.edit()
                .putStringSet(PREF_ON, stringSet)
                .apply();
    }


    void filterApps(Filter<App> filter) {
        mFilteredApps.clear();
        mFilteredApps.addAll(filter(mInitApps, app -> (mIsSystemEnable || !app.is_system_app) && filter.filter(app)));
        mAppAdapter.update(mFilteredApps);
    }

    void sort(Comparator<App> comparator) {
        Collections.sort(mFilteredApps, comparator);
        mAppAdapter.update(mFilteredApps);
    }


    private void warning() {
        if (mSharedPreferences.getBoolean(PREF_DONT_SHOW_AGAIN, false)) return;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.using_warning_title))
                .setMessage(getString(R.string.using_warning))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .setNegativeButton(getString(R.string.btn_dont_show_again), (dialog, which) -> mSharedPreferences
                        .edit()
                        .putBoolean(PREF_DONT_SHOW_AGAIN, true)
                        .apply())
                .show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.item_show_system).setChecked(mSharedPreferences.getBoolean(PREF_SHOW_SYSTEM, false));
        menu.findItem(R.id.item_show_toast).setChecked(mSharedPreferences.getBoolean(PREF_TOAST, true));
        mSearchView = (SearchView) menu.findItem(R.id.item_search).getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterApps(app -> app.name.toLowerCase().contains(query.toLowerCase()) || app.packageName.toLowerCase().contains(query.toLowerCase()));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterApps(app -> app.name.toLowerCase().contains(newText.toLowerCase()) || app.packageName.toLowerCase().contains(newText.toLowerCase()));
                return true;
            }
        });
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
                mIsSystemEnable = item.isChecked();
                filterApps(app -> true);
                break;
            case R.id.item_show_toast:
                item.setChecked(!item.isChecked());
                mSharedPreferences.edit()
                        .putBoolean(PREF_TOAST, item.isChecked())
                        .apply();
                break;
            case R.id.item_cancel_all:
                filterApps(app -> {
                    app.get_it = false;
                    return true;
                });
                updatePref();
                break;
            case R.id.item_on:
                item.setChecked(true);
                filterApps(app -> app.get_it);
                break;
            case R.id.item_off:
                item.setChecked(true);
                filterApps(app -> !app.get_it);
                break;
            case R.id.item_all:
                item.setChecked(true);
                filterApps(app -> true);
                break;
            case R.id.item_help:
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.using_warning_title))
                        .setMessage(getString(R.string.using_warning))
                        .show();
                break;
            case R.id.item_select_all:
                filterApps(app -> {
                    app.get_it = true;
                    return true;
                });
                updatePref();
                break;

            case R.id.item_sort_by_name:
                item.setChecked(true);
                sort((a, b) -> a.name.compareTo(b.name));
                break;
            case R.id.item_sort_by_name_reverse:
                item.setChecked(true);
                sort(Collections.reverseOrder((a, b) -> a.name.compareTo(b.name)));
                break;
            case R.id.item_sort_by_install_time:
                item.setChecked(true);
                sort((a, b) -> a.installTime == b.installTime ? 0 : (a.installTime > b.installTime ? 1 : -1));
                break;
            case R.id.item_sort_by_install_time_reverse:
                item.setChecked(true);
                sort(Collections.reverseOrder((a, b) -> a.installTime == b.installTime ? 0 : (a.installTime > b.installTime ? 1 : -1)));
                break;
            case R.id.item_sort_by_update_time:
                item.setChecked(true);
                sort((a, b) -> a.updateTime == b.updateTime ? 0 : (a.updateTime > b.updateTime ? 1 : -1));
                break;
            case R.id.item_sort_by_update_time_reverse:
                item.setChecked(true);
                sort(Collections.reverseOrder((a, b) -> a.updateTime == b.updateTime ? 0 : (a.updateTime > b.updateTime ? 1 : -1)));
                break;
        }
        return true;
    }


    class AppAdapter extends BaseAdapter {
        private List<App> mApps = new ArrayList<>();

        public AppAdapter() {
        }

        public void update(List<App> apps) {
            mApps.clear();
            mApps.addAll(apps);
            notifyDataSetChanged();
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
            TextView tvPackageName;
            TextView tvInstallTime;
            TextView tvUpdateTime;
            final Switch mSwitch;
            if (convertView != null) {
                holder = (Holder) convertView.getTag();
                ivIcon = holder.ivIcon;
                tvName = holder.tvName;
                mSwitch = holder.mSwitch;
                tvPackageName = holder.tvPackageName;
                tvInstallTime = holder.tvInstallTime;
                tvUpdateTime = holder.tvUpdateTime;
                view = convertView;
            } else {
                view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_app, parent, false);
                ivIcon = (ImageView) view.findViewById(R.id.ivIcon);
                tvName = (TextView) view.findViewById(R.id.tvName);
                mSwitch = (Switch) view.findViewById(R.id.sw);
                tvPackageName = (TextView) view.findViewById(R.id.tvPackageName);
                tvUpdateTime = (TextView) view.findViewById(R.id.tvUpdateTime);
                tvInstallTime = (TextView) view.findViewById(R.id.tvInstallTime);

                holder = new Holder(ivIcon, tvName, mSwitch, tvPackageName, tvInstallTime, tvUpdateTime);
                view.setTag(holder);

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
            mSwitch.setOnClickListener(v -> {
                boolean get_it = mApps.get(position).get_it;
                mApps.get(position).get_it = !get_it;
                updatePref();
            });
            DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");

            ivIcon.setImageDrawable(mApps.get(position).icon);
            tvName.setText(mApps.get(position).name);
            mSwitch.setChecked(mApps.get(position).get_it);
            tvPackageName.setText(mApps.get(position).packageName);
            tvInstallTime.setText(dateformat.format(new Date(mApps.get(position).installTime)));
            tvUpdateTime.setText(dateformat.format(new Date(mApps.get(position).updateTime)));
            return view;
        }


        class Holder {
            ImageView ivIcon;
            TextView tvName;
            Switch mSwitch;
            TextView tvPackageName;
            TextView tvInstallTime;
            TextView tvUpdateTime;

            Holder(ImageView ivIcon, TextView tvName, Switch aSwitch, TextView tvPackageName, TextView tvInstallTime, TextView tvUpdateTime) {
                this.ivIcon = ivIcon;
                this.tvName = tvName;
                this.mSwitch = aSwitch;
                this.tvPackageName = tvPackageName;
                this.tvInstallTime = tvInstallTime;
                this.tvUpdateTime = tvUpdateTime;
            }
        }
    }


    class App {
        String name;
        Drawable icon;
        boolean get_it;
        String packageName;
        boolean is_system_app;
        long updateTime;
        long installTime;

        App(String name, Drawable icon, boolean get_it, String packageName, boolean is_system_app, long updateTime, long installTime) {
            this.name = name;
            this.icon = icon;
            this.get_it = get_it;
            this.packageName = packageName;
            this.is_system_app = is_system_app;
            this.updateTime = updateTime;
            this.installTime = installTime;
        }

        @Override
        public String toString() {
            return "name:" + name + " get_it:" + get_it;
        }


    }

}
