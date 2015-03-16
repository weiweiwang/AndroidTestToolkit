package android.weiweiwang.github.toolkit;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.weiweiwang.github.toolkit.utils.CommonUtils;
import android.weiweiwang.github.toolkit.utils.ICCID;
import android.weiweiwang.github.toolkit.utils.PerformanceMonitorUtils;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String TAG = "AndroidTestToolkit.MainActivity";
    /**
     * sd卡cache的根目录
     */
    public static final File SDCARD_DIR = Environment.getExternalStorageDirectory();


    private static final String MONITOR_STATUS_IN_PROGRESS = "started";


    /**
     * 图片cache的目录
     */
    public static final File APP_DATA_BASE_DIR = new File(SDCARD_DIR, ".AndroidTestToolkit");

    private TextView textViewInformation;
    private Button btnPerformanceMonitor;
    private ActivityManager mActivityManager = null;
    private PackageManager packageManager;
    private Timer timer = new Timer();
    private Writer performanceLogWriter;


    private final static int DO_UPDATE_TEXT = 0;
    private final static int DO_APPEND_TEXT = 1;
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 5, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());


    private Map<String, String> codeDistrictMap = new HashMap<String, String>();
    private Map<String, String> macVendorMap = new HashMap<String, String>();

    private final Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            final int what = msg.what;
            switch (what) {
                case DO_UPDATE_TEXT:
                    updateText(msg.getData().getString("text"));
                    break;
                case DO_APPEND_TEXT:
                    appendText(msg.getData().getString("text"));
                    break;
            }
        }
    };

    private void updateText(String text) {
        textViewInformation.setText(text);
    }

    private void appendText(String text) {
        textViewInformation.append(text);
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        textViewInformation = (TextView) findViewById(R.id.tv_information);
        btnPerformanceMonitor = (Button) findViewById(R.id.btn_performanceMonitor);
        codeDistrictMap = new HashMap<String, String>();
        threadPoolExecutor.submit(new Runnable() {
            public void run() {
                codeDistrictMap.putAll(loadFixedPhone());
                macVendorMap.putAll(loadMacVendor());
            }
        });
        //获得ActivityManager服务的对象
        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        packageManager = getPackageManager();
        textViewInformation.setMovementMethod(new ScrollingMovementMethod());
    }


    public void btnGuessWhereAmIClick(View view) {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();       //取出IMEI
        String tel = tm.getLine1Number();     //取出MSISDN，很可能为空
        String countryIso = tm.getSimCountryIso();
        int simState = tm.getSimState();
        List<CellInfo> allCellInfo = tm.getAllCellInfo();
        CellLocation cellLocation = tm.getCellLocation();
        String iccid = tm.getSimSerialNumber();  //取出ICCID
        String imsi = tm.getSubscriberId();     //取出IMSI
        int phoneType = tm.getPhoneType();
        int networkType = tm.getNetworkType();
//        String networkCountryIso = tm.getNetworkCountryIso();
        String networkOperator = tm.getNetworkOperator();
        String networkOperatorName = tm.getNetworkOperatorName();
        if (TextUtils.isEmpty(iccid)) {
            textViewInformation.append("Sim not found\n");
        } else {
            ICCID iccidInfo = new ICCID(iccid, codeDistrictMap);
            textViewInformation.append(String.format("imei:%s\ntel:%s\niccid:%s\nimsi:%s\noperator:%s\nprovince:%s\n", imei, tel, iccid, imsi, networkOperator + "->" + iccidInfo.getOperatorName(), iccidInfo.getProvinceName()));
        }
    }

    private byte[] convertIpAddress(int ip) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (ip & 0xFF);
        bytes[1] = (byte) ((ip >> 8) & 0xFF);
        bytes[2] = (byte) ((ip >> 16) & 0xFF);
        bytes[3] = (byte) ((ip >> 24) & 0xFF);
        return bytes;
    }

    private String getVendor(String mac) {
        String vendor = "";
        String prefix = mac.substring(0, 8);
        if (macVendorMap.containsKey(prefix)) {
            vendor = macVendorMap.get(prefix);
        }
        return vendor;
    }


    public void btnScanNetworkClick(View view) {
        threadPoolExecutor.submit(new ScanNetworkTask());
    }


    private class ScanNetworkTask implements Runnable {
        public void run() {
            try {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                DhcpInfo dhcp = wifiManager.getDhcpInfo();
                byte[] gateway = convertIpAddress(dhcp.gateway);
                byte[] mask = convertIpAddress(dhcp.netmask);
                byte[] dhcpServer = convertIpAddress(dhcp.serverAddress);
                byte[] dns1 = convertIpAddress(dhcp.dns1);
                byte[] dns2 = convertIpAddress(dhcp.dns2);
                int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
                byte[] quads = new byte[4];
                for (int k = 0; k < 4; k++) {
                    quads[k] = (byte) (broadcast >> (k * 8));
                }
                InetAddress broadcastAddress = InetAddress.getByAddress(quads);
                InetAddress gatewayAddress = InetAddress.getByAddress(gateway);
                InetAddress dhcpServerAddress = InetAddress.getByAddress(dhcpServer);
                InetAddress maskAddress = InetAddress.getByAddress(mask);
                InetAddress dns1Address = InetAddress.getByAddress(dns1);
                InetAddress dns2Address = InetAddress.getByAddress(dns2);
                String routerMacAddress = getMacAddress(gatewayAddress.getHostAddress());
                String routerVendor = getVendor(routerMacAddress);
                int ip = wifiInfo.getIpAddress();
                String wifiSSID = wifiInfo.getSSID();
                String wifiBSSID = wifiInfo.getBSSID();
                String wifiMac = wifiInfo.getMacAddress().toUpperCase();
                int rssi = wifiInfo.getRssi();
                int linkSpeed = wifiInfo.getLinkSpeed();
                String text = String.format("BSSID:%s\nSSID:%s\nMAC:%s\nGateway:%s\nBroadcast:%s\nMask:%s\nDHCP:%s\nDNS:%s %s\nRouter:%s\nSpeed:%d%s\n", wifiBSSID, wifiSSID, wifiMac, gatewayAddress.getHostAddress(), broadcastAddress.getHostAddress(), maskAddress.getHostAddress(), dhcpServerAddress.getHostAddress(), dns1Address.getHostAddress(), dns2Address.getHostAddress(), routerVendor, linkSpeed, WifiInfo.LINK_SPEED_UNITS);
                sendAppendTextMsg(text);

                byte[] bytes = convertIpAddress(ip);
                int count = 0;
                long start = System.currentTimeMillis();
                for (int i = 1; i < 255; i++) {
                    bytes[3] = (byte) i;
                    InetAddress address = InetAddress.getByAddress(bytes);
                    Process process = Runtime.getRuntime().exec("ping -c 1 -W 3 " + address.getHostAddress());
                    Logger.d(TAG, "processed:" + address.getHostAddress());
                    String macAddress = getMacAddress(address.getHostAddress());
                    if (null != macAddress && !"00:00:00:00:00:00".equals(macAddress)) {
                        count += 1;
                        sendAppendTextMsg(String.format("IP:%s MAC:%s Vendor:%s Cur:%d\n", address.getHostAddress(), macAddress, getVendor(macAddress), count));
                    }
                }
                long end = System.currentTimeMillis();
                sendAppendTextMsg(String.format("time used:%.2f seconds\n", (end - start) / 1000.0));
            } catch (Exception e) {
                sendAppendTextMsg(e.toString() + "\n");
                Logger.w(TAG, e.toString(), e);
            }
        }
    }

    private void sendAppendTextMsg(String text) {
        Message msg = new Message();
        msg.what = DO_APPEND_TEXT;
        Bundle bundle = new Bundle();
        bundle.putString("text", text);
        msg.setData(bundle);
        myHandler.sendMessage(msg);
    }

    private String getMacAddress(String ip) {
        try {
            BufferedReader br1 = new BufferedReader(new FileReader("/proc/net/arp"));
            String str2;
            while ((str2 = br1.readLine()) != null) {
                String[] splits = str2.split("\\s+");
                if (splits.length >= 4 && splits[0].equals(ip)) {
                    return splits[3].toUpperCase();
                }
            }
        } catch (Exception e) {
            Logger.w(TAG, e.toString(), e);
        }
        return null;
    }

    private Map<String, String> loadMacVendor() {
        BufferedReader reader = null;
        Map<String, String> macVendorMap = new HashMap<String, String>();
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("mac.db")));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] splits = line.trim().split("\t");
                if (splits.length > 1) {
                    macVendorMap.put(splits[0], splits[1]);
                }
            }
            Logger.d(TAG, "loaded " + macVendorMap.size() + " mac vendors");
        } catch (Exception e) {
            Logger.d(TAG, e.toString(), e);
        } finally {
            CommonUtils.closeQuietly(reader);
        }
        return macVendorMap;
    }

    private Map<String, String> loadFixedPhone() {
        BufferedReader reader = null;
        Map<String, String> codeDistrictMap = new HashMap<String, String>();
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("fixed_phone")));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] splits = line.trim().split("\\|");
                if (null != splits && splits.length > 1) {
                    codeDistrictMap.put(splits[0], splits[1]);
                }
            }
        } catch (Exception e) {
            Logger.d(TAG, e.toString(), e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Logger.d(TAG, e.toString(), e);
                e.printStackTrace();
            }
        }
        return codeDistrictMap;
    }

    /**
     * 读取配置文件random_dirs.conf,这个文件中每行一个路径，程序会在每个路径下面写入一个随机文件
     *
     * @param view
     */
    public void btnRandomWriteSDCardClick(View view) {
        BufferedReader reader = null;
        try {
            if (!APP_DATA_BASE_DIR.exists()) {
                APP_DATA_BASE_DIR.mkdirs();
            }
            File randomDirectoryConfFile = new File(APP_DATA_BASE_DIR, "random_dirs.conf");
            if (!randomDirectoryConfFile.exists()) {
                randomDirectoryConfFile.createNewFile();
            }
            BufferedReader randomContentReader = new BufferedReader(new InputStreamReader(getAssets().open("random_content.txt")));
            StringBuilder randomContent = new StringBuilder();
            String line = null;
            while ((line = randomContentReader.readLine()) != null) {
                randomContent.append(line + System.getProperty("line.separator"));
            }
            randomContentReader.close();

            reader = new BufferedReader(new FileReader(randomDirectoryConfFile));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim().replaceAll("\\s", "");
                if (!line.isEmpty()) {
                    File dir = new File(SDCARD_DIR, line);
                    boolean success = dir.mkdirs();
                    if (success) {
                        File randomFile = File.createTempFile("random_", ".txt", dir);
                        BufferedWriter writer = new BufferedWriter(new FileWriter(randomFile));
                        writer.write(randomContent.toString());
                        writer.close();
                        count++;
                    } else {
                        Logger.d(TAG, "failed to create dirs:" + dir.getAbsolutePath());
                    }
                }
            }
            textViewInformation.append(String.format("Done created %d directories!\n", count));
        } catch (Exception e) {
            Logger.d(TAG, e.toString(), e);
            textViewInformation.append(e.toString() + "\n");
        } finally {
            CommonUtils.closeQuietly(reader);
        }
    }


    public void clearOutput(View view) {
        textViewInformation.setText("");
    }


    public void startPerformanceLogging(ApplicationInfo applicationInfo) {
        try {
            if (!APP_DATA_BASE_DIR.exists()) {
                APP_DATA_BASE_DIR.mkdirs();
            }

            File performanceLogFile = File.createTempFile(applicationInfo.packageName + "_performance_log_", ".log", APP_DATA_BASE_DIR);
            performanceLogWriter = new BufferedWriter(new FileWriter(performanceLogFile));

            timer.scheduleAtFixedRate(new PerformanceMonitorTask(applicationInfo, performanceLogWriter), 0, 10000);
            btnPerformanceMonitor.setText(R.string.performanceMonitorInProgress);
            btnPerformanceMonitor.setTag(MONITOR_STATUS_IN_PROGRESS);
            textViewInformation.append(String.format("performance monitoring started, performance log file:%s\n", performanceLogFile.getAbsolutePath()));
        } catch (Exception e) {
            Logger.w(TAG, "failed to create performance log writer", e);
            textViewInformation.append("failed to start performance monitor\n");
        }
    }

    public void stopPerformanceMonitor() {
        timer.cancel();
        timer = new Timer();
        textViewInformation.append("performance monitoring stopped\n");
        CommonUtils.closeQuietly(performanceLogWriter);
        btnPerformanceMonitor.setText(R.string.performanceMonitor);
        btnPerformanceMonitor.setTag(null);
    }


    public void btnPerformanceMonitorClick(View view) {
        String tag = (String) view.getTag();
        if (MONITOR_STATUS_IN_PROGRESS.equals(tag)) {
            stopPerformanceMonitor();
            return;
        }
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.app_chooser, (ViewGroup) findViewById(R.id.app_chooser_dialog));
        ListView appChooserListView = (ListView) layout.findViewById(R.id.app_chooser_list_view);
        List<ApplicationInfo> runningPackages = PerformanceMonitorUtils.getRunningPackages(mActivityManager, packageManager);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout).setTitle(R.string.chooseAppDialogTitle).setCancelable(true);
        AlertDialog alertDialog = builder.create();


        AppChooserListAdapter adapter = new AppChooserListAdapter(getApplicationContext(), runningPackages, alertDialog, this);
        appChooserListView.setAdapter(adapter);

        alertDialog.show();
    }


    private class PerformanceMonitorTask extends TimerTask {
        private ApplicationInfo applicationInfo;
        private long previousSystemCpuTotal;
        private long previousAppCpuTotal;
        private long previousSystemCpuIdle;
        private Writer writer;
        private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());


        public PerformanceMonitorTask(ApplicationInfo applicationInfo, Writer writer) {
            this.applicationInfo = applicationInfo;
            this.writer = writer;
        }

        /*
        http://kongqingyun123.blog.163.com/blog/static/6377283520126974730476/
        CPU总使用率（%） =  100*((totalCputime2- totalCputime1)-(idle2-idle1))/(totalCputime2-totalCputime1)
单个程序的CPU使用率（%） = 100*(processCpuTime2-processCpuTime1)/(totalCpuTime2-totalCpuTime1)
         */
        @Override
        public void run() {
            List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = PerformanceMonitorUtils.getRunningAppProcessesForApplication(applicationInfo, mActivityManager);
            ObjectNode appMemoryInfoObject = PerformanceMonitorUtils.getAppMemoryUsage(runningAppProcessInfoList, mActivityManager);
            ObjectNode appCpuInfoObject = PerformanceMonitorUtils.getAppCpuUsage(runningAppProcessInfoList);
            ObjectNode monitorInfoObject = new ObjectNode(CommonUtils.OBJECT_MAPPER.getNodeFactory());

            monitorInfoObject.set("cpu", appCpuInfoObject);
            monitorInfoObject.set("memory", appMemoryInfoObject);

            long currentAppMemoryUsage = appMemoryInfoObject.path("total").longValue();//KB
            long currentSystemCpuTotal = appCpuInfoObject.path("system").path("total").longValue();
            long currentAppCpuTotal = appCpuInfoObject.path("processes").path("total").longValue();
            long currentSystemCpuIdle = appCpuInfoObject.path("system").path("idle").longValue();

            double systemCpuUsage = 100.0 * ((currentSystemCpuTotal - currentSystemCpuIdle) - (previousSystemCpuTotal - previousSystemCpuIdle))
                    / (currentSystemCpuTotal - previousSystemCpuTotal);
            double appCpuUsage = 100.0 * (currentAppCpuTotal - previousAppCpuTotal) / (currentSystemCpuTotal - previousSystemCpuTotal);
            ((ObjectNode) appCpuInfoObject.path("system")).put("usage", systemCpuUsage);
            ((ObjectNode) appCpuInfoObject.path("processes")).put("usage", appCpuUsage);

            previousAppCpuTotal = currentAppCpuTotal;
            previousSystemCpuTotal = currentSystemCpuTotal;
            previousSystemCpuIdle = currentSystemCpuIdle;
            //TODO add traffic and battery usage
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(applicationInfo.packageName, 0);
                String json = monitorInfoObject.toString();
                String dateTimeInString = simpleDateFormat.format(new Date());
                String logString = String.format("%s\t%s\t%s\t%s\t%s\t%f\t%f\t%d", dateTimeInString, packageInfo.packageName, packageInfo.versionName, packageInfo.versionCode, json, systemCpuUsage, appCpuUsage,currentAppMemoryUsage);
                Logger.d(TAG, logString);
                writer.write(logString + "\n");
                writer.flush();
                sendAppendTextMsg(logString + "\n");
            } catch (Exception e) {
                Logger.w(TAG, "failed to monitoring performance", e);
                sendAppendTextMsg("failed to monitoring performance,please restart\n");
            }
        }
    }

}
