package com.nivetha.rentmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RoommateAdapter(
    private val roommates: List<UserProfile>,
    private val onItemClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<RoommateAdapter.RoommateViewHolder>() {

    class RoommateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvRoommateName)
        val tvAge: TextView = view.findViewById(R.id.tvRoommateAge)
        val tvInfo: TextView = view.findViewById(R.id.tvRoommateInfo)
        val tvLifestyle: TextView = view.findViewById(R.id.tvRoommateLifestyle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoommateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_roommate, parent, false)
        return RoommateViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoommateViewHolder, position: Int) {
        val roommate = roommates[position]
        holder.tvName.text = roommate.fullName
        holder.tvAge.text = "${roommate.age} years"
        holder.tvInfo.text = "${roommate.location} | Budget: ₹${roommate.budget}"
        holder.tvLifestyle.text = "${roommate.sleepSchedule} • ${roommate.foodPreference}"
        
        holder.itemView.setOnClickListener { onItemClick(roommate) }
    }

    override fun getItemCount() = roommates.size
}
