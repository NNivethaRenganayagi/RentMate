package com.nivetha.rentmate

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddPropertyActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_property)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        checkLandlordRole()

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val etRent = findViewById<EditText>(R.id.etRent)
        val etBedrooms = findViewById<EditText>(R.id.etBedrooms)
        val rgType = findViewById<RadioGroup>(R.id.rgPropertyType)
        val rgRoom = findViewById<RadioGroup>(R.id.rgRoomType)
        val cbWifi = findViewById<CheckBox>(R.id.cbWifi)
        val cbParking = findViewById<CheckBox>(R.id.cbParking)
        val cbKitchen = findViewById<CheckBox>(R.id.cbKitchen)
        val cbWashing = findViewById<CheckBox>(R.id.cbWashing)
        val cbAC = findViewById<CheckBox>(R.id.cbAC)
        val cbFurnished = findViewById<CheckBox>(R.id.cbFurnished)
        val rgSmoking = findViewById<RadioGroup>(R.id.rgSmoking)
        val rgPets = findViewById<RadioGroup>(R.id.rgPets)
        val etLease = findViewById<EditText>(R.id.etLease)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnSave = findViewById<Button>(R.id.btnSaveProperty)
        val progressBar = findViewById<ProgressBar>(R.id.pbSaving)

        btnSave.setOnClickListener {
            saveProperty(etTitle, etLocation, etRent, etBedrooms, rgType, rgRoom,
                cbWifi, cbParking, cbKitchen, cbWashing, cbAC, cbFurnished,
                rgSmoking, rgPets, etLease, etDescription, btnSave, progressBar)
        }
    }

    private fun checkLandlordRole() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")
                if (role != "Landlord") {
                    Toast.makeText(this, R.string.error_landlord_only, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
    }

    private fun saveProperty(
        etTitle: EditText, etLoc: EditText, etRent: EditText, etBed: EditText,
        rgType: RadioGroup, rgRoom: RadioGroup,
        cbWifi: CheckBox, cbPark: CheckBox, cbKit: CheckBox, cbWash: CheckBox, cbAC: CheckBox, cbFur: CheckBox,
        rgSmoke: RadioGroup, rgPets: RadioGroup, etLease: EditText, etDesc: EditText,
        btnSave: Button, pb: ProgressBar
    ) {
        val title = etTitle.text.toString().trim()
        val location = etLoc.text.toString().trim()
        val rentStr = etRent.text.toString().trim()
        val bedStr = etBed.text.toString().trim()
        val lease = etLease.text.toString().trim()

        if (title.isEmpty() || location.isEmpty() || rentStr.isEmpty() || bedStr.isEmpty()) {
            Toast.makeText(this, R.string.error_mandatory, Toast.LENGTH_SHORT).show()
            return
        }

        val rent = rentStr.toIntOrNull() ?: -1
        val bedrooms = bedStr.toIntOrNull() ?: -1

        if (rent <= 0) {
            Toast.makeText(this, R.string.error_invalid_rent, Toast.LENGTH_SHORT).show()
            return
        }
        if (bedrooms <= 0) {
            Toast.makeText(this, R.string.error_invalid_bedrooms, Toast.LENGTH_SHORT).show()
            return
        }

        val propType = when (rgType.checkedRadioButtonId) {
            R.id.rbApartment -> "Apartment"
            R.id.rbHouse -> "Independent House"
            R.id.rbPG -> "PG"
            else -> "Apartment"
        }

        val roomType = if (rgRoom.checkedRadioButtonId == R.id.rbPrivate) "Private" else "Shared"

        val amenitiesList = mutableListOf<String>()
        if (cbWifi.isChecked) amenitiesList.add("WiFi")
        if (cbPark.isChecked) amenitiesList.add("Parking")
        if (cbKit.isChecked) amenitiesList.add("Kitchen")
        if (cbWash.isChecked) amenitiesList.add("Washing Machine")
        if (cbAC.isChecked) amenitiesList.add("AC")
        if (cbFur.isChecked) amenitiesList.add("Furnished")
        val amenities = amenitiesList.joinToString(", ")

        val rulesList = mutableListOf<String>()
        rulesList.add(if (rgSmoke.checkedRadioButtonId == R.id.rbSmokingNo) "No Smoking" else "Smoking Allowed")
        rulesList.add(if (rgPets.checkedRadioButtonId == R.id.rbPetsNo) "No Pets" else "Pets Allowed")
        val rules = rulesList.joinToString(", ")

        val ownerId = auth.currentUser?.uid ?: return
        
        val propertyData = hashMapOf(
            "title" to title,
            "location" to location,
            "rent" to rentStr, // Storing as string to match existing model/list display logic, but validated as int
            "propertyType" to propType,
            "bedrooms" to bedrooms,
            "roomType" to roomType,
            "amenities" to amenities,
            "rules" to rules,
            "leaseDuration" to lease,
            "description" to etDesc.text.toString().trim(),
            "ownerId" to ownerId
        )

        pb.visibility = View.VISIBLE
        btnSave.isEnabled = false

        db.collection("properties").add(propertyData)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.property_saved, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                pb.visibility = View.GONE
                btnSave.isEnabled = true
                Toast.makeText(this, R.string.property_save_failed, Toast.LENGTH_SHORT).show()
            }
    }
}
