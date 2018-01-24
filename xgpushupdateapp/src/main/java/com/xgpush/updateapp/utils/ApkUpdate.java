package com.xgpush.updateapp.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import java.io.File;

/**
 * Created by apex014417 on 2018/1/24.
 */

public class ApkUpdate {
    final Context mContext;
    DownloadManager downloadManager;
    long mTaskId;
    String apkUrl = "http://fir.im/NfcTemp";
    String versionUrl2 = "http://nfc.apexmic.com/app/temprecordlabel";
    String versionUrl3 = "http://nfc.apexmic.com/downloads/apk/temprecordlabel.apk";
    String apkName = "TempRecordLabel_2.0.4.apk";

    public ApkUpdate(Context mContext) {
        this.mContext = mContext;
        downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    void downApk(String downloadUrl, String fileName) {
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        //指定下载路径和下载文件名
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        //获取下载管理器
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        downloadManager.enqueue(request);
    }

    public void downloadTempRecordLabelApk() {
        downloadAPK(versionUrl3, apkName);
    }

    /**
     * 使用系统下载器下载
     */
    private void downloadAPK(String apkUrl, String apkName) {
        //创建下载任务
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setAllowedOverRoaming(false);
        //漫游网络是否可以下载
        request.setTitle("测试app");
        request.setDescription("正在下载...");
        request.setMimeType("application/vnd.android.package-archive");
        request.allowScanningByMediaScanner();
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        //在通知栏中显示，默认就是显示的
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(true);
        //sdcard的目录下的download文件夹，必须设置
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName);
        //request.setDestinationInExternalFilesDir(),也可以自己制定下载路径

        //将下载请求加入下载队列
        downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        //加入下载队列后会给该任务返回一个long型的id，
        //通过该id可以取消任务，重启任务等等，看上面源码中框起来的方法
        mTaskId = downloadManager.enqueue(request);
        //注册广播接收者，监听下载状态
        mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    //广播接受者，接收下载状态
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkDownloadStatus();//检查下载状态
        }
    };

    //检查下载状态
    private void checkDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(mTaskId);//筛选下载任务，传入任务ID，可变参数
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    Lg.i(">>>下载暂停");
                case DownloadManager.STATUS_PENDING:
                    Lg.i(">>>下载延迟");
                case DownloadManager.STATUS_RUNNING:
                    Lg.i(">>>正在下载");
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    Lg.i(">>>下载完成");
                    installAPK();
                    break;
                case DownloadManager.STATUS_FAILED:
                    Lg.i(">>>下载失败");
                    break;
            }
        }
    }

    /**
     * 下载到本地后执行安装
     */
    public void installAPK() {
        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + File.separator + apkName;
        File file = new File(downloadPath);
        if (!file.exists()) return;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://" + file.toString());
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        //在服务中开启activity必须设置flag,后面解释
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }
}
