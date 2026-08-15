package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
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
        val scrollHome = findViewById<ScrollView>(R.id.scrollHome)
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        
        val layoutTenant = findViewById<LinearLayout>(R.id.layoutTenantActions)
        val layoutLandlord = findViewById<LinearLayout>(R.id.layoutLandlordActions)
        
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)
        val btnFindProperties = findViewById<Button>(R.id.btnViewProperties)
        val btnMyProperties = findViewById<Button>(R.id.btnMyProperties)
        val btnFindRoommates = findViewById<Button>(R.id.btnFindRoommates)
        val btnAddProperty = findViewById<Button>(R.id.btnAddPropertyHome)
        val btnLogout = findViewById<Button>(R.id.btnLogoutHome)

        loadUserRoleAndSetupUI(pbLoading, scrollHome, tvSubtitle, layoutTenant, layoutLandlord)

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
        pb: ProgressBar, scroll: ScrollView, tvSubtitle: TextView,
        layoutTenant: LinearLayout, layoutLandlord: LinearLayout
    ) {
        val uid = auth.currentUser?.uid ?: return
        pb.visibility = View.VISIBLE
        scroll.visibility = View.GONE

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                pb.visibility = View.GONE
                scroll.visibility = View.VISIBLE
                if (doc.exists()) {
                    val role = doc.getString("role") ?: "Tenant"

                    if (role == "Landlord") {
                        tvSubtitle.text = getString(R.string.subtitle_landlord)
                        layoutLandlord.visibility = View.VISIBLE
                        layoutTenant.visibility = View.GONE
                    } else {
                        tvSubtitle.text = getString(R.string.subtitle_tenant)
                        layoutTenant.visibility = View.VISIBLE
                        layoutLandlord.visibility = View.GONE
                    }
                } else {
                    // Safe fallback for new users
                    tvSubtitle.text = getString(R.string.subtitle_tenant)
                    layoutTenant.visibility = View.VISIBLE
                    layoutLandlord.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                pb.visibility = View.GONE
                scroll.visibility = View.VISIBLE
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
            }
    }
}