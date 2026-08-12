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

class LandlordTenantCompatibilityActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landlord_tenant_compatibility)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val propertyId = intent.getStringExtra("PROPERTY_ID") ?: return

        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvName = findViewById<TextView>(R.id.tvLandlordName)
        val tvOverall = findViewById<TextView>(R.id.tvOverallLandlordScore)
        val tvCat = findViewById<TextView>(R.id.tvLandlordCategory)
        val tvBudget = findViewById<TextView>(R.id.tvBudgetScore)
        val tvLease = findViewById<TextView>(R.id.tvLeaseScore)
        val tvRules = findViewById<TextView>(R.id.tvRulesScore)
        val tvLifestyle = findViewById<TextView>(R.id.tvLifestyleScore)
        val tvComm = findViewById<TextView>(R.id.tvCommScore)
        val tvLocation = findViewById<TextView>(R.id.tvLocationScore)
        val tvExplanations = findViewById<TextView>(R.id.tvExplanations)

        fetchDataAndCalculate(propertyId, pbLoading, tvName, tvOverall, tvCat, tvBudget, tvLease,
            tvRules, tvLifestyle, tvComm, tvLocation, tvExplanations)
    }

    private fun fetchDataAndCalculate(
        propertyId: String,
        pbLoading: ProgressBar,
        tvName: TextView,
        tvOverall: TextView,
        tvCat: TextView,
        tvBudget: TextView,
        tvLease: TextView,
        tvRules: TextView,
        tvLifestyle: TextView,
        tvComm: TextView,
        tvLocation: TextView,
        tvExplanations: TextView
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        pbLoading.visibility = View.VISIBLE

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { tenantDoc ->
                val tenant = tenantDoc.toObject(UserProfile::class.java)
                if (tenant == null) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this, R.string.error_profile_missing, Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                db.collection("properties").document(propertyId).get()
                    .addOnSuccessListener { propDoc ->
                        val property = propDoc.toObject(Property::class.java)
                        if (property == null) {
                            pbLoading.visibility = View.GONE
                            Toast.makeText(this, "Property not found", Toast.LENGTH_SHORT).show()
                            finish()
                            return@addOnSuccessListener
                        }

                        db.collection("users").document(property.ownerId).get()
                            .addOnSuccessListener { ownerDoc ->
                                val landlord = ownerDoc.toObject(UserProfile::class.java)
                                pbLoading.visibility = View.GONE
                                if (landlord == null) {
                                    Toast.makeText(this, R.string.error_landlord_missing, Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                calculateCompatibility(tenant, property, landlord, tvName, tvOverall, 
                                    tvCat, tvBudget, tvLease, tvRules, tvLifestyle, tvComm, tvLocation, tvExplanations)
                            }
                    }
            }
    }

    private fun calculateCompatibility(
        tenant: UserProfile,
        prop: Property,
        landlord: UserProfile,
        tvName: TextView,
        tvOverall: TextView,
        tvCat: TextView,
        tvBudget: TextView,
        tvLease: TextView,
        tvRules: TextView,
        tvLifestyle: TextView,
        tvComm: TextView,
        tvLocation: TextView,
        tvExplanations: TextView
    ) {
        tvName.text = landlord.fullName

        val explanations = mutableListOf<String>()

        // 1. Budget / Rent (25%)
        val tenantBudget = tenant.budget.toDoubleOrNull() ?: 0.0
        val propRent = prop.rent.toDoubleOrNull() ?: 0.0
        var budgetScore = 0.0
        if (tenantBudget >= propRent) {
            budgetScore = 100.0
            explanations.add(getString(R.string.match_budget_ok))
        } else {
            val diff = propRent - tenantBudget
            budgetScore = when {
                diff <= 2000 -> {
                    explanations.add(getString(R.string.match_budget_partial))
                    70.0
                }
                else -> {
                    explanations.add(getString(R.string.match_budget_bad))
                    30.0
                }
            }
        }

        // 2. Lease Duration (20%)
        var leaseScore = 0.0
        if (tenant.leaseDuration.isNullOrEmpty() || prop.leaseDuration.isNullOrEmpty()) {
            leaseScore = 80.0
            explanations.add("${getString(R.string.hint_lease_duration)}: ${getString(R.string.not_specified)} (Fair match)")
        } else if (tenant.leaseDuration.contains(prop.leaseDuration, ignoreCase = true) || 
            prop.leaseDuration.contains(tenant.leaseDuration, ignoreCase = true)) {
            leaseScore = 100.0
            explanations.add(getString(R.string.match_lease_ok))
        } else {
            leaseScore = 50.0
            explanations.add(getString(R.string.match_lease_similar))
        }

        // 3. Property Rules (20%)
        var rulesScore = 100.0
        if (tenant.smoking && prop.rules.contains("No Smoking", ignoreCase = true)) {
            rulesScore -= 50.0
            explanations.add(getString(R.string.match_rules_smoking))
        } else if (!tenant.smoking) {
            explanations.add(getString(R.string.match_rules_smoke_ok))
        }

        if (tenant.pets && prop.rules.contains("No Pets", ignoreCase = true)) {
            rulesScore -= 50.0
            explanations.add(getString(R.string.match_rules_pets))
        } else {
            explanations.add(getString(R.string.match_rules_pets_ok))
        }
        rulesScore = max(0.0, rulesScore)

        // 4. Lifestyle (15%)
        // Comparing cleanliness as a proxy for lifestyle match
        var lifestyleScore = 0.0
        if (landlord.cleanliness == 0) {
            lifestyleScore = 80.0
            explanations.add("Landlord lifestyle info not specified (Fair match)")
        } else {
            val cleanDiff = abs(tenant.cleanliness - landlord.cleanliness)
            lifestyleScore = (5 - cleanDiff) / 5.0 * 100.0
            if (lifestyleScore >= 80) explanations.add(getString(R.string.match_clean_similar))
        }

        // 5. Preferences / Communication (10%)
        // Matching sleep schedule as a proxy for communication/preference match
        val commScore = if (landlord.sleepSchedule.isEmpty()) 80.0 
                        else if (tenant.sleepSchedule == landlord.sleepSchedule) 100.0 
                        else 60.0

        // 6. Location (10%)
        var locationScore = 0.0
        if (tenant.preferredLocation.contains(prop.location, ignoreCase = true)) {
            locationScore = 100.0
            explanations.add(getString(R.string.match_location_ok))
        } else {
            locationScore = 40.0
            explanations.add(getString(R.string.match_location_bad))
        }

        // Weighted Overall Score
        val totalScore = (budgetScore * 0.25) + (leaseScore * 0.20) + (rulesScore * 0.20) +
                         (lifestyleScore * 0.15) + (commScore * 0.10) + (locationScore * 0.10)

        tvOverall.text = "${totalScore.toInt()}%"
        
        val category = when {
            totalScore >= 80 -> getString(R.string.cat_excellent)
            totalScore >= 60 -> getString(R.string.cat_good)
            totalScore >= 40 -> getString(R.string.cat_moderate)
            else -> getString(R.string.cat_poor)
        }
        tvCat.text = category

        tvBudget.text = "Budget/Rent Match: ${budgetScore.toInt()}%"
        tvLease.text = "${getString(R.string.label_lease_match)}${leaseScore.toInt()}%"
        tvRules.text = "${getString(R.string.label_rules_match)}${rulesScore.toInt()}%"
        tvLifestyle.text = "Lifestyle Match: ${lifestyleScore.toInt()}%"
        tvComm.text = "${getString(R.string.label_comm_match)}${commScore.toInt()}%"
        tvLocation.text = "${getString(R.string.label_location_match)}${locationScore.toInt()}%"

        tvExplanations.text = explanations.joinToString("\n")
    }
}
