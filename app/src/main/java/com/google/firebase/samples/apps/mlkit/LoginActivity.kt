package com.google.firebase.samples.apps.mlkit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        progressBar.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        mAuth = FirebaseAuth.getInstance()
        mAuth.currentUser?.also {
            Intent(this, LivePreviewActivity::class.java).also {
                startActivity(it)
                finish()
            }
        }

        progressBar.visibility = View.GONE

        singUpTV.setOnClickListener {
            Intent(this, SignUpActivity::class.java).also {
                startActivity(it)
            }
        }

        loginBtn.setOnClickListener {
            val emailStr = emailET.text.toString()
            val passStr = passwordET.text.toString()
            if (emailStr.isEmpty() || passStr.isEmpty())
                showMessage("Please provide all the details")
            else
                authenticate(emailStr, passStr)
        }
    }

    private fun authenticate(emailStr: String, passStr: String) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authResult = mAuth.signInWithEmailAndPassword(emailStr, passStr).await()
                if(authResult != null && authResult.user != null){
                    withContext(Dispatchers.Main){
                        progressBar.visibility = View.GONE
                        Intent(this@LoginActivity, LivePreviewActivity::class.java).also {
                            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(it)
                        }
                    }
                }
            }catch (e: FirebaseAuthInvalidCredentialsException){
                withContext(Dispatchers.Main){
                    progressBar.visibility = View.GONE
                    showMessage("${e.message}")
                }
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    progressBar.visibility = View.GONE
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