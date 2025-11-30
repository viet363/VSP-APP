package com.example.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project1762.Helper.ManagmentCart
import com.example.project1762.Helper.ChangeNumberItemsListener
import com.example.app.Model.ItemsModel
import com.example.app.databinding.ViewholderCartBinding

class CartAdapter(
    private var listItemSelected: ArrayList<ItemsModel>,
    private val context: Context,
    private val changeNumberItemsListener: ChangeNumberItemsListener
) : RecyclerView.Adapter<CartAdapter.Viewholder>() {

    class Viewholder(val binding: ViewholderCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val managmentCart = ManagmentCart(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val binding = ViewholderCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = listItemSelected[position]

        holder.binding.titleTxt.text = item.title
        holder.binding.feeEachTime.text = formatMoney(item.price)
        holder.binding.totalEachItem.text = formatMoney(item.numberInCart * item.price)
        holder.binding.numberItemTxt.text = item.numberInCart.toString()

        Glide.with(context)
            .load(item.picUrl.firstOrNull())
            .into(holder.binding.pic)

        // --- PLUS ---
        holder.binding.plusCartBtn.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val listener = object : ChangeNumberItemsListener {
                    override fun onChanged() {
                        notifyItemChanged(currentPosition)
                        changeNumberItemsListener.onChanged()
                    }
                }
                managmentCart.plusItem(listItemSelected, currentPosition, listener)
            }
        }

        // --- MINUS ---
        holder.binding.minusCartBtn.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val listener = object : ChangeNumberItemsListener {
                    override fun onChanged() {
                        notifyItemChanged(currentPosition)
                        changeNumberItemsListener.onChanged()
                    }
                }
                managmentCart.minusItem(listItemSelected, currentPosition, listener)
            }
        }

        // --- DELETE ---
        holder.binding.btndelte.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val listener = object : ChangeNumberItemsListener {
                    override fun onChanged() {
                        // Sử dụng vị trí hiện tại từ holder
                        listItemSelected.removeAt(currentPosition)
                        notifyItemRemoved(currentPosition)
                        notifyItemRangeChanged(currentPosition, listItemSelected.size)
                        changeNumberItemsListener.onChanged()
                    }
                }
                managmentCart.deleteItem(listItemSelected, currentPosition, listener)
            }
        }
    }

    override fun getItemCount(): Int = listItemSelected.size

    private fun formatMoney(amount: Double): String {
        return "%,d đ".format(amount.toInt())
    }
}