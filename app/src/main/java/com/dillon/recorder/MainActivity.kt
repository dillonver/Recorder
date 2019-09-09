package com.dillon.recorder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    //测试手机：三星S9，Android9.0系统  双向录音通过
    //注意，此demo没有做权限申请，请自行打开权限
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val i = Intent(this, RecordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i)
        } else {
            startService(i)
        }
        Toast.makeText(this, "Start service!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
