package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUserRole: String = "Tenant"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val pbLoading = findViewById<ProgressBar>(R.id.pbProfileLoading)
        val scrollProfile = findViewById<ScrollView>(R.id.scrollProfile)
        
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        
        // Tenant Layouts
        val layoutTenantRental = findViewById<LinearLayout>(R.id.layoutTenantRental)
        val layoutTenantLifestyle = findViewById<LinearLayout>(R.id.layoutTenantLifestyle)
        val etBudget = findViewById<EditText>(R.id.etBudget)
        val etPrefLocation = findViewById<EditText>(R.id.etPrefLocation)
        val etRoomType = findViewById<EditText>(R.id.etRoomType)
        val sbCleanliness = findViewById<SeekBar>(R.id.sbCleanliness)
        val spSleepSchedule = findViewById<Spinner>(R.id.spSleepSchedule)
        val rgFood = findViewById<RadioGroup>(R.id.rgFood)
        val cbSmoking = findViewById<CheckBox>(R.id.cbSmoking)
        val cbPets = findViewById<CheckBox>(R.id.cbPets)

        // Landlord Layouts
        val layoutLandlordInfo = findViewById<LinearLayout>(R.id.layoutLandlordInfo)
        val etMinRent = findViewById<EditText>(R.id.etMinRent)
        val etMaxRent = findViewById<EditText>(R.id.etMaxRent)
        val etPreferredPropType = findViewById<EditText>(R.id.etPreferredPropType)

        // Shared
        val etLeaseDuration = findViewById<EditText>(R.id.etLeaseDuration)
        val etAmenities = findViewById<EditText>(R.id.etAmenities)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val progressBar = findViewById<ProgressBar>(R.id.profileProgressBar)

        loadProfileData(pbLoading, scrollProfile, etFullName, etAge, rgRole, etLocation, 
            etBudget, etPrefLocation, etRoomType, sbCleanliness, spSleepSchedule, rgFood, cbSmoking, cbPets,
            layoutTenantRental, layoutTenantLifestyle, layoutLandlordInfo,
            etMinRent, etMaxRent, etPreferredPropType, etLeaseDuration, etAmenities)

        btnSaveProfile.setOnClickListener {
            saveProfile(etFullName, etAge, etLocation, etBudget, etPrefLocation, etRoomType,
                sbCleanliness, spSleepSchedule, rgFood, cbSmoking, cbPets,
                etMinRent, etMaxRent, etPreferredPropType, etLeaseDuration, etAmenities,
                btnSaveProfile, progressBar)
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

    private fun loadProfileData(
        pb: ProgressBar, scroll: ScrollView, etName: EditText, etAge: EditText, rgRole: RadioGroup, etLoc: EditText,
        etBud: EditText, etPrefLoc: EditText, etRoom: EditText, sbClean: SeekBar,
        spSleep: Spinner, rgFood: RadioGroup, cbSmoke: CheckBox, cbPets: CheckBox,
        layTenantRental: View, layTenantLife: View, layLandlord: View,
        etMinR: EditText, etMaxR: EditText, etPropT: EditText, etLease: EditText, etAmen: EditText
    ) {
        val uid = auth.currentUser?.uid ?: return
        pb.visibility = View.VISIBLE
        scroll.visibility = View.GONE

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                pb.visibility = View.GONE
                scroll.visibility = View.VISIBLE
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    profile?.let {
                        currentUserRole = it.role
                        etName.setText(it.fullName)
                        etAge.setText(it.age)
                        etLoc.setText(it.location)
                        etLease.setText(it.leaseDuration)
                        etAmen.setText(it.amenities)

                        if (it.role == "Landlord") {
                            rgRole.check(R.id.rbLandlord)
                            layTenantRental.visibility = View.GONE
                            layTenantLife.visibility = View.GONE
                            layLandlord.visibility = View.VISIBLE
                            etMinR.setText(it.minRent)
                            etMaxR.setText(it.maxRent)
                            etPropT.setText(it.preferredPropertyType)
                        } else {
                            rgRole.check(R.id.rbTenant)
                            layTenantRental.visibility = View.VISIBLE
                            layTenantLife.visibility = View.VISIBLE
                            layLandlord.visibility = View.GONE
                            etBud.setText(it.budget)
                            etPrefLoc.setText(it.preferredLocation)
                            etRoom.setText(it.roomType)
                            sbClean.progress = (it.cleanliness - 1).coerceIn(0, 4)
                            
                            val sleepArray = resources.getStringArray(R.array.sleep_schedules)
                            val sleepIndex = sleepArray.indexOf(it.sleepSchedule)
                            if (sleepIndex >= 0) spSleep.setSelection(sleepIndex)

                            when (it.foodPreference) {
                                "Vegetarian" -> rgFood.check(R.id.rbVeg)
                                "Non-Vegetarian" -> rgFood.check(R.id.rbNonVeg)
                                else -> rgFood.check(R.id.rbAnyFood)
                            }
                            cbSmoke.isChecked = it.smoking
                            cbPets.isChecked = it.pets
                        }
                    }
                }
            }
            .addOnFailureListener {
                pb.visibility = View.GONE
                scroll.visibility = View.VISIBLE
            }
    }

    private fun saveProfile(
        etName: EditText, etAge: EditText, etLoc: EditText, etBud: EditText, etPrefLoc: EditText, etRoom: EditText,
        sbClean: SeekBar, spSleep: Spinner, rgFood: RadioGroup, cbSmoke: CheckBox, cbPets: CheckBox,
        etMinR: EditText, etMaxR: EditText, etPropT: EditText, etLease: EditText, etAmen: EditText,
        btnSave: Button, pb: ProgressBar
    ) {
        val fullName = etName.text.toString().trim()
        val age = etAge.text.toString().trim()
        val location = etLoc.text.toString().trim()

        if (fullName.isEmpty() || age.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, R.string.error_mandatory, Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        val userProfileMap = mutableMapOf<String, Any>(
            "fullName" to fullName,
            "age" to age,
            "location" to location,
            "leaseDuration" to etLease.text.toString().trim(),
            "amenities" to etAmen.text.toString().trim(),
            "role" to currentUserRole
        )

        if (currentUserRole == "Landlord") {
            userProfileMap["minRent"] = etMinR.text.toString().trim()
            userProfileMap["maxRent"] = etMaxR.text.toString().trim()
            userProfileMap["preferredPropertyType"] = etPropT.text.toString().trim()
        } else {
            userProfileMap["budget"] = etBud.text.toString().trim()
            userProfileMap["preferredLocation"] = etPrefLoc.text.toString().trim()
            userProfileMap["roomType"] = etRoom.text.toString().trim()
            userProfileMap["cleanliness"] = sbClean.progress + 1
            userProfileMap["sleepSchedule"] = spSleep.selectedItem.toString()
            userProfileMap["foodPreference"] = when (rgFood.checkedRadioButtonId) {
                R.id.rbVeg -> "Vegetarian"
                R.id.rbNonVeg -> "Non-Vegetarian"
                else -> "Any"
            }
            userProfileMap["smoking"] = cbSmoke.isChecked
            userProfileMap["pets"] = cbPets.isChecked
        }

        pb.visibility = View.VISIBLE
        btnSave.isEnabled = false

        db.collection("users").document(uid).update(userProfileMap)
            .addOnSuccessListener {
                pb.visibility = View.GONE
                btnSave.isEnabled = true
                Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                db.collection("users").document(uid).set(userProfileMap)
                    .addOnSuccessListener {
                        pb.visibility = View.GONE
                        btnSave.isEnabled = true
                        Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { err ->
                        pb.visibility = View.GONE
                        btnSave.isEnabled = true
                        Toast.makeText(this, "${getString(R.string.profile_save_failed)}: ${err.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }
}
