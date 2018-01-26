package com.xgpush.updateapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.xgpush.updateapp.utils.ApkUpdate;
import com.xgpush.updateapp.utils.AppInstallUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    String xgPushPackageName = "com.xgpush";
    String tempRecordLabelPackageName = "com.apex.iot.nfctemp";
    ApkUpdate apkUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        apkUpdate = new ApkUpdate(this);
    }

    void findViews() {
        findViewById(R.id.btnCheckUpdate).setOnClickListener(this);
        findViewById(R.id.btnUninstall).setOnClickListener(this);
        findViewById(R.id.btnInstall).setOnClickListener(this);
        findViewById(R.id.btnCommand).setOnClickListener(this);
        findViewById(R.id.btnCommand2).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCheckUpdate:
//                StartDownload startDownload = new StartDownload(this);
//                startDownload.updateTempRecordLabel();
                apkUpdate.downloadTempRecordLabelApk();
                break;
            case R.id.btnUninstall:
                Uri uri = Uri.fromParts("package", tempRecordLabelPackageName, null);
                Intent intent = new Intent(Intent.ACTION_DELETE, uri);
                startActivity(intent);
                break;
            case R.id.btnInstall:
                apkUpdate.installAPK();
                break;
            case R.id.btnCommand:
                apkUpdate.installSilent(this);
                break;
            case R.id.btnCommand2:
                apkUpdate.installCommand(this);
                break;
        }
    }
}
