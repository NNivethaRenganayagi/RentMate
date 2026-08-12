package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoommateListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: RoommateAdapter
    private val roommateList = mutableListOf<UserProfile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_roommate_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val rvRoommates = findViewById<RecyclerView>(R.id.rvRoommates)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyRoommates)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoadingRoommates)

        rvRoommates.layoutManager = LinearLayoutManager(this)
        adapter = RoommateAdapter(roommateList) { selectedUser ->
            // In a real app we'd need the UID. Let's assume we can get it from a field or by searching again.
            // For now, let's find the UID by searching for this specific name/location combo to simplify for MAD project.
            // Better: Add uid to UserProfile data class.
            findUidAndNavigate(selectedUser)
        }
        rvRoommates.adapter = adapter

        fetchRoommates(pbLoading, tvEmpty)
    }

    private fun fetchRoommates(pbLoading: ProgressBar, tvEmpty: TextView) {
        val currentUserId = auth.currentUser?.uid ?: return
        pbLoading.visibility = View.VISIBLE

        db.collection("users")
            .whereEqualTo("role", "Tenant")
            .get()
            .addOnSuccessListener { snapshots ->
                pbLoading.visibility = View.GONE
                roommateList.clear()
                for (doc in snapshots) {
                    if (doc.id != currentUserId) {
                        val roommate = doc.toObject(UserProfile::class.java)
                        roommateList.add(roommate)
                    }
                }
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (roommateList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                pbLoading.visibility = View.GONE
                Toast.makeText(this, "Error loading roommates", Toast.LENGTH_SHORT).show()
            }
    }

    private fun findUidAndNavigate(user: UserProfile) {
        // Since UserProfile doesn't have a UID field yet, we search for it.
        // In a production app, UID should be part of the model.
        db.collection("users")
            .whereEqualTo("fullName", user.fullName)
            .whereEqualTo("location", user.location)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val uid = documents.documents[0].id
                    val intent = Intent(this, RoommateCompatibilityActivity::class.java)
                    intent.putExtra("SELECTED_USER_UID", uid)
                    intent.putExtra("SELECTED_USER_NAME", user.fullName)
                    startActivity(intent)
                }
            }
    }
}
