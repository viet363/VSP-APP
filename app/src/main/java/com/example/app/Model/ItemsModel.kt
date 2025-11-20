package com.example.app.Model

import android.os.Parcel
import android.os.Parcelable


data class ItemsModel(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var picUrl: ArrayList<String> = ArrayList(),
    var model: ArrayList<String> = ArrayList(),
    var price: Long = 0L,
    var rating: Double = 0.0,
    var numberInCart: Int = 0,
    var showRecommended: Boolean = false,
    var categoryId: Long = 0L
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(), // Đọc id
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.createStringArrayList() as ArrayList<String>,
        parcel.createStringArrayList() as ArrayList<String>,
        parcel.readLong(), // Thay readDouble() thành readLong()
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readLong() // Thay readString() thành readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id) // Ghi id
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeStringList(picUrl)
        parcel.writeStringList(model)
        parcel.writeLong(price) // Thay writeDouble() thành writeLong()
        parcel.writeDouble(rating)
        parcel.writeInt(numberInCart)
        parcel.writeByte(if (showRecommended) 1 else 0)
        parcel.writeLong(categoryId) // Thay writeString() thành writeLong()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemsModel> {
        override fun createFromParcel(parcel: Parcel): ItemsModel {
            return ItemsModel(parcel)
        }

        override fun newArray(size: Int): Array<ItemsModel?> {
            return arrayOfNulls(size)
        }
    }
}