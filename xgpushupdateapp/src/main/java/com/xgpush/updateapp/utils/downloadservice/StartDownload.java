package com.xgpush.updateapp.utils.downloadservice;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.xgpush.updateapp.R;
import com.xgpush.updateapp.utils.Lg;
import com.xgpush.updateapp.utils.downloadservice.entities.FileInfo;
import com.xgpush.updateapp.utils.downloadservice.services.DownloadService;
import com.xgpush.updateapp.utils.downloadservice.services.UpdataInfo;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.content.Context.NOTIFICATION_SERVICE;


/**
 * Copyright  2016年 珠海艾派克科技股份有限公司. All rights reserved.
 * app升级更新类
 *
 * @author wyb
 * @version 1.0
 * @since 1.0
 */
public class StartDownload {
    public final static String ACTION_START = "com.notifications.intent.action.STARTClick";
    public final static String ACTION_PAUSE = "com.notifications.intent.action.PAUSEClick";
    private static NotificationManager mNotificationManager = null;
    private static Notification mNotification;
    private final String TAG = this.getClass().getName();
    private final int UPDATA_NONEED = 0;
    private final int UPDATA_CLIENT = 1;
    private final int GET_UNDATAINFO_ERROR = 2;
    private final int DOWN_ERROR = 4;
    private UpdataInfo info;
    private String localVersion;
    private RemoteViews mRemoteViews;
    private FileInfo fileInfo;
    private Context context;
    private boolean downloadType = false;

    public StartDownload(Context context) {
        this.context = context;
    }

    public void update() {
        try {
            localVersion = getVersionName();
            Lg.i("-----------CheckVersionTask1------------");
            CheckVersionTask cv = new CheckVersionTask();
            new Thread(cv).start();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 获取版本
     *
     * @return 返回版本名
     * @throws PackageManager.NameNotFoundException
     */
    public String getVersionName() throws PackageManager.NameNotFoundException {
        //getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(),
                0);
        return packInfo.versionName;
    }

    /**
     * 服务器与app当前版本比对结果的处理进程
     */
    public class CheckVersionTask implements Runnable {
        public String UPDATE_URL = "http://119.28.68.140:8089/version.xml";
        InputStream is;

        public void run() {
            HttpURLConnection conn = null;
            try {
                String path = UPDATE_URL;
                URL url = new URL(path);
                conn = (HttpURLConnection) url
                        .openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    // 从服务器获得一个输入流
                    is = conn.getInputStream();
                }
                info = getUpdataInfo(is);
                if (info.getVersion().equals(localVersion)) {

                    Message msg = new Message();
                    msg.what = UPDATA_NONEED;
                    handler.sendMessage(msg);
                    // LoginMain();
                } else {
                    Message msg = new Message();
                    msg.what = UPDATA_CLIENT;
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                Message msg = new Message();
                msg.what = GET_UNDATAINFO_ERROR;
                handler.sendMessage(msg);
                e.printStackTrace();
            } finally {

                conn.disconnect();
            }
        }

        public UpdataInfo getUpdataInfo(InputStream is) throws Exception {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            int type = parser.getEventType();
            UpdataInfo info = new UpdataInfo();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        if ("version".equals(parser.getName())) {
                            info.setVersion(parser.nextText());
                        } else if ("url".equals(parser.getName())) {
                            info.setUrl(parser.nextText());
                        } else if ("description".equals(parser.getName())) {
                            info.setDescription(parser.nextText());
                        }
                        break;
                }
                type = parser.next();
            }
            return info;
        }
    }

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATA_NONEED:
                    Toast.makeText(context, context.getResources().getString(R.string.current_new),
                            Toast.LENGTH_SHORT).show();
                    break;
                case UPDATA_CLIENT:
                    //对话框通知用户升级程序
                    showUpdataDialog();
                    break;
                case GET_UNDATAINFO_ERROR:
                    //服务器超时
                    Toast.makeText(context, context.getResources().getString(R.string.get_server_fail), Toast.LENGTH_SHORT).show();
                    break;
                case DOWN_ERROR:
                    //下载apk失败
                    Toast.makeText(context, context.getResources().getString(R.string.down_fail), Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };

    public void updateTempRecordLabel() {
        UpdataInfo info = new UpdataInfo();
        String versionUrl = "http://fir.im/NfcTemp";
        String versionUrl2 = " http://nfc.apexmic.com/app/temprecordlabel";
        info.setUrl(versionUrl2);
        info.setDescription("update to 2.0.4");
        info.setVersion("2.0.4");
        this.info = info;
        showUpdataDialog();
    }

    /**
     * 弹出对话框通知用户更新程序
     * <p>
     * 弹出对话框的步骤：
     * 1.创建alertDialog的builder.
     * 2.要给builder设置属性, 对话框的内容,样式,按钮
     * 3.通过builder 创建一个对话框
     * 4.对话框show()出来
     */
    protected void showUpdataDialog() {
        AlertDialog.Builder builer = new AlertDialog.Builder(context);
        builer.setTitle("版本升级");
        builer.setMessage(info.getDescription());
        //当点确定按钮时从服务器上下载 新的apk 然后安装   װ
        builer.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                registerReceiver();
                Lg.i("-----------CheckVersionTask2------------");
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notificationInit();
                    }
                });


            }
        });
        builer.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                //do sth
            }
        });
        AlertDialog dialog = builer.create();
        dialog.show();
    }

    /**
     * 注册广播接收器
     */
    private void registerReceiver() {
        // 注册广播接收器
        fileInfo = new FileInfo(0, info.getUrl(), "test.apk", 0, 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE);
        filter.addAction(DownloadService.ACTION_FINISHED);
        filter.addAction(ACTION_START);
        filter.addAction(ACTION_PAUSE);
        context.registerReceiver(mReceiver, filter);

    }

    /**
     * 更新UI的广播接收器
     */
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.ACTION_UPDATE.equals(intent.getAction())) {
                int finised = intent.getIntExtra("finished", 0);
                int id = intent.getIntExtra("id", 0);
                //notificationInit();
                mRemoteViews.setTextViewText(R.id.btn_start, String.valueOf(finised) + "/100");
                mRemoteViews.setProgressBar(R.id.pb_progress, 100, finised, false);
                mNotificationManager.notify(1, mNotification);
                Log.i("mReceiver", id + "-finised = " + finised);
            } else if (DownloadService.ACTION_FINISHED.equals(intent.getAction())) {
                // 下载结束
                FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
                mRemoteViews.setTextViewText(R.id.tv_fileName, context.getString(R.string.download_finish));
                mRemoteViews.setTextViewText(R.id.btn_start, String.valueOf(100) + "/100");
                mRemoteViews.setProgressBar(R.id.pb_progress, 100, 100, false);
                mNotificationManager.notify(1, mNotification);
                installApk(new File(DownloadService.DOWNLOAD_PATH, fileInfo.getFileName()));
                context.unregisterReceiver(mReceiver);
            } else if (ACTION_START.equals(intent.getAction())) {
                Intent intent1 = new Intent(context, DownloadService.class);
                intent1.setAction(DownloadService.ACTION_START);
                intent1.putExtra("fileInfo", fileInfo);
                context.startService(intent1);
            } else if (ACTION_PAUSE.equals(intent.getAction())) {
                if (!downloadType) {
                    Intent intent2 = new Intent(context, DownloadService.class);
                    intent2.setAction(DownloadService.ACTION_STOP);
                    intent2.putExtra("fileInfo", fileInfo);
                    context.startService(intent2);
                    downloadType = true;
                } else {
                    Intent intent1 = new Intent(context, DownloadService.class);
                    intent1.setAction(DownloadService.ACTION_START);
                    intent1.putExtra("fileInfo", fileInfo);
                    context.startService(intent1);
                    downloadType = false;
                }
            }
        }
    };

    //安装apkd
    public void installApk(File file) {
        Intent intent = new Intent();
        //执行动作
        intent.setAction(Intent.ACTION_VIEW);
        //执行的数据类型
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /**
     * 通知栏
     */
    private void notificationInit() {
        //通知栏内显示下载进度条
        if (mNotificationManager != null) {
            mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            mNotificationManager.cancel(1);
            mNotificationManager = null;
            mNotification = null;
            mRemoteViews = null;
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_item);
        //点击的事件处理

        //这里加了广播，所及INTENT的必须用getBroadcast方法

        Intent buttonInten1 = new Intent(ACTION_PAUSE);
        //这里加了广播，所及INTENT的必须用getBroadcast方法
      /*  PendingIntent intent_prev1 = PendingIntent.getBroadcast(context, 1, buttonInten1, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.btn_stop, intent_prev1);*/


        mBuilder.setContent(mRemoteViews).setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(context.getString(R.string.start_update)).setOngoing(false).setPriority(Notification.PRIORITY_DEFAULT);
        mNotification = mBuilder.build();
        mNotification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(1, mNotification);

        Intent intent1 = new Intent(context, DownloadService.class);
        intent1.setAction(DownloadService.ACTION_START);
        intent1.putExtra("fileInfo", fileInfo);
        context.startService(intent1);
        Lg.i("-----------CheckVersionTask3------------");
    }
}
