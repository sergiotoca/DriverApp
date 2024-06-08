package com.groupf.driverapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.driverapp.R
import com.groupf.driverapp.Model.DriverInfoModel
import com.example.driverapp.databinding.ActivitySplashScreenBinding
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.Arrays
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object{
        private const val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<IdpConfig>
    private lateinit var firebaseAuth:FirebaseAuth
    private lateinit var listener:FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference
    private lateinit var binding: ActivitySplashScreenBinding

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if(firebaseAuth != null && listener != null) firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    private fun delaySplashScreen(){
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)
            }

    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

    }

    private fun init(){

        database = FirebaseDatabase.getInstance()
        driverInfoRef =  database.getReference(Common.DRIVER_INFO_REFERENCE)


        providers = Arrays.asList(
            IdpConfig.PhoneBuilder().build(),
            IdpConfig.GoogleBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if(user != null)
            {
                checkUserFromFirebase()
            }
            else
                showLoginLayout()
        }
    }

    private fun checkUserFromFirebase() {
        driverInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.exists())
                    {
                        //Toast.makeText(this@SplashScreenActivity, "User already register!", Toast.LENGTH_SHORT).show()
                        val model = dataSnapshot.getValue(DriverInfoModel::class.java)
                        goToHomeActivity(model)
                    }
                    else
                    {
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, p0.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun goToHomeActivity(model: DriverInfoModel?) {
        Common.currentUser = model
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null)

        val edt_first_name = itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edt_last_name = itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edt_phone_number = itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText

        val btn_continue = itemView.findViewById<View>(R.id.btn_register) as Button


        //Set Data
        if(FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
                !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        //View
        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        //Event
        btn_continue.setOnClickListener{
            if(TextUtils.isDigitsOnly(edt_first_name.text.toString()))
            {
                Toast.makeText( this@SplashScreenActivity, "Please enter First Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_last_name.text.toString()))
            {
                Toast.makeText( this@SplashScreenActivity, "Please enter Last Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_phone_number.text.toString()))
            {
                Toast.makeText( this@SplashScreenActivity, "Please enter Phone Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else
            {
                val model = DriverInfoModel()
                model.firstName = edt_first_name.text.toString()
                model.lastName = edt_last_name.text.toString()
                model.phoneNumber = edt_phone_number.text.toString()
                model.rating = 0.0


                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener {e ->
                        Toast.makeText( this@SplashScreenActivity, ""+e.message, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        binding.progressBar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText( this@SplashScreenActivity, "Register Successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                        goToHomeActivity(model)
                        binding.progressBar.visibility = View.GONE
                    }
            }

        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build();

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
            , LOGIN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == LOGIN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            }
            else
                Toast.makeText( this@SplashScreenActivity, ""+response!!.error!!.message, Toast.LENGTH_SHORT).show()
        }
    }
}