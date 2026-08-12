package com.nivetha.rentmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
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
    private val fullPropertyList = mutableListOf<Property>()

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

        // Filter UI Elements
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val etMinRent = findViewById<EditText>(R.id.etMinRent)
        val etMaxRent = findViewById<EditText>(R.id.etMaxRent)
        val spPropType = findViewById<Spinner>(R.id.spPropType)
        val spRoomType = findViewById<Spinner>(R.id.spRoomType)
        val spBedrooms = findViewById<Spinner>(R.id.spBedrooms)
        val btnApply = findViewById<Button>(R.id.btnApplyFilters)
        val btnClear = findViewById<Button>(R.id.btnClearFilters)

        rvProperties.layoutManager = LinearLayoutManager(this)
        adapter = PropertyAdapter(propertyList) { property ->
            val intent = Intent(this, PropertyDetailsActivity::class.java)
            intent.putExtra("PROPERTY_ID", property.id)
            startActivity(intent)
        }
        rvProperties.adapter = adapter

        fetchProperties(pbLoading, tvEmpty)

        checkUserRole(btnAddProperty)

        btnApply.setOnClickListener {
            applyFilters(etSearch, etMinRent, etMaxRent, spPropType, spRoomType, spBedrooms, tvEmpty)
        }

        btnClear.setOnClickListener {
            clearFilters(etSearch, etMinRent, etMaxRent, spPropType, spRoomType, spBedrooms, tvEmpty)
        }
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

                fullPropertyList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val property = doc.toObject(Property::class.java)
                        fullPropertyList.add(property)
                    }
                }

                showAllProperties(tvEmpty)
            }
    }

    private fun showAllProperties(tvEmpty: TextView) {
        propertyList.clear()
        propertyList.addAll(fullPropertyList)
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (propertyList.isEmpty()) View.VISIBLE else View.GONE
        if (propertyList.isEmpty()) tvEmpty.text = getString(R.string.msg_no_properties)
    }

    private fun applyFilters(
        etSearch: EditText, etMin: EditText, etMax: EditText,
        spType: Spinner, spRoom: Spinner, spBeds: Spinner,
        tvEmpty: TextView
    ) {
        val query = etSearch.text.toString().trim().lowercase()
        val minRent = etMin.text.toString().toDoubleOrNull() ?: 0.0
        val maxRent = etMax.text.toString().toDoubleOrNull() ?: Double.MAX_VALUE
        val typeFilter = spType.selectedItem.toString()
        val roomFilter = spRoom.selectedItem.toString()
        val bedsFilter = spBeds.selectedItem.toString()

        val filtered = fullPropertyList.filter { p ->
            val matchesSearch = p.title.lowercase().contains(query) || p.location.lowercase().contains(query)
            val rent = p.rent.toDoubleOrNull() ?: 0.0
            val matchesRent = rent >= minRent && rent <= maxRent
            val matchesType = typeFilter == "All Types" || p.propertyType.equals(typeFilter, ignoreCase = true)
            val matchesRoom = roomFilter == "All Rooms" || p.roomType.equals(roomFilter, ignoreCase = true)
            
            val matchesBeds = when (bedsFilter) {
                "All Beds" -> true
                "4+" -> p.bedrooms >= 4
                else -> p.bedrooms == (bedsFilter.toIntOrNull() ?: 0)
            }

            matchesSearch && matchesRent && matchesType && matchesRoom && matchesBeds
        }

        propertyList.clear()
        propertyList.addAll(filtered)
        adapter.notifyDataSetChanged()

        if (propertyList.isEmpty()) {
            tvEmpty.text = getString(R.string.msg_no_match)
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    private fun clearFilters(
        etSearch: EditText, etMin: EditText, etMax: EditText,
        spType: Spinner, spRoom: Spinner, spBeds: Spinner,
        tvEmpty: TextView
    ) {
        etSearch.text.clear()
        etMin.text.clear()
        etMax.text.clear()
        spType.setSelection(0)
        spRoom.setSelection(0)
        spBeds.setSelection(0)
        showAllProperties(tvEmpty)
    }
}
