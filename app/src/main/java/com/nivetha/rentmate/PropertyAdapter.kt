package com.nivetha.rentmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PropertyAdapter(
    private val properties: List<Property>,
    private val onItemClick: (Property) -> Unit
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvPropertyTitle)
        val tvLocation: TextView = view.findViewById(R.id.tvPropertyLocation)
        val tvRent: TextView = view.findViewById(R.id.tvPropertyRent)
        val tvBHK: TextView = view.findViewById(R.id.tvPropertyBHK)
        val tvType: TextView = view.findViewById(R.id.tvPropertyType)
        val tvRoom: TextView = view.findViewById(R.id.tvPropertyRoom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = properties[position]
        holder.tvTitle.text = property.title
        holder.tvLocation.text = property.location
        holder.tvRent.text = "₹${property.rent}"
        
        holder.tvBHK.text = "${property.bedrooms} BHK"
        holder.tvType.text = property.propertyType
        holder.tvRoom.text = property.roomType
        
        holder.itemView.setOnClickListener { onItemClick(property) }
    }

    override fun getItemCount() = properties.size
}
