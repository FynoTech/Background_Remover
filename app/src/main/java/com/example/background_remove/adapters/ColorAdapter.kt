import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setSafeClickListenertools

import com.photo.editor.background.eraser.bgeraser.R

class ColorAdapter(
    private val colors: List<Int?>,
    private val onColorClick: (Int?) -> Unit  // callback for clicked color
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.colorImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]

        if (position == 0) {
            holder.imageView.setImageDrawable(holder.itemView.context.getDrawable(R.drawable.noback))
            holder.imageView.setBackgroundColor(Color.TRANSPARENT)
        } else {
            color?.let {
                holder.imageView.setBackgroundColor(it)
            }
        }

        // Click listener
        holder.itemView.setSafeClickListener(holder.itemView.context) {
            onColorClick(colors[position]) // pass clicked color back
        }
    }

    override fun getItemCount(): Int = colors.size
}
