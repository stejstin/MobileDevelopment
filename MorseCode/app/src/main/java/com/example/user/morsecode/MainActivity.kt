package com.example.user.morsecode

import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
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

val SAMPLE_RATE = 44100;

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
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
