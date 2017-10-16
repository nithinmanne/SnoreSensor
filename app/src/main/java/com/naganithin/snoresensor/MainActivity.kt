package com.naganithin.snoresensor

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.Button
import android.media.AudioRecord
import android.widget.TextView
import com.naganithin.fft.FFT


class MainActivity : AppCompatActivity() {

    private val requestCodeP = 0
    private var recording = false
    private val mSAMPLERATE = 44100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        ActivityCompat.requestPermissions(this, permissions, requestCodeP)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeP && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, permissions, requestCodeP)
    }

    private fun startRec() {
        Thread(Runnable {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)

            var bufferSize = AudioRecord.getMinBufferSize(mSAMPLERATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT)
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                bufferSize = mSAMPLERATE * 2
            }

            val record = AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                    mSAMPLERATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize)

            val audioBuffer = ShortArray(bufferSize / 2)

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                return@Runnable
            }
            record.startRecording()

            var shortsRead: Long = 0
            val fft = FFT(1024)
            var snore = 0
            val text = findViewById<TextView>(R.id.textView)

            while (recording) {
                val numberOfShort = record.read(audioBuffer, 0, audioBuffer.size)
                shortsRead += numberOfShort.toLong()

                val y = DoubleArray(1024)
                val x = audioBuffer.map { it.toDouble() }.toDoubleArray()

                fft.fft(x, y)
                val j = mPES(x)

                if (j==1) {
                    snore++
                    if (snore>5) {
                        runOnUiThread {
                            text.text = getString(R.string.yes)
                            snore = 0
                            Thread.sleep(2000)
                        }
                    }
                }
                else {
                    snore = 0
                    runOnUiThread { text.text = getString(R.string.no) }
                }
            }
            record.stop()
            record.release()
            runOnUiThread { text.text = "" }
        }).start()
    }

    private fun mPES(a: DoubleArray): Int {
        val mEL = (0..51).sumByDouble { a[it] * a[it] }
        val mEH = (53..511).sumByDouble { a[it] * a[it] }
        val pes = mEL / (mEL + mEH)
        return if (pes < 0.65)
            1
        else
            0
    }

    fun record(v: View) {
        recording = !recording
        val button = findViewById<Button>(R.id.button)
        val text = findViewById<TextView>(R.id.textView)
        if (recording) {
            button.text = getString(R.string.stop)
            text.text = getString(R.string.no)
            startRec()
        }
        else {
            button.text = getString(R.string.start)
        }
        print(v)
    }

}
