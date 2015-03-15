package android.weiweiwang.github.toolkit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by weiwei on 15/3/14.
 */
public class AppChooserListAdapter extends BaseAdapter {
    private static final String TAG = "AndroidTestToolkit.AppChooserListAdapter";

    private Context context = null;
    private List<ApplicationInfo> applicationInfoList = null;
    private PackageManager packageManager = null;
    private LayoutInflater inflater = null;
    private AlertDialog alertDialog;
    private MainActivity mainActivity;

    public AppChooserListAdapter(Context context, List<ApplicationInfo> applicationInfoList, AlertDialog alertDialog, MainActivity mainActivity) {
        this.context = context;
        this.applicationInfoList = applicationInfoList;
        this.alertDialog = alertDialog;
        this.mainActivity = mainActivity;
        inflater = LayoutInflater.from(context);
        packageManager = context.getPackageManager();
    }

    @Override
    public int getCount() {
        return applicationInfoList.size();
    }

    @Override
    public Object getItem(int i) {
        return applicationInfoList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Holder holder;
        if (view == null) {
            holder = new Holder();
            view = inflater.inflate(R.layout.app_chooser_list_view_item, null);
            holder.appIcon = (ImageView) view.findViewById(R.id.app_icon);
            holder.appPkg = (TextView) view.findViewById(R.id.app_pkg);
            holder.appChooseBtn = (Button) view.findViewById(R.id.app_choose_button);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }
        ApplicationInfo applicationInfo = applicationInfoList.get(i);
        holder.appIcon.setImageDrawable(applicationInfo.loadIcon(packageManager));
        holder.appPkg.setText(applicationInfo.loadLabel(packageManager));
        holder.appChooseBtn.setOnClickListener(new AppChooseButtonClickListner(i, applicationInfo));
//        holder.name.setText(list.get(position).name);
//        holder.birth.setText(list.get(position).birth);
//        holder.itembtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(context, list.get(position).name + "的生日是：" + list.get(position).birth, Toast.LENGTH_LONG).show();
//            }
//        });
        return view;
    }

    private class AppChooseButtonClickListner implements View.OnClickListener {
        private int position;
        private ApplicationInfo applicationInfo;

        public AppChooseButtonClickListner(int i, ApplicationInfo applicationInfo) {
            this.position = i;
            this.applicationInfo = applicationInfo;
        }

        @Override
        public void onClick(View view) {
            Logger.d(TAG, String.format("position:%s,package:%s,title:%s", position, applicationInfo.packageName, applicationInfo.loadLabel(AppChooserListAdapter.this.packageManager)));
            alertDialog.dismiss();
            mainActivity.startPerformanceLogging(applicationInfo);
        }
    }

    protected class Holder {
        ImageView appIcon;
        TextView appPkg;
        Button appChooseBtn;
    }
}
