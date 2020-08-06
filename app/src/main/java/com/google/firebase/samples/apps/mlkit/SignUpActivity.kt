package com.google.firebase.samples.apps.mlkit

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        mAuth = FirebaseAuth.getInstance()

        signUpBtn.setOnClickListener {
            val emailStr = emailET.text.toString()
            val passStr = passwordET.text.toString()
            val cnfrmPassStr = confirmPassET.text.toString()
            if (emailStr.isEmpty() || passStr.isEmpty() || cnfrmPassStr.isEmpty())
                showMessage("Please provide all the details")
            else if (passStr.length < 8)
                showMessage("Use 8 characters or more for password")
            else if (passStr != cnfrmPassStr)
                showMessage("Both passwords didn't match. Try again")
            else
                signUpUser(emailStr, passStr)

        }
    }

    private fun signUpUser(emailStr: String, passStr: String) {
        val dialog = ProgressDialog(this)
        dialog.setTitle("Registering User...")
        dialog.setMessage("Please wait while we're creating your profile")
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authResult = mAuth.createUserWithEmailAndPassword(emailStr, passStr).await()
                if (authResult.user != null) {
                    withContext(Dispatchers.Main) {
                        Intent(this@SignUpActivity, LivePreviewActivity::class.java).also {
                            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(it)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    showDialog("Error : ${e.message}")
                }
            }
        }
    }


    private fun showDialog(msg: String) {
        AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Okay") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
    }

    private fun showMessage(msg: String) {
        Snackbar.make(
                findViewById(android.R.id.content),
                msg,
                Snackbar.LENGTH_LONG
        ).show()
    }
}
