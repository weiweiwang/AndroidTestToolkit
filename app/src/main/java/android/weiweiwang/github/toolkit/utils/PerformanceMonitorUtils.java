package android.weiweiwang.github.toolkit.utils;

import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.weiweiwang.github.toolkit.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by weiwei on 15/3/15.
 */
public class PerformanceMonitorUtils {
    private static final String TAG = "AndroidTestToolkit.PerformanceMonitorUtils";

    /**
     * 需要注意的是这里面内存的单位有区别，在使用的时候需要注意
     *
     * @param runningAppProcessInfoList
     * @param activityManager
     * @return
     */
    public static ObjectNode getAppMemoryUsage(List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList, ActivityManager activityManager) {
        ObjectNode appMemoryUsageObject = new ObjectNode(CommonUtils.OBJECT_MAPPER.getNodeFactory());
        ObjectNode appProcessMemoryUsage = new ObjectNode(CommonUtils.OBJECT_MAPPER.getNodeFactory());
        long total = 0;
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcessInfoList) {
            int pid = runningAppProcessInfo.pid;
            // 用户ID 类似于Linux的权限不同，ID也就不同 比如 root等
            int uid = runningAppProcessInfo.uid;
            // 进程名，默认是包名或者由属性android：process=""指定
            String processName = runningAppProcessInfo.processName;
            // 获得该进程占用的内存
            int[] myMempid = new int[]{pid};
            // 此MemoryInfo位于android.os.Debug.MemoryInfo包中，用来统计进程的内存信息
            Debug.MemoryInfo memoryInfo = activityManager
                    .getProcessMemoryInfo(myMempid)[0];
            total += memoryInfo.getTotalPss();
            // 获取进程占内存用信息 kb单位
            ObjectNode processMemoryInfoObject = CommonUtils.OBJECT_MAPPER.convertValue(memoryInfo, ObjectNode.class);
            processMemoryInfoObject.put("pid", pid);
            processMemoryInfoObject.put("uid", uid);
            processMemoryInfoObject.put("process_name", processName);
            appProcessMemoryUsage.set(processName, processMemoryInfoObject);
        }

        ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(outInfo);
        appMemoryUsageObject.put("total", total);
        appMemoryUsageObject.set("processes", appProcessMemoryUsage);
        appMemoryUsageObject.set("system", CommonUtils.OBJECT_MAPPER.convertValue(outInfo, ObjectNode.class));
        return appMemoryUsageObject;
    }

    /**
     * @param runningAppProcessInfo
     * @return {"utime":410,"stime":126,"cutime":2,"cstime":6,"total":536,"pid":9244,"uid":10089,"process_name":"com.wandoujia.phoenix2"}
     */
    public static ObjectNode getProcessCpuUsage(ActivityManager.RunningAppProcessInfo runningAppProcessInfo) {
        ObjectNode cpuInfo = new ObjectNode(CommonUtils.OBJECT_MAPPER.getNodeFactory());
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + runningAppProcessInfo.pid + "/stat"));
            String line = reader.readLine();
            String[] splits = line.split("\\s+");
            //http://kongqingyun123.blog.163.com/blog/static/6377283520126974730476/
            //cpu  309795 17855 263367 2993556 26762 36 3184 0 0 0
            cpuInfo.put("utime", Long.parseLong(splits[13]));
            cpuInfo.put("stime", Long.parseLong(splits[14]));
            cpuInfo.put("cutime", Long.parseLong(splits[15]));
            cpuInfo.put("cstime", Long.parseLong(splits[16]));
            cpuInfo.put("pid", runningAppProcessInfo.pid);
            cpuInfo.put("uid", runningAppProcessInfo.uid);
            cpuInfo.put("process_name", runningAppProcessInfo.processName);
            cpuInfo.put("total", cpuInfo.path("stime").longValue() + cpuInfo.path("utime").longValue());
        } catch (Exception e) {
            Logger.w(TAG, "failed to get cpu info", e);
        } finally {
            CommonUtils.closeQuietly(reader);
        }
        return cpuInfo;
    }

    /**
     * @return {"user":388328,"nice":20878,"system":324931,"idle":3621991,"iowait":29827,"irq":41,"softirq":3558,"total":4389554,"usage":10.51980198019802}
     */
    public static ObjectNode getSystemCpuUsage() {
        ObjectNode cpuInfo = new ObjectNode(CommonUtils.OBJECT_MAPPER.getNodeFactory());
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/stat"));
            String line = reader.readLine();
            String[] splits = line.split("\\s+");
            //http://kongqingyun123.blog.163.com/blog/static/6377283520126974730476/
            //cpu  309795 17855 263367 2993556 26762 36 3184 0 0 0
            cpuInfo.put("user", Long.parseLong(splits[1]));
            cpuInfo.put("nice", Long.parseLong(splits[2]));
            cpuInfo.put("system", Long.parseLong(splits[3]));
            cpuInfo.put("idle", Long.parseLong(splits[4]));
            cpuInfo.put("iowait", Long.parseLong(splits[5]));
            cpuInfo.put("irq", Long.parseLong(splits[6]));
            cpuInfo.put("softirq", Long.parseLong(splits[7]));
            cpuInfo.put("total", cpuInfo.path("user").longValue() + cpuInfo.path("nice").longValue()
                    + cpuInfo.path("system").longValue() + cpuInfo.path("idle").longValue() + cpuInfo.path("iowait").longValue()
                    + cpuInfo.path("irq").longValue() + cpuInfo.path("softirq").longValue());
        } catch (Exception e) {
            Logger.w(TAG, "failed to get cpu info", e);
        } finally {
            CommonUtils.closeQuietly(reader);
        }
        return cpuInfo;
    }

    /**
     * @param runningAppProcessInfoList
     * @return {"processes":{"com.wandoujia.phoenix2":{"utime":410,"stime":126,"cutime":2,"cstime":6,"total":536,"pid":9244,"uid":10089,"process_name":"com.wandoujia.phoenix2"},"com.wandoujia.player.walkman":{"utime":4984,"stime":3627,"cutime":3,"cstime":5,"total":8611,"pid":10575,"uid":10089,"process_name":"com.wandoujia.player.walkman"},"total":9147,"usage":0.3094059405940594},"system":{"user":388328,"nice":20878,"system":324931,"idle":3621991,"iowait":29827,"irq":41,"softirq":3558,"total":4389554,"usage":10.51980198019802}}
     */
    public static ObjectNode getAppCpuUsage(List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList) {
        ObjectNode appCpuInfoObject = new ObjectNode(CommonUtils.OBJECT_MAPPER.getNodeFactory());
        ObjectNode appProcessCpuUsage = new ObjectNode(CommonUtils.OBJECT_MAPPER.getNodeFactory());
        long total = 0;
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcessInfoList) {
            int pid = runningAppProcessInfo.pid;
            // 用户ID 类似于Linux的权限不同，ID也就不同 比如 root等
            int uid = runningAppProcessInfo.uid;
            // 进程名，默认是包名或者由属性android：process=""指定
            String processName = runningAppProcessInfo.processName;
            // 获得该进程占用的内存
            ObjectNode processCpuUsage = getProcessCpuUsage(runningAppProcessInfo);
            total += processCpuUsage.path("total").longValue();
            appProcessCpuUsage.set(processName, processCpuUsage);
        }
        appProcessCpuUsage.put("total", total);
        appCpuInfoObject.set("processes", appProcessCpuUsage);
        appCpuInfoObject.set("system", getSystemCpuUsage());
        return appCpuInfoObject;
    }

    /**
     * @param applicationInfo
     * @param activityManager
     * @return
     */
    public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesForApplication(ApplicationInfo applicationInfo, ActivityManager activityManager) {
        List<ActivityManager.RunningAppProcessInfo> result = new ArrayList<ActivityManager.RunningAppProcessInfo>();
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcessInfoList) {
            Set<String> set = new HashSet<String>(Arrays.asList(runningAppProcessInfo.pkgList));
            if (set.contains(applicationInfo.packageName)) {
                result.add(runningAppProcessInfo);
            }
        }
        return result;
    }

    public static List<ApplicationInfo> getRunningPackages(ActivityManager activityManager, PackageManager packageManager) {
        List<ActivityManager.RunningAppProcessInfo> appProcessList = activityManager
                .getRunningAppProcesses();
        Set<String> runningPackages = new HashSet<String>();
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : appProcessList) {
            String[] pkgList = runningAppProcessInfo.pkgList;
            runningPackages.addAll(Arrays.asList(pkgList));
        }
        List<ApplicationInfo> list = new ArrayList<ApplicationInfo>(runningPackages.size());
        for (String pkg : runningPackages) {
            try {
                ApplicationInfo applicationInfo = null;
                applicationInfo = packageManager.getApplicationInfo(pkg, 0);
                if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    list.add(applicationInfo);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Logger.w(TAG, "not found:" + pkg, e);
            }
        }
        return list;
    }
}
