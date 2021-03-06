package com.davidferrand.pagingimagegallery

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.annotation.Px
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.davidferrand.pagingimagegallery.databinding.ActivityGridBinding

class GridActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_POSITION = "EXTRA_POSITION"
        const val EXTRA_IMAGES = "EXTRA_IMAGES"
    }

    private lateinit var binding: ActivityGridBinding

    private val implementations = listOf(
        CarouselImplementation(
            "vfinal",
            com.davidferrand.pagingimagegallery.recyclerview.vfinal.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v5-final: show overlay when prominent",
            com.davidferrand.pagingimagegallery.recyclerview.v5final.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v4-final: tap to scroll to center",
            com.davidferrand.pagingimagegallery.recyclerview.v4final.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v4-alpha1: tap to scroll",
            com.davidferrand.pagingimagegallery.recyclerview.v4alpha1.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v3-final: scale animation, pre-load extra items",
            com.davidferrand.pagingimagegallery.recyclerview.v3final.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v3-alpha3: scale animation, maintain spacing better",
            com.davidferrand.pagingimagegallery.recyclerview.v3alpha3.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v3-alpha2: scale animation, maintain spacing",
            com.davidferrand.pagingimagegallery.recyclerview.v3alpha2.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v3-alpha1: scale animation",
            com.davidferrand.pagingimagegallery.recyclerview.v3alpha1.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v2-final: paging, fix initial scroll",
            com.davidferrand.pagingimagegallery.recyclerview.v2final.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v2-alpha2: paging, center first/last items",
            com.davidferrand.pagingimagegallery.recyclerview.v2alpha2.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v2-alpha1: paging",
            com.davidferrand.pagingimagegallery.recyclerview.v2alpha1.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v1-final: aspect ratio, limited fixed width",
            com.davidferrand.pagingimagegallery.recyclerview.v1final.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v1-alpha2: aspect ratio, fixed width",
            com.davidferrand.pagingimagegallery.recyclerview.v1alpha2.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v1-alpha1: aspect ratio, width=wrap_content",
            com.davidferrand.pagingimagegallery.recyclerview.v1alpha1.CarouselActivity::class.java
        ),
        CarouselImplementation(
            "v0: basic implementation",
            com.davidferrand.pagingimagegallery.recyclerview.v0.CarouselActivity::class.java
        )
    )
    private var selectedImplementation: CarouselImplementation =
        implementations[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGridBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding.detailSelector) {
            adapter = ArrayAdapter(
                this@GridActivity,
                android.R.layout.simple_spinner_dropdown_item,
                implementations.map { it.label }
            )

            onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedImplementation = implementations[position]
                    }
                }
        }

        with(binding.recyclerView) {
            layoutManager = GridLayoutManager(this@GridActivity, 5)
            addItemDecoration(
                GridSpacingDecoration(resources.getDimensionPixelSize(R.dimen.grid_spacing))
            )

            reloadImages()
        }
    }

    /** Display a random set of images each time */
    private fun reloadImages() {
        val images: ArrayList<Image> = ArrayList(Data.images.shuffled())
        binding.recyclerView.swapAdapter(GridAdapter(images) { _, position ->
            startActivity(
                Intent(this@GridActivity, selectedImplementation.clazz)
                    .putExtra(EXTRA_TITLE, selectedImplementation.label)
                    .putParcelableArrayListExtra(EXTRA_IMAGES, images)
                    .putExtra(EXTRA_POSITION, position)
            )
        }, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.grid_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_shuffle -> {
                reloadImages()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }
}

/** Works only with a [GridLayoutManager] */
class GridSpacingDecoration(
    @Px private val spacing: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        child: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val itemPosition = parent.getChildAdapterPosition(child)
        val itemCount = parent.adapter?.itemCount ?: 0

        val lm = parent.layoutManager as GridLayoutManager
        val spanCount = lm.spanCount
        val spanIndex = lm.spanSizeLookup.getSpanIndex(itemPosition, spanCount)

        val isTopRow = itemPosition < spanCount

        val bottomRowIsComplete = itemCount % spanCount == 0
        val isBottomRow =
            if (bottomRowIsComplete) itemPosition >= itemCount - spanCount else itemPosition >= itemCount - (itemCount % spanCount)

        outRect.top = if (isTopRow) spacing else spacing / 2
        outRect.bottom = if (isBottomRow) spacing else spacing / 2
        outRect.left = (spacing * ((spanCount - spanIndex) / spanCount.toFloat())).toInt()
        outRect.right = (spacing * ((spanIndex + 1) / spanCount.toFloat())).toInt()
    }
}

class GridAdapter(
    private val images: List<Image>,
    private val onImageClicked: (Image, Int) -> Unit
) : RecyclerView.Adapter<GridAdapter.VH>() {
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(parent)

    override fun onBindViewHolder(holder: VH, position: Int) {
        val image = images[position]

        Glide.with(holder.imageView)
            .load(image.url)
            .transition(withCrossFade())
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            onImageClicked(image, position)
        }
    }

    override fun getItemCount(): Int = images.size

    override fun getItemId(position: Int): Long {
        return images[position].url.hashCode().toLong()
    }

    class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_grid_image, parent, false)
    ) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
    }
}

data class CarouselImplementation(
    val label: String,
    val clazz: Class<out Activity>
)