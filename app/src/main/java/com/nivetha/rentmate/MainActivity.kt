package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnAction = findViewById<Button>(R.id.btnAction)
        val tvToggle = findViewById<TextView>(R.id.tvToggle)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvError = findViewById<TextView>(R.id.tvError)

        tvToggle.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                btnAction.text = getString(R.string.btn_login)
                tvToggle.text = getString(R.string.toggle_signup)
            } else {
                btnAction.text = getString(R.string.btn_signup)
                tvToggle.text = getString(R.string.toggle_login)
            }
            tvError.visibility = View.GONE
        }

        btnAction.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = getString(R.string.error_empty)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            tvError.visibility = View.GONE

            if (isLoginMode) {
                // Login
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        progressBar.visibility = View.GONE
                        if (task.isSuccessful) {
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        } else {
                            tvError.text = task.exception?.message ?: getString(R.string.auth_failed)
                            tvError.visibility = View.VISIBLE
                        }
                    }
            } else {
                // Register
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        progressBar.visibility = View.GONE
                        if (task.isSuccessful) {
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        } else {
                            tvError.text = task.exception?.message ?: getString(R.string.auth_failed)
                            tvError.visibility = View.VISIBLE
                        }
                    }
            }
        }
    }
}