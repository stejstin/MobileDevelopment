package com.example.user.morsecode

import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.Context
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
import java.util.HashMap
import java.io.InputStream

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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
