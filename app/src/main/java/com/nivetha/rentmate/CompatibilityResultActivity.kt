package com.nivetha.rentmate

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.max

class CompatibilityResultActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compatibility_result)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val propertyId = intent.getStringExtra("PROPERTY_ID") ?: return

        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvPropertyTitle = findViewById<TextView>(R.id.tvPropertyTitle)
        val tvOverallScore = findViewById<TextView>(R.id.tvOverallScore)
        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvBudgetScore = findViewById<TextView>(R.id.tvBudgetScore)
        val tvLocationScore = findViewById<TextView>(R.id.tvLocationScore)
        val tvRoomTypeScore = findViewById<TextView>(R.id.tvRoomTypeScore)
        val tvAmenitiesScore = findViewById<TextView>(R.id.tvAmenitiesScore)
        val tvRulesScore = findViewById<TextView>(R.id.tvRulesScore)
        val tvExplanations = findViewById<TextView>(R.id.tvExplanations)

        fetchDataAndCalculate(propertyId, pbLoading, tvPropertyTitle, tvOverallScore, tvCategory,
            tvBudgetScore, tvLocationScore, tvRoomTypeScore, tvAmenitiesScore, tvRulesScore, tvExplanations)
    }

    private fun fetchDataAndCalculate(
        propertyId: String,
        pbLoading: ProgressBar,
        tvPropertyTitle: TextView,
        tvOverallScore: TextView,
        tvCategory: TextView,
        tvBudgetScore: TextView,
        tvLocationScore: TextView,
        tvRoomTypeScore: TextView,
        tvAmenitiesScore: TextView,
        tvRulesScore: TextView,
        tvExplanations: TextView
    ) {
        val userId = auth.currentUser?.uid ?: return
        pbLoading.visibility = View.VISIBLE

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val userProfile = userDoc.toObject(UserProfile::class.java)
                if (userProfile == null) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this, R.string.error_profile_missing, Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                db.collection("properties").document(propertyId).get()
                    .addOnSuccessListener { propDoc ->
                        val property = propDoc.toObject(Property::class.java)
                        pbLoading.visibility = View.GONE
                        if (property == null) {
                            Toast.makeText(this, R.string.error_loading_data, Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        calculateCompatibility(userProfile, property, tvPropertyTitle, tvOverallScore,
                            tvCategory, tvBudgetScore, tvLocationScore, tvRoomTypeScore,
                            tvAmenitiesScore, tvRulesScore, tvExplanations)
                    }
                    .addOnFailureListener {
                        pbLoading.visibility = View.GONE
                        Toast.makeText(this, R.string.error_loading_data, Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                pbLoading.visibility = View.GONE
                Toast.makeText(this, R.string.error_loading_data, Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateCompatibility(
        user: UserProfile,
        prop: Property,
        tvTitle: TextView,
        tvOverall: TextView,
        tvCat: TextView,
        tvBudget: TextView,
        tvLocation: TextView,
        tvRoomType: TextView,
        tvAmenities: TextView,
        tvRules: TextView,
        tvExplanations: TextView
    ) {
        tvTitle.text = prop.title

        val explanations = mutableListOf<String>()

        // 1. Budget (30%)
        val userBudget = user.budget.toDoubleOrNull() ?: 0.0
        val propRent = prop.rent.toDoubleOrNull() ?: 0.0
        var budgetScore = 0.0
        if (userBudget >= propRent) {
            budgetScore = 100.0
            explanations.add(getString(R.string.match_budget_ok))
        } else {
            val diff = propRent - userBudget
            budgetScore = when {
                diff <= 2000 -> {
                    explanations.add(getString(R.string.match_budget_partial))
                    70.0
                }
                diff <= 5000 -> {
                    explanations.add(getString(R.string.match_budget_bad))
                    40.0
                }
                else -> {
                    explanations.add(getString(R.string.match_budget_bad))
                    10.0
                }
            }
        }

        // 2. Location (25%)
        var locationScore = 0.0
        if (user.preferredLocation.contains(prop.location, ignoreCase = true) || 
            prop.location.contains(user.preferredLocation, ignoreCase = true)) {
            locationScore = 100.0
            explanations.add(getString(R.string.match_location_ok))
        } else {
            locationScore = 30.0
            explanations.add(getString(R.string.match_location_bad))
        }

        // 3. Room Type (15%)
        var roomTypeScore = 0.0
        if (user.roomType.contains(prop.roomType, ignoreCase = true) || 
            prop.roomType.contains(user.roomType, ignoreCase = true)) {
            roomTypeScore = 100.0
            explanations.add(getString(R.string.match_room_type_ok))
        } else {
            roomTypeScore = 0.0
            explanations.add(getString(R.string.match_room_type_bad))
        }

        // 4. Amenities (15%)
        val userAmenities = user.amenities.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val propAmenities = prop.amenities.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        var amenitiesScore = 0.0
        if (userAmenities.isNotEmpty()) {
            var matchCount = 0
            userAmenities.forEach { ua ->
                if (propAmenities.any { it.contains(ua) || ua.contains(it) }) {
                    matchCount++
                }
            }
            amenitiesScore = (matchCount.toDouble() / userAmenities.size) * 100.0
        } else {
            amenitiesScore = 100.0
        }

        // 5. Rules (15%)
        var rulesScore = 100.0
        var ruleConflict = false
        if (user.pets && prop.rules.contains("No Pets", ignoreCase = true)) {
            rulesScore -= 50.0
            explanations.add(getString(R.string.match_rules_pets))
            ruleConflict = true
        }
        if (user.smoking && prop.rules.contains("No Smoking", ignoreCase = true)) {
            rulesScore -= 50.0
            explanations.add(getString(R.string.match_rules_smoking))
            ruleConflict = true
        }
        rulesScore = max(0.0, rulesScore)
        if (!ruleConflict) explanations.add(getString(R.string.match_rules_ok))

        // Total Weighted Score
        val totalScore = (budgetScore * 0.30) + (locationScore * 0.25) + (roomTypeScore * 0.15) + 
                         (amenitiesScore * 0.15) + (rulesScore * 0.15)

        tvOverall.text = "${totalScore.toInt()}%"
        
        val category = when {
            totalScore >= 80 -> getString(R.string.cat_excellent)
            totalScore >= 60 -> getString(R.string.cat_good)
            totalScore >= 40 -> getString(R.string.cat_moderate)
            else -> getString(R.string.cat_poor)
        }
        tvCat.text = category

        tvBudget.text = "${getString(R.string.label_budget_match)}${budgetScore.toInt()}%"
        tvLocation.text = "${getString(R.string.label_location_match)}${locationScore.toInt()}%"
        tvRoomType.text = "${getString(R.string.label_room_type_match)}${roomTypeScore.toInt()}%"
        tvAmenities.text = "${getString(R.string.label_amenities_match)}${amenitiesScore.toInt()}%"
        tvRules.text = "${getString(R.string.label_rules_match)}${rulesScore.toInt()}%"

        tvExplanations.text = explanations.joinToString("\n")
    }
}
