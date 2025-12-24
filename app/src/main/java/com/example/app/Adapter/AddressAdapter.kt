package com.example.app.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.app.Model.UserAddressModel
import com.example.app.databinding.ItemAddressBinding

class AddressAdapter(
    private val addresses: List<UserAddressModel>,
    private val onAddressSelected: (UserAddressModel) -> Unit,
    private val onEditAddress: (UserAddressModel) -> Unit,
    private val onDeleteAddress: (UserAddressModel) -> Unit
) : RecyclerView.Adapter<AddressAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAddressBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAddressBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val address = addresses[position]

        holder.binding.apply {
            // Set receiver name
            receiverName.text = address.receiverName ?: "Người nhận"

            // Set phone
            phone.text = address.phone ?: "Chưa có số điện thoại"

            // Set address detail
            addressDetail.text = address.addressDetail

            // Show default badge - SỬA: dùng isDefaultBoolean
            defaultBadge.visibility = if (address.isDefaultBoolean) View.VISIBLE else View.GONE

            // Set click listeners
            selectButton.setOnClickListener {
                onAddressSelected(address)
            }

            editButton.setOnClickListener {
                onEditAddress(address)
            }

            deleteButton.setOnClickListener {
                onDeleteAddress(address)
            }
        }
    }

    override fun getItemCount(): Int = addresses.size
}