package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class PropertyDetailsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

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

        db.collection("properties").document(propertyId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val property = document.toObject(Property::class.java)
                    property?.let {
                        tvTitle.text = it.title
                        tvRent.text = "₹${it.rent}"
                        tvLocation.text = "${getString(R.string.label_location)}${it.location}"
                        tvType.text = "${getString(R.string.label_property_type)}${it.propertyType}"
                        tvBedrooms.text = "${getString(R.string.label_bedrooms)}${it.bedrooms}"
                        tvRoomType.text = "${getString(R.string.label_room_type)}${it.roomType}"
                        val leaseText = if (it.leaseDuration.isNullOrEmpty()) getString(R.string.not_specified) else it.leaseDuration
                        tvLease.text = "${getString(R.string.hint_lease_duration)}: $leaseText"
                        tvAmenities.text = "${getString(R.string.label_amenities)}${it.amenities}"
                        tvRules.text = "${getString(R.string.label_rules)}${it.rules}"
                        tvDescription.text = "${getString(R.string.label_description)}${it.description}"
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
    }
}
