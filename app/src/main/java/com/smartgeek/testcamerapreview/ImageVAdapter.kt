package com.smartgeek.testcamerapreview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions

class ImageVAdapter(private val imageList: List<ImageDetails>,private val onItemClick: (ImageDetails) -> Unit): RecyclerView.Adapter<ImageVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.frame_display, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Glide.with(holder.itemView.context)
            .load(imageList[position].frameid)
            .placeholder(R.drawable.ic_launcher_background)
            .apply(RequestOptions.bitmapTransform(CircleCrop()))
            .into(holder.displayFrame)

        holder.itemView.setOnClickListener {
            onItemClick.invoke(imageList[position])
        }
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var displayFrame: ImageView = itemView.findViewById(R.id.iv_frame_image)
    }

}