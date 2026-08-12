package com.nivetha.rentmate

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RoommateCompatibilityActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_roommate_compatibility)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val selectedUserUid = intent.getStringExtra("SELECTED_USER_UID") ?: return
        val selectedUserName = intent.getStringExtra("SELECTED_USER_NAME") ?: "Roommate"

        val pbLoading = findViewById<ProgressBar>(R.id.pbLoadingCompatibility)
        val tvName = findViewById<TextView>(R.id.tvRoommateName)
        val tvOverall = findViewById<TextView>(R.id.tvOverallRoommateScore)
        val tvCat = findViewById<TextView>(R.id.tvRoommateCategory)
        val tvBudget = findViewById<TextView>(R.id.tvBudgetScore)
        val tvLocation = findViewById<TextView>(R.id.tvLocationScore)
        val tvClean = findViewById<TextView>(R.id.tvCleanlinessScore)
        val tvSleep = findViewById<TextView>(R.id.tvSleepScore)
        val tvFood = findViewById<TextView>(R.id.tvFoodScore)
        val tvSmoking = findViewById<TextView>(R.id.tvSmokingScore)
        val tvPets = findViewById<TextView>(R.id.tvPetsScore)
        val tvExplanations = findViewById<TextView>(R.id.tvRoommateExplanations)

        tvName.text = selectedUserName
        fetchDataAndCalculate(selectedUserUid, pbLoading, tvOverall, tvCat, tvBudget, tvLocation, 
            tvClean, tvSleep, tvFood, tvSmoking, tvPets, tvExplanations)
    }

    private fun fetchDataAndCalculate(
        otherUid: String,
        pbLoading: ProgressBar,
        tvOverall: TextView,
        tvCat: TextView,
        tvBudget: TextView,
        tvLocation: TextView,
        tvClean: TextView,
        tvSleep: TextView,
        tvFood: TextView,
        tvSmoking: TextView,
        tvPets: TextView,
        tvExplanations: TextView
    ) {
        val currentUid = auth.currentUser?.uid ?: return
        pbLoading.visibility = View.VISIBLE

        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { currentDoc ->
                val currentUser = currentDoc.toObject(UserProfile::class.java)
                if (currentUser == null) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this, R.string.error_profile_missing, Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                db.collection("users").document(otherUid).get()
                    .addOnSuccessListener { otherDoc ->
                        val otherUser = otherDoc.toObject(UserProfile::class.java)
                        pbLoading.visibility = View.GONE
                        if (otherUser == null) {
                            Toast.makeText(this, "Error loading roommate profile", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        calculateRoommateCompatibility(currentUser, otherUser, tvOverall, tvCat, 
                            tvBudget, tvLocation, tvClean, tvSleep, tvFood, tvSmoking, tvPets, tvExplanations)
                    }
            }
    }

    private fun calculateRoommateCompatibility(
        me: UserProfile,
        them: UserProfile,
        tvOverall: TextView,
        tvCat: TextView,
        tvBudget: TextView,
        tvLocation: TextView,
        tvClean: TextView,
        tvSleep: TextView,
        tvFood: TextView,
        tvSmoking: TextView,
        tvPets: TextView,
        tvExplanations: TextView
    ) {
        val explanations = mutableListOf<String>()

        // 1. Budget (20%)
        val myBudget = me.budget.toDoubleOrNull() ?: 0.0
        val theirBudget = them.budget.toDoubleOrNull() ?: 0.0
        var budgetScore = 0.0
        if (myBudget > 0 && theirBudget > 0) {
            val diff = abs(myBudget - theirBudget)
            val avg = (myBudget + theirBudget) / 2
            budgetScore = max(0.0, 100.0 - (diff / avg * 100.0))
            if (budgetScore >= 80) explanations.add(getString(R.string.match_budget_similar))
            else if (budgetScore < 50) explanations.add(getString(R.string.conflict_budget))
        }

        // 2. Location (15%)
        var locationScore = 0.0
        if (me.preferredLocation.contains(them.preferredLocation, ignoreCase = true) || 
            them.preferredLocation.contains(me.preferredLocation, ignoreCase = true)) {
            locationScore = 100.0
            explanations.add(getString(R.string.match_location_city))
        } else {
            locationScore = 40.0
            explanations.add(getString(R.string.conflict_location))
        }

        // 3. Cleanliness (20%)
        val cleanDiff = abs(me.cleanliness - them.cleanliness)
        val cleanlinessScore = (5 - cleanDiff) / 5.0 * 100.0
        if (cleanlinessScore >= 80) explanations.add(getString(R.string.match_clean_similar))
        else if (cleanlinessScore < 50) explanations.add(getString(R.string.conflict_clean))

        // 4. Sleep Schedule (15%)
        var sleepScore = 0.0
        if (me.sleepSchedule == them.sleepSchedule) {
            sleepScore = 100.0
            explanations.add(getString(R.string.match_sleep_same))
        } else {
            // Early Bird vs Normal = 60, Normal vs Night Owl = 60, Early Bird vs Night Owl = 20
            sleepScore = 40.0
            explanations.add(getString(R.string.conflict_sleep))
        }

        // 5. Food Preference (10%)
        var foodScore = 0.0
        if (me.foodPreference == them.foodPreference || me.foodPreference == "Any" || them.foodPreference == "Any") {
            foodScore = 100.0
            explanations.add(getString(R.string.match_food_ok))
        } else {
            foodScore = 30.0
            explanations.add(getString(R.string.conflict_food))
        }

        // 6. Smoking (10%)
        val smokingScore = if (me.smoking == them.smoking) 100.0 else 20.0
        if (me.smoking != them.smoking) explanations.add(getString(R.string.conflict_smoking))

        // 7. Pets (10%)
        val petsScore = if (me.pets == them.pets) 100.0 else 20.0
        if (me.pets != them.pets) explanations.add(getString(R.string.conflict_pets))

        // Total Weighted Score
        val totalScore = (budgetScore * 0.20) + (locationScore * 0.15) + (cleanlinessScore * 0.20) +
                         (sleepScore * 0.15) + (foodScore * 0.10) + (smokingScore * 0.10) + (petsScore * 0.10)

        tvOverall.text = "${totalScore.toInt()}%"
        
        val category = when {
            totalScore >= 80 -> getString(R.string.cat_excellent)
            totalScore >= 60 -> getString(R.string.cat_good)
            totalScore >= 40 -> getString(R.string.cat_moderate)
            else -> getString(R.string.cat_poor)
        }
        tvCat.text = category

        tvBudget.text = "Budget Match: ${budgetScore.toInt()}%"
        tvLocation.text = "Location Match: ${locationScore.toInt()}%"
        tvClean.text = "${getString(R.string.label_cleanliness_match)}${cleanlinessScore.toInt()}%"
        tvSleep.text = "${getString(R.string.label_sleep_match)}${sleepScore.toInt()}%"
        tvFood.text = "${getString(R.string.label_food_match)}${foodScore.toInt()}%"
        tvSmoking.text = "Smoking Preference Match: ${smokingScore.toInt()}%"
        tvPets.text = "Pets Preference Match: ${petsScore.toInt()}%"

        tvExplanations.text = explanations.joinToString("\n")
    }
}
