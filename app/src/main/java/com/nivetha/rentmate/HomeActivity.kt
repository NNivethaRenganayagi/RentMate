package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewProperties).setOnClickListener {
            startActivity(Intent(this, PropertyListActivity::class.java))
        }

        findViewById<Button>(R.id.btnFindRoommates).setOnClickListener {
            startActivity(Intent(this, RoommateListActivity::class.java))
        }

        val btnAddPropertyHome = findViewById<Button>(R.id.btnAddPropertyHome)
        checkUserRole(btnAddPropertyHome)
    }

    private fun checkUserRole(btn: Button) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.getString("role") == "Landlord") {
                    btn.visibility = View.VISIBLE
                    btn.setOnClickListener {
                        startActivity(Intent(this, AddPropertyActivity::class.java))
                    }
                }
            }
    }
}