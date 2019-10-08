package com.example.funplus.control

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.funplus.R
import com.example.funplus.model.Picture
import com.example.funplus.model.StepCounterService
import com.example.funplus.model.UserLocation
import com.example.funplus.utility.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.view.ViewParent
import androidx.viewpager.widget.ViewPager


const val TAG = "DBG"

class MainActivity : AppCompatActivity(){

    private lateinit var plusMinusFrag: NumberFrag
    private lateinit var letterFrag: LetterFrag
    private lateinit var fTransaction: FragmentTransaction
    private lateinit var fManager: FragmentManager

    private lateinit var fragPageAdapter: FragViewPagerAdapter
    private lateinit var viewPager: ViewPager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fManager = supportFragmentManager
        showPlusMinusFrag()
        plusMinusFrag = NumberFrag()
        letterFrag = LetterFrag()

        //Starts step counter service
      //  startService(Intent(applicationContext, StepCounterService::class.java))
        fragPageAdapter = FragViewPagerAdapter(fManager)
        viewPager = this.findViewById(R.id.fcontainer)
        viewPager.adapter = fragPageAdapter
        goToNumberGameBtn.setOnClickListener {
            goToGameFrag(plusMinusFrag)
        }
        goToLetterGameBtn.setOnClickListener {
            goToGameFrag(letterFrag)
        }

        fab_sos.setOnClickListener {
            Picture.takePicture(this, this)
            UserLocation.getLocation(this, this)
        }
    }
    //display plus-minus game by default when app starts

    private fun showPlusMinusFrag() {
        Log.d(TAG, "showPlusMinusFrag()")
        plusMinusFrag = NumberFrag()
        fTransaction = fManager.beginTransaction()
        fTransaction.add(R.id.fcontainer, plusMinusFrag)
        fTransaction.commit()
    }

    //switch between different fragments
    private fun goToGameFrag(frag: Fragment) {
        Log.d(TAG, "goToGameFrag")
        fTransaction = fManager.beginTransaction()
        fTransaction.replace(R.id.fcontainer, frag)
        fTransaction.commit()
    }

    /**
     * Function runs when the user successfully takes a picture and confirms. Bitmap of the picture
     * is converted into a Base64 string to be used for upload, along with the location data.
     * @param requestCode Request from previous activity
     * @param resultCode Checks if previous operation was initiated successfully, RESULT_OK ensures
     *                   that a picture was taken successfully.
     * @param data Data from the previous activity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        PermissionChecker.askForPermissionIfNotGranted(this, this, CAMERA_REQUEST_CODE, CAMERA)
        if (requestCode == Picture.captureReq && resultCode == Activity.RESULT_OK) {
            //get original bitmap, and resize it to half(for easier upload and download)
            val imgBitmap = BitmapFactory.decodeFile(Picture.photoPath)
            val oriWidth = imgBitmap.width
            val oriHeight = imgBitmap.height
            val finalWidth = (oriWidth*0.3).toInt()
            val finalHeight = (oriHeight*0.3).toInt()
            Log.d(TAG, "oriWidth="+oriWidth +" oriHeight=" + oriHeight)
            Log.d(TAG, "finalWidth="+finalWidth +" finalHeight=" + finalHeight)
            val finalBitmap = Bitmap.createScaledBitmap(imgBitmap, finalWidth, finalHeight, false)
            val byteArrayOutputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val bitmapString = Base64.encodeToString(byteArray, Base64.DEFAULT)
            doUpload(bitmapString)
        }
    }

    private fun doUpload(bitmapString: String) {
        Log.d(TAG, "doUpload MAINactity")
        UserLocation.getLocation(this, this).addOnCompleteListener {
            if (it.isSuccessful && it.result != null) {
                val currentLat = it.result!!.latitude
                val currentLong = it.result!!.longitude
                Log.d(TAG,"success: lat=" + currentLat.toString() + "longt=" + currentLong.toString())
                Uploader.doUpload(currentLat.toString(), currentLong.toString(), bitmapString)
            } else {
                Toast.makeText(this, "failed to get location coordinates", Toast.LENGTH_LONG).show()
                Log.d(TAG, it.result.toString())
            }
        }
    }

}
