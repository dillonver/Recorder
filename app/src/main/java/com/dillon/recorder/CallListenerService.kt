package com.dillon.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaRecorder
import android.os.*
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class CallListenerService : Service() {
    //An additional thread for running tasks that shouldn't block the UI.
    private var mBackgroundThread: HandlerThread? = null

    //A [Handler] for running tasks in the background.
    private var mBackgroundHandler: Handler? = null
    private var telephonyManager: TelephonyManager? = null
    private var mediaRecorder: MediaRecorder? = null
    private var listener: MyPhoneStateListener? = null
    private var isOutGoingCall = true
    var file: File? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        telephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        listener = MyPhoneStateListener()
        telephonyManager!!.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        startBackgroundThread()
        super.onCreate()
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        startBackgroundThread()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        return super.onStartCommand(intent, flags, startId)
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val mNotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "call_01"
        val channelName = "record"
        val channelDescription = "this a record"
        val channelImportance =
                NotificationManager.IMPORTANCE_NONE
        val mChannel = NotificationChannel(channelId, channelName, channelImportance)
        mChannel.description = channelDescription
        mChannel.enableLights(false)
        mChannel.lightColor = Color.RED
        mChannel.enableVibration(false)
        mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        mNotificationManager.createNotificationChannel(mChannel)
        val notifyID = 1
        val notification = Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setChannelId(channelId)
                .build()
        startForeground(notifyID, notification)
    }

    // 监听电话呼叫状态变化
    private inner class MyPhoneStateListener : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String) {//incomingNumber对方号码
            super.onCallStateChanged(state, incomingNumber)
            try {
                when (state) {
                    TelephonyManager.CALL_STATE_IDLE//空闲状态。
                    -> if (mediaRecorder != null) {
                        mediaRecorder!!.stop()
                        mediaRecorder!!.release()
                        mediaRecorder = null
                        //提示：拨号出去的录音，是从拨号就开始录音的；而接听，是从接听开始录音
                        Log.i("CallListenerService", "mediaRecorder.stop")
                        //TODO  录制完毕，上传到服务器

                    }
                    TelephonyManager.CALL_STATE_RINGING//零响状态。
                    -> {
                        //来电会有响铃状态
                        isOutGoingCall = false
                        Log.i("CALL_STATE_RINGING", incomingNumber)
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK//通话状态
                    -> {
                        //录音
                        mediaRecorder = MediaRecorder()
                        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)//设置双向录音
                        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
                        val time = System.currentTimeMillis()
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        val date = Date(time)
                        val time1 = format.format(date)

                        val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.toString() + "/record")
                        if (!dir.exists()) {
                            dir.mkdir()
                        }
                        if (isOutGoingCall) {
                            Log.i("CallListenerService", "拨出电话：$incomingNumber")
                        } else {
                            Log.i("CallListenerService", "接听电话：$incomingNumber")
                        }
                        file = File(dir.absolutePath, "$incomingNumber-$time1.m4a")
                        isOutGoingCall = true

                        mediaRecorder!!.setOutputFile(file!!.absolutePath)
                        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                        mediaRecorder!!.prepare()
                        mediaRecorder!!.start()
                        Log.i("CallListenerService", "mediaRecorder.start")
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    //Starts a background thread and its [Handler].
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CallBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    // Stops the background thread and its [Handler].
    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            Log.i(this.toString(), e.toString())
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopBackgroundThread()
        listener = null
    }


}