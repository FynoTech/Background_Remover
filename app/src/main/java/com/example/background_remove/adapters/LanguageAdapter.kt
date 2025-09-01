package com.example.background_remove.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.background_remove.R
import com.example.background_remove.utils.LanguageModel
import com.example.background_remove.utils.setSafeClickListener


class LanguageAdapter(
    private val context: Context,
    private val listData: List<LanguageModel>,
    var notifyLanguage: NotifyLanguage
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {
    var pref: SharedPreferences
    var currentLanguageCode: String? = null
    var selectedindex = -1

    init {
        pref = context.getSharedPreferences("Languages", Context.MODE_PRIVATE)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.language_sample_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        if (currentLanguageCode == listData[position].code) {
            holder.card.background = context.getDrawable(R.drawable.langselect)
        } else {
            holder.card.background = context.getDrawable(R.drawable.langunselect)
        }
        holder.card.setSafeClickListener(holder.itemView.context) {
            notifyItemChanged(selectedindex)
            selectedindex = position
            notifyLanguage.notifyLanguage(
                listData[position].flag, listData[position].name, listData[position].code, position
            )

        }
        holder.countryName.text = listData[position].name
        holder.countryFlag.setImageResource(listData[position].flag)
    }

    override fun getItemCount(): Int {

        return listData.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val countryName: TextView
        val countryFlag: ImageView
        val card: LinearLayout

        init {
            countryName = itemView.findViewById<View>(R.id.tvlanguageName) as TextView
            countryFlag = itemView.findViewById<View>(R.id.languageIcon) as ImageView
            card = itemView.findViewById<View>(R.id.Item) as LinearLayout
        }
    }

    fun SelectedLanguage(code: String?, position: Int) {
        Log.d("abcd234", "SelectedLanguage:${position} ")

        selectedindex = position
        currentLanguageCode = code
        if (position != -1) {
            notifyItemChanged(position)
        } else {
            notifyDataSetChanged()
        }
    }

    interface NotifyLanguage {
        fun notifyLanguage(flag: Int, name: String?, code: String?, position: Int)
    }
}
