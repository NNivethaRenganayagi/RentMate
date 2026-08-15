package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PropertyDetailsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_property_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val propertyId = intent.getStringExtra("PROPERTY_ID") ?: return

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvRent = findViewById<TextView>(R.id.tvRent)
        val tvLocation = findViewById<TextView>(R.id.tvLocation)
        val tvType = findViewById<TextView>(R.id.tvType)
        val tvBedrooms = findViewById<TextView>(R.id.tvBedrooms)
        val tvRoomType = findViewById<TextView>(R.id.tvRoomType)
        val tvLease = findViewById<TextView>(R.id.tvLease)
        val tvAmenities = findViewById<TextView>(R.id.tvAmenities)
        val tvRules = findViewById<TextView>(R.id.tvRules)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val btnCheckCompatibility = findViewById<Button>(R.id.btnCheckCompatibility)
        val btnCheckLandlordCompatibility = findViewById<Button>(R.id.btnCheckLandlordCompatibility)
        
        val layoutManagement = findViewById<LinearLayout>(R.id.layoutManagement)
        val btnEdit = findViewById<Button>(R.id.btnEditProperty)
        val btnDelete = findViewById<Button>(R.id.btnDeleteProperty)

        db.collection("properties").document(propertyId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val property = document.toObject(Property::class.java)
                    property?.let {
                        tvTitle.text = it.title
                        tvRent.text = "₹${it.rent}"
                        tvLocation.text = it.location
                        tvType.text = it.propertyType
                        tvBedrooms.text = "${it.bedrooms} BHK"
                        tvRoomType.text = it.roomType
                        
                        tvAmenities.text = if (it.amenities.isNullOrEmpty()) getString(R.string.not_specified) else it.amenities
                        tvRules.text = if (it.rules.isNullOrEmpty()) getString(R.string.not_specified) else it.rules
                        tvLease.text = if (it.leaseDuration.isNullOrEmpty()) getString(R.string.not_specified) else it.leaseDuration
                        tvDescription.text = if (it.description.isNullOrEmpty()) getString(R.string.no_description) else it.description

                        // Check ownership
                        if (it.ownerId == auth.currentUser?.uid) {
                            layoutManagement.visibility = View.VISIBLE
                        }
                    }
                }
            }

        btnCheckCompatibility.setOnClickListener {
            val intent = Intent(this, CompatibilityResultActivity::class.java)
            intent.putExtra("PROPERTY_ID", propertyId)
            startActivity(intent)
        }

        btnCheckLandlordCompatibility.setOnClickListener {
            val intent = Intent(this, LandlordTenantCompatibilityActivity::class.java)
            intent.putExtra("PROPERTY_ID", propertyId)
            startActivity(intent)
        }

        btnEdit.setOnClickListener {
            val intent = Intent(this, AddPropertyActivity::class.java)
            intent.putExtra("PROPERTY_ID", propertyId)
            startActivity(intent)
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation(propertyId)
        }
    }

    private fun showDeleteConfirmation(propertyId: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setPositiveButton(R.string.btn_confirm_delete) { _, _ ->
                deleteProperty(propertyId)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun deleteProperty(propertyId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        
        // Final ownership check before delete
        db.collection("properties").document(propertyId).get().addOnSuccessListener { doc ->
            if (doc.getString("ownerId") == currentUid) {
                db.collection("properties").document(propertyId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.property_deleted, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error deleting property", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, R.string.error_ownership, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
