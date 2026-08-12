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

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val etBudget = findViewById<EditText>(R.id.etBudget)
        val etPrefLocation = findViewById<EditText>(R.id.etPrefLocation)
        val etRoomType = findViewById<EditText>(R.id.etRoomType)
        val sbCleanliness = findViewById<SeekBar>(R.id.sbCleanliness)
        val spSleepSchedule = findViewById<Spinner>(R.id.spSleepSchedule)
        val rgFood = findViewById<RadioGroup>(R.id.rgFood)
        val cbSmoking = findViewById<CheckBox>(R.id.cbSmoking)
        val cbPets = findViewById<CheckBox>(R.id.cbPets)
        val etLeaseDuration = findViewById<EditText>(R.id.etLeaseDuration)
        val etAmenities = findViewById<EditText>(R.id.etAmenities)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val progressBar = findViewById<ProgressBar>(R.id.profileProgressBar)

        loadProfileData(etFullName, etAge, rgRole, etLocation, etBudget, etPrefLocation,
            etRoomType, sbCleanliness, spSleepSchedule, rgFood, cbSmoking, cbPets,
            etLeaseDuration, etAmenities, progressBar)

        btnSaveProfile.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val age = etAge.text.toString().trim()
            val location = etLocation.text.toString().trim()

            if (fullName.isEmpty() || age.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, R.string.error_mandatory, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = if (findViewById<RadioButton>(rgRole.checkedRadioButtonId).id == R.id.rbTenant) "Tenant" else "Landlord"
            val budget = etBudget.text.toString().trim()
            val prefLocation = etPrefLocation.text.toString().trim()
            val roomType = etRoomType.text.toString().trim()
            val cleanliness = sbCleanliness.progress + 1
            val sleepSchedule = spSleepSchedule.selectedItem.toString()
            val foodPref = when (rgFood.checkedRadioButtonId) {
                R.id.rbVeg -> "Vegetarian"
                R.id.rbNonVeg -> "Non-Vegetarian"
                else -> "Any"
            }
            val smoking = cbSmoking.isChecked
            val pets = cbPets.isChecked
            val leaseDuration = etLeaseDuration.text.toString().trim()
            val amenities = etAmenities.text.toString().trim()

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val userProfile = hashMapOf(
                "fullName" to fullName,
                "age" to age,
                "role" to role,
                "location" to location,
                "budget" to budget,
                "preferredLocation" to prefLocation,
                "roomType" to roomType,
                "cleanliness" to cleanliness,
                "sleepSchedule" to sleepSchedule,
                "foodPreference" to foodPref,
                "smoking" to smoking,
                "pets" to pets,
                "leaseDuration" to leaseDuration,
                "amenities" to amenities
            )

            progressBar.visibility = View.VISIBLE
            btnSaveProfile.isEnabled = false

            db.collection("users").document(userId)
                .set(userProfile)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    btnSaveProfile.isEnabled = true
                    Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSaveProfile.isEnabled = true
                    Toast.makeText(this, "${getString(R.string.profile_save_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
        etName: EditText, etAge: EditText, rgRole: RadioGroup, etLoc: EditText,
        etBud: EditText, etPrefLoc: EditText, etRoom: EditText, sbClean: SeekBar,
        spSleep: Spinner, rgFood: RadioGroup, cbSmoke: CheckBox, cbPets: CheckBox,
        etLease: EditText, etAmen: EditText, pb: ProgressBar
    ) {
        val uid = auth.currentUser?.uid ?: return
        pb.visibility = View.VISIBLE

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                pb.visibility = View.GONE
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    profile?.let {
                        etName.setText(it.fullName)
                        etAge.setText(it.age)
                        etLoc.setText(it.location)
                        etBud.setText(it.budget)
                        etPrefLoc.setText(it.preferredLocation)
                        etRoom.setText(it.roomType)
                        sbClean.progress = (it.cleanliness - 1).coerceIn(0, 4)
                        
                        if (it.role == "Landlord") {
                            rgRole.check(R.id.rbLandlord)
                        } else {
                            rgRole.check(R.id.rbTenant)
                        }

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
                        etLease.setText(it.leaseDuration)
                        etAmen.setText(it.amenities)
                    }
                }
            }
            .addOnFailureListener {
                pb.visibility = View.GONE
            }
    }
}