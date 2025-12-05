package com.example.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Helper.ChangeNumberItemsListener
import com.example.app.Helper.ManagmentCart
import com.example.app.Model.CartItemModel
import com.example.app.databinding.ViewholderCartBinding

class CartAdapter(
    private var listItemSelected: ArrayList<CartItemModel>,
    private val context: Context,
    private val listener: ChangeNumberItemsListener
) : RecyclerView.Adapter<CartAdapter.Viewholder>() {

    private val management = ManagmentCart(context)

    class Viewholder(val binding: ViewholderCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val binding = ViewholderCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val cartItem = listItemSelected[position]
        val item = cartItem.item

        holder.binding.titleTxt.text = item.title
        holder.binding.feeEachTime.text = formatMoney(item.price)
        holder.binding.totalEachItem.text = formatMoney(item.price * cartItem.quantity)
        holder.binding.numberItemTxt.text = cartItem.quantity.toString()

        Glide.with(context)
            .load(item.picUrl.firstOrNull())
            .into(holder.binding.pic)

        // ---------------- PLUS ----------------
        holder.binding.plusCartBtn.setOnClickListener {
            if (cartItem.cartDetailId != null && cartItem.cartDetailId != 0L) {
                // Cập nhật trên server
                management.updateCartServer(
                    cartItem.cartDetailId!!,
                    cartItem.quantity + 1
                ) { success, message ->
                    if (success) {
                        // Cập nhật local
                        cartItem.quantity++
                        management.updateLocalQuantity(item.id, cartItem.quantity)
                        notifyItemChanged(position)
                        listener.onChanged()
                    } else {
                        Toast.makeText(
                            context,
                            message ?: "Cập nhật số lượng thất bại",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                // Chỉ cập nhật local cho item chưa sync với server
                cartItem.quantity++
                management.updateLocalQuantity(item.id, cartItem.quantity)
                notifyItemChanged(position)
                listener.onChanged()
            }
        }

        // ---------------- MINUS ----------------
        holder.binding.minusCartBtn.setOnClickListener {
            if (cartItem.quantity == 1) {
                // Xóa item
                if (cartItem.cartDetailId != null && cartItem.cartDetailId != 0L) {
                    // Xóa trên server
                    management.deleteFromServer(cartItem.cartDetailId!!) { success, message ->
                        if (success) {
                            // Xóa local
                            management.removeFromLocal(item.id)
                            listItemSelected.removeAt(position)
                            notifyItemRemoved(position)
                            listener.onChanged()
                        } else {
                            Toast.makeText(
                                context,
                                message ?: "Xóa sản phẩm thất bại",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Chỉ xóa local
                    management.removeFromLocal(item.id)
                    listItemSelected.removeAt(position)
                    notifyItemRemoved(position)
                    listener.onChanged()
                }
            } else {
                // Giảm số lượng
                if (cartItem.cartDetailId != null && cartItem.cartDetailId != 0L) {
                    // Cập nhật trên server
                    management.updateCartServer(
                        cartItem.cartDetailId!!,
                        cartItem.quantity - 1
                    ) { success, message ->
                        if (success) {
                            // Cập nhật local
                            cartItem.quantity--
                            management.updateLocalQuantity(item.id, cartItem.quantity)
                            notifyItemChanged(position)
                            listener.onChanged()
                        } else {
                            Toast.makeText(
                                context,
                                message ?: "Cập nhật số lượng thất bại",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Chỉ cập nhật local
                    cartItem.quantity--
                    management.updateLocalQuantity(item.id, cartItem.quantity)
                    notifyItemChanged(position)
                    listener.onChanged()
                }
            }
        }
    }

    override fun getItemCount(): Int = listItemSelected.size

    private fun formatMoney(amount: Double): String {
        return "%,d đ".format(amount.toInt())
    }
}