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
     * @param runningAppProcessInfoList
     * @param activityManager
     * @return {"com.wandoujia.phoenix2":{"dalvikPrivateClean":0,"dalvikPrivateDirty":6180,"dalvikPss":6570,"dalvikSharedClean":0,"dalvikSharedDirty":16788,"dalvikSwappablePss":0,"dalvikSwappedOut":0,"nativePrivateClean":0,"nativePrivateDirty":10564,"nativePss":10625,"nativeSharedClean":0,"nativeSharedDirty":2344,"nativeSwappablePss":0,"nativeSwappedOut":0,"otherPrivateClean":3876,"otherPrivateDirty":2248,"otherPss":12646,"otherSharedClean":35036,"otherSharedDirty":4352,"otherSwappablePss":3872,"otherSwappedOut":0,"totalPrivateClean":3876,"totalPrivateDirty":18992,"totalPss":29841,"totalSharedClean":35036,"totalSharedDirty":23484,"totalSwappablePss":3872,"totalSwappedOut":0,"totalUss":22868,"pid":9244,"uid":10089,"process_name":"com.wandoujia.phoenix2"},"com.wandoujia.player.walkman":{"dalvikPrivateClean":0,"dalvikPrivateDirty":11304,"dalvikPss":11695,"dalvikSharedClean":0,"dalvikSharedDirty":16804,"dalvikSwappablePss":0,"dalvikSwappedOut":0,"nativePrivateClean":0,"nativePrivateDirty":9416,"nativePss":9491,"nativeSharedClean":0,"nativeSharedDirty":2360,"nativeSwappablePss":0,"nativeSwappedOut":0,"otherPrivateClean":2608,"otherPrivateDirty":3508,"otherPss":14857,"otherSharedClean":41900,"otherSharedDirty":4344,"otherSwappablePss":2560,"otherSwappedOut":0,"totalPrivateClean":2608,"totalPrivateDirty":24228,"totalPss":36043,"totalSharedClean":41900,"totalSharedDirty":23508,"totalSwappablePss":2560,"totalSwappedOut":0,"totalUss":26836,"pid":10575,"uid":10089,"process_name":"com.wandoujia.player.walkman"}}
     */
    public static ObjectNode getAppMemoryUsage(List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList, ActivityManager activityManager) {
        ObjectNode appMemoryInfoObject = new ObjectNode(CommonUtils.OBJECT_MAPPER.getNodeFactory());
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
            // 获取进程占内存用信息 kb单位
            ObjectNode processMemoryInfoObject = CommonUtils.OBJECT_MAPPER.convertValue(memoryInfo, ObjectNode.class);
            processMemoryInfoObject.put("pid", pid);
            processMemoryInfoObject.put("uid", uid);
            processMemoryInfoObject.put("process_name", processName);
            appMemoryInfoObject.set(processName, processMemoryInfoObject);
        }
        return appMemoryInfoObject;
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
     *
     * @param applicationInfo
     * @param activityManager
     * @return
     */
    public static  List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesForApplication(ApplicationInfo applicationInfo,ActivityManager activityManager) {
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

    public static List<ApplicationInfo> getRunningPackages(ActivityManager activityManager,PackageManager packageManager) {
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
