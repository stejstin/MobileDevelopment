package com.example.user.morsecode

import android.Manifest.permission.SEND_SMS
import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.X
import android.view.inputmethod.InputMethodManager
import com.example.user.morsecode.R.id.mTextView

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.lang.Math.round
import java.util.*
import kotlin.concurrent.timerTask
import android.widget.Toast
import java.nio.file.Files.size
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import java.util.jar.Manifest


val SAMPLE_RATE = 44100;

class MainActivity : AppCompatActivity() {
    var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getDefaultSharedPreferences(this.applicationContext)


        val morsePitch = prefs!!.getString("morse_pitch", "550").toInt()
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            sendSMS("7312342862", mTextView.text.toString())
        }
        mTextView.movementMethod = ScrollingMovementMethod()

        testButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString())
            hideKeyboard()
        }

        val json =loadJSONFromAsset()
        buildDicts(json)

        ShowCodes.setOnClickListener {
            showcodes()
        }
        tbutton.setOnClickListener{ view ->
            translate()
        }
        playButton.setOnClickListener{ view ->
                playString(mTextView.text.toString())

        }



    }


    private fun sendSMS(phoneNumber:String, message:String) {
        val sentPendingIntents = ArrayList<PendingIntent>()
        val deliveredPendingIntents = ArrayList<PendingIntent>()

        val sentPI = PendingIntent.getBroadcast( this, 0,
                Intent(this, SmsSentReceiver::class.java), 0)
        val deliveredPI = PendingIntent.getBroadcast(this , 0,
                Intent(this , SmsDeliveredReceiver::class.java), 0)

        val PERMISSION_REQUEST_CODE = 1
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        {
            if ((checkSelfPermission(SEND_SMS) === PackageManager.PERMISSION_DENIED))
            {
                Log.d("permission", "permission denied to SEND_SMS - requesting it")
                val permissions = arrayOf<String>(SEND_SMS)
                requestPermissions(permissions, PERMISSION_REQUEST_CODE)
            }
        }


        try{
            val sms = SmsManager.getDefault()
            val mSMSMessage = sms.divideMessage(message)
            for (i in 0 until mSMSMessage.size)
            {
                sentPendingIntents.add(i, sentPI)
                deliveredPendingIntents.add(i, deliveredPI)
            }
            sms.sendMultipartTextMessage(phoneNumber, null, mSMSMessage,
                    sentPendingIntents, deliveredPendingIntents)
        }
        catch (e:Exception) {
            e.printStackTrace()
            Toast.makeText(getBaseContext(), "SMS sending failed...", Toast.LENGTH_SHORT).show()
        }
    }

    class SmsDeliveredReceiver:BroadcastReceiver() {
        override fun onReceive(context:Context, arg1:Intent) {
            when (getResultCode()) {
                Activity.RESULT_OK -> Toast.makeText(context, "SMS delivered", Toast.LENGTH_SHORT).show()
                Activity.RESULT_CANCELED -> Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class SmsSentReceiver:BroadcastReceiver() {
        override fun onReceive(context:Context, arg1:Intent) {
            when (getResultCode()) {
                Activity.RESULT_OK -> Toast.makeText(context, "SMS Sent", Toast.LENGTH_SHORT).show()
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Toast.makeText(context, "SMS generic failure", Toast.LENGTH_SHORT)
                        .show()
                SmsManager.RESULT_ERROR_NO_SERVICE -> Toast.makeText(context, "SMS no service", Toast.LENGTH_SHORT)
                        .show()
                SmsManager.RESULT_ERROR_NULL_PDU -> Toast.makeText(context, "SMS null PDU", Toast.LENGTH_SHORT).show()
                SmsManager.RESULT_ERROR_RADIO_OFF -> Toast.makeText(context, "SMS radio off", Toast.LENGTH_SHORT).show()
            }
        }
    }



    fun Activity.hideKeyboard() {
        hideKeyboard(if (currentFocus == null) View( this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun appendTextAndScroll(text: String) {
        if (mTextView != null) {
            mTextView.append(text + "\n")
            val layout = mTextView.layout
            if(layout != null){
                val scrollDelta = (layout.getLineBottom( mTextView.lineCount -1)
                - mTextView.scrollY - mTextView.height)
                if (scrollDelta > 0)
                    mTextView.scrollBy( 0, scrollDelta)
            }
        }
    }


    fun loadJSONFromAsset(): JSONObject{
        val filename = "morse.json"
        val json_string = application.assets.open(filename).bufferedReader().use{
            it.readText()
        }
        val json_object = JSONObject(json_string.substring(json_string.indexOf("{"), json_string.lastIndexOf( "}")+1))
        return json_object
    }


    var letToCodeDict: HashMap<String,String> = HashMap()
    var codeToLetDict: HashMap<String,String> = HashMap()

    fun buildDicts(json_object: JSONObject){
        for(k in json_object.keys()) {
            var code = json_object[k].toString()

            letToCodeDict.set(k, code)
            codeToLetDict.set(code, k)

            Log.d("log", "$k: $code")
        }
    }

    fun showcodes(){
        appendTextAndScroll("HERE ARE THE CODES")
        for(k in letToCodeDict.keys.sorted())
            appendTextAndScroll("$k: ${letToCodeDict[k]}")
    }

    fun translate() {
        var r = " "
        var s = inputText.text.toString()
        var t = " "

        s = s.toLowerCase()
            for (c in s) {
                    if (c.toString() == " ")
                        r += "/"
                    else if (c.toString() in letToCodeDict) {
                        r += " " + letToCodeDict[c.toString()]
                        t= c.toString()

                    } else
                        r += "?"
            }
        if(!codeToLetDict.contains(t))
            appendTextAndScroll(r)
        else
            appendTextAndScroll(morsetolet(s))
    }

    fun morsetolet(sentence:String):String{
        var r = " "
        var words = sentence.split(" ")

        for(letter in words){
            if(letter == "/")
                r+=" "
            else if (letter in codeToLetDict){
                r+=codeToLetDict[letter]
            }

        }
        return r
    }

    fun playString(s:String, i: Int=0) : Unit{


        if (i>s.length-1)
            return;

        var mDelay: Long = 0;


        var thenFun: () -> Unit = { ->
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                playString(s, i+1)
            })

        }

        var c = s[i]
        Log.d("log", "processing pos: " + i + " char: [" + c + "]")
        if ( c=='.')
            playDot(thenFun)
        else if(c =='-')
            playDash(thenFun)
        else if(c =='/')
            pause(6*dotLength, thenFun)
        else if (c==' ')
            pause(2*dotLength,thenFun)

    }

    val dotLength:Int = 50
    val dashLength:Int = dotLength * 3

    val dotSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dotLength)
    val dashSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dashLength)


    fun playDash(onDone: () -> Unit ={    }){
        Log.d("DEBUG", "playDash")
        playSoundBuffer(dashSoundBuffer, { -> pause(dotLength,onDone)})
    }

    fun playDot(onDone : () -> Unit = {   }){
        Log.d("debug", "playDot")
        playSoundBuffer(dotSoundBuffer, {-> pause(dotLength, onDone)})
    }

    fun pause(durationMSec: Int, onDone : () -> Unit = {   }){
        Log.d("DEBUG", "pause" + durationMSec)
        Timer().schedule( timerTask{
            onDone()
        }, durationMSec.toLong())
    }


    private fun genSineWaveSoundBuffer(frequency: Double, durationMSec: Int) : ShortArray{
        val duration: Int = round((durationMSec / 1000.0) * SAMPLE_RATE).toInt()

        var mSound: Double
        val mBuffer = ShortArray(duration)
        for( i in 0 until duration) {
            mSound = Math.sin(2.0 * Math.PI * i.toDouble() / (SAMPLE_RATE / frequency))
            mBuffer[i] = (mSound * java.lang.Short.MAX_VALUE).toShort()
        }
        return mBuffer
    }

    private fun playSoundBuffer(mBuffer:ShortArray, onDone : () -> Unit = {   }){
        var minBufferSize = SAMPLE_RATE/10
        if(minBufferSize < mBuffer.size){
            minBufferSize = minBufferSize + minBufferSize *
                    (Math.round(mBuffer.size.toFloat())/ minBufferSize.toFloat() ).toInt()
        }

        val nBuffer = ShortArray(minBufferSize)
        for(i in nBuffer.indices){
            if (i < mBuffer.size) nBuffer[i] = mBuffer[i]
            else nBuffer[i] = 0;
        }

        val mAudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM)

        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume())
        mAudioTrack.setNotificationMarkerPosition(mBuffer.size)
        mAudioTrack.setPlaybackPositionUpdateListener(object: AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onPeriodicNotification(track : AudioTrack){

            }
            override fun onMarkerReached(track : AudioTrack){
                Log.d("log", "Audio track end of file reached..")
                mAudioTrack.stop(); mAudioTrack.release(); onDone();
            }
        })
        mAudioTrack.play(); mAudioTrack.write(nBuffer, 0, minBufferSize)
    }




    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent( this, SettingsActivity::class.java)
                startActivity(intent)

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
