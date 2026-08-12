package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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

class PropertyListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: PropertyAdapter
    private val propertyList = mutableListOf<Property>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_property_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val rvProperties = findViewById<RecyclerView>(R.id.rvProperties)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val btnAddProperty = findViewById<Button>(R.id.btnAddProperty)

        rvProperties.layoutManager = LinearLayoutManager(this)
        adapter = PropertyAdapter(propertyList) { property ->
            val intent = Intent(this, PropertyDetailsActivity::class.java)
            intent.putExtra("PROPERTY_ID", property.id)
            startActivity(intent)
        }
        rvProperties.adapter = adapter

        fetchProperties(pbLoading, tvEmpty)

        checkUserRole(btnAddProperty)
    }

    private fun checkUserRole(btn: Button) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.getString("role") == "Landlord") {
                    btn.visibility = View.VISIBLE
                    btn.setOnClickListener {
                        startActivity(Intent(this, AddPropertyActivity::class.java))
                    }
                }
            }
    }

    private fun fetchProperties(pbLoading: ProgressBar, tvEmpty: TextView) {
        pbLoading.visibility = View.VISIBLE
        db.collection("properties")
            .addSnapshotListener { snapshots, e ->
                pbLoading.visibility = View.GONE
                if (e != null) {
                    Toast.makeText(this, "Error fetching properties", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                propertyList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val property = doc.toObject(Property::class.java)
                        propertyList.add(property)
                    }
                }

                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (propertyList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
}
