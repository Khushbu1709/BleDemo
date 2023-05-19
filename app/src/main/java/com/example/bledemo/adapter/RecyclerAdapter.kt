package com.example.bluethoothlistgetdemo.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bledemo.databinding.ItemRecyclerBinding
import com.example.bledemo.model.BluetoothModel

class RecyclerAdapter(
    private val context: Context,
    private var bluetoothModel: ArrayList<BluetoothModel>,
    private var callback: (position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemRecyclerBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.binding.txtPosition.text = "$position"
        holder.binding.name.text = bluetoothModel[position].name
        holder.binding.address.text = bluetoothModel[position].address
        holder.setIsRecyclable(false)


        holder.binding.cardView.setOnClickListener {
            callback.invoke(position)
        }
    }

    override fun getItemCount(): Int = bluetoothModel.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class MyViewHolder(val binding: ItemRecyclerBinding) :
        RecyclerView.ViewHolder(binding.root)
}
