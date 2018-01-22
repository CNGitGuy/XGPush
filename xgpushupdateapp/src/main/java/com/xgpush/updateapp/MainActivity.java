package com.xgpush.updateapp;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.xgpush.updateapp.utils.downloadservice.StartDownload;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
    }

    void findViews() {
        findViewById(R.id.btnCheckUpdate).setOnClickListener(this);
        findViewById(R.id.btnUninstall).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCheckUpdate:
                StartDownload startDownload = new StartDownload(this);
                startDownload.update();
                break;
            case R.id.btnUninstall:
                Uri uri = Uri.fromParts("package", "com.xgpush", null);
                Intent intent = new Intent(Intent.ACTION_DELETE, uri);
                startActivity(intent);
                break;
        }
    }
}
