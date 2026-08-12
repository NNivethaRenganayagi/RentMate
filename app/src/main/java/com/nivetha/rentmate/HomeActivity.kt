package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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

        val pbLoading = findViewById<ProgressBar>(R.id.pbHomeLoading)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)
        val btnFindProperties = findViewById<Button>(R.id.btnViewProperties)
        val btnMyProperties = findViewById<Button>(R.id.btnMyProperties)
        val btnFindRoommates = findViewById<Button>(R.id.btnFindRoommates)
        val btnAddProperty = findViewById<Button>(R.id.btnAddPropertyHome)
        val btnLogout = findViewById<Button>(R.id.btnLogoutHome)

        loadUserRoleAndSetupUI(pbLoading, tvWelcome, btnEditProfile, btnFindProperties, 
            btnMyProperties, btnFindRoommates, btnAddProperty, btnLogout)

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnFindProperties.setOnClickListener {
            val intent = Intent(this, PropertyListActivity::class.java)
            intent.putExtra("ONLY_MY_PROPERTIES", false)
            startActivity(intent)
        }

        btnMyProperties.setOnClickListener {
            val intent = Intent(this, PropertyListActivity::class.java)
            intent.putExtra("ONLY_MY_PROPERTIES", true)
            startActivity(intent)
        }

        btnFindRoommates.setOnClickListener {
            startActivity(Intent(this, RoommateListActivity::class.java))
        }

        btnAddProperty.setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserRoleAndSetupUI(
        pb: ProgressBar, tvWelcome: TextView, btnEdit: Button, 
        btnFindProp: Button, btnMyProp: Button, btnFindRoom: Button, 
        btnAddProp: Button, btnLog: Button
    ) {
        val uid = auth.currentUser?.uid ?: return
        pb.visibility = View.VISIBLE

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                pb.visibility = View.GONE
                if (doc.exists()) {
                    val role = doc.getString("role") ?: "Tenant"
                    tvWelcome.visibility = View.VISIBLE
                    btnEdit.visibility = View.VISIBLE
                    btnLog.visibility = View.VISIBLE

                    if (role == "Landlord") {
                        btnMyProp.visibility = View.VISIBLE
                        btnAddProp.visibility = View.VISIBLE
                        btnFindProp.visibility = View.GONE
                        btnFindRoom.visibility = View.GONE
                    } else {
                        btnFindProp.visibility = View.VISIBLE
                        btnFindRoom.visibility = View.VISIBLE
                        btnMyProp.visibility = View.GONE
                        btnAddProp.visibility = View.GONE
                    }
                } else {
                    // Safe fallback for new users without profile doc yet
                    tvWelcome.visibility = View.VISIBLE
                    btnEdit.visibility = View.VISIBLE
                    btnLog.visibility = View.VISIBLE
                    btnFindProp.visibility = View.VISIBLE
                    btnFindRoom.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                pb.visibility = View.GONE
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
            }
    }
}