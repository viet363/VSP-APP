package com.example.project1762.Helper

import android.content.Context
import com.example.app.Helper.TinyDB
import com.example.app.Model.ItemsModel

class ManagmentCart(context: Context) {

    private val tinyDB = TinyDB(context)

    fun insertItem(item: ItemsModel) {
        val list = getListCart()
        val index = list.indexOfFirst { it.id == item.id }

        if (index >= 0) {
            list[index].numberInCart += item.numberInCart
        } else {
            list.add(item)
        }

        tinyDB.putListObject("CartList", list)
    }

    fun getListCart(): ArrayList<ItemsModel> {
        return tinyDB.getListObject("CartList")
    }

    fun setCartList(list: ArrayList<ItemsModel>) {
        tinyDB.putListObject("CartList", list)
    }

    fun plusItem(list: ArrayList<ItemsModel>, position: Int, listener: ChangeNumberItemsListener) {
        list[position].numberInCart++
        setCartList(list)
        listener.onChanged()
    }

    fun minusItem(list: ArrayList<ItemsModel>, position: Int, listener: ChangeNumberItemsListener) {
        if (list[position].numberInCart > 1) {
            list[position].numberInCart--
        } else {
            list.removeAt(position)
        }
        setCartList(list)
        listener.onChanged()
    }

    fun deleteItem(list: ArrayList<ItemsModel>, position: Int, listener: ChangeNumberItemsListener) {
        list.removeAt(position)
        setCartList(list)
        listener.onChanged()
    }

    fun getTotalFee(): Double {
        return getListCart().sumOf { it.price * it.numberInCart }
    }
}
