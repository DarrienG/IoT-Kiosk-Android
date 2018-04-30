package com.darrienglasser.iot_kiosk.Adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.darrienglasser.iot_kiosk.R
import com.squareup.picasso.Picasso

class ShopAdapter(
        public val dataSet: MutableList<ShopItem>,
        private val inCart: Boolean,
        private val context: Context,
        public val numItems: MutableList<Int>? = null) : RecyclerView.Adapter<ShopAdapter.ViewHolder>() {

    init {
        if (inCart) {
            assert(numItems != null)
            numItems?.let {
                assert(dataSet.size == it.size)
            }
        }
    }

    public var scl: OnShopClickListener? = null

    public var bcl: ((ShopItem, Int, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.shop_row, parent, false))
    }

    override fun getItemCount(): Int = dataSet.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val price = moneyfi(dataSet[position].price)
        holder.nameView.text = dataSet[position].name

        Picasso.get()
                .load(dataSet[position].img)
                .into(holder.snackView)

        if (inCart) {
            if (bcl == null) throw RuntimeException("Remove not implemented!")
            if (numItems == null) throw RuntimeException("Number of items needs to be passed in for cart")

            holder.priceView.text = String.format(
                    context.getString(R.string.placeholder_price),
                    "$price * ${numItems[position]} = \$${moneyfi(dataSet[position].price * numItems[position])}")

            holder.cartRow.visibility = GONE
            holder.buyRow.visibility = VISIBLE
            holder.removeNow.setOnClickListener {
                bcl?.invoke(dataSet[position], position, numItems[position])
            }
        } else {

            if (scl == null) throw RuntimeException("Add to cart and buy now not defined")

            holder.priceView.text = String.format(
                    context.getString(R.string.placeholder_price),
                    price)

            scl?.let { cl ->
                holder.cartRow.visibility = VISIBLE
                holder.buyRow.visibility = GONE
                holder.addToCart.setOnClickListener{ cl.onCartClick(dataSet[position]) }
                holder.buyNow.setOnClickListener{ cl.onBuyClick(dataSet[position]) }
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameView: TextView = itemView.findViewById(R.id.name_view)
        val priceView: TextView = itemView.findViewById(R.id.price_view)
        val snackView: ImageView = itemView.findViewById(R.id.snack_view)
        val cartRow: LinearLayout = itemView.findViewById(R.id.cart_row)
        val addToCart: Button = itemView.findViewById(R.id.cart_button)
        val buyNow: Button = itemView.findViewById(R.id.buy_now_button)

        val buyRow: LinearLayout = itemView.findViewById(R.id.buy_row)
        val removeNow: Button = itemView.findViewById(R.id.remove_item)
    }

    public interface OnShopClickListener {
        fun onCartClick(si: ShopItem)
        fun onBuyClick(si: ShopItem)
    }

    public fun removeItem(position: Int) {
        dataSet.removeAt(position)
        numItems?.let {
            it.removeAt(position)
            assert(dataSet.size == it.size)
        }

        notifyItemRemoved(position)
        notifyItemRangeChanged(position, dataSet.size)
    }
}

public fun moneyfi(money: Double): String {
    var price = money.toString()
    if (price.length - price.indexOf(".") == 2) {
        price += "0"
    }
    return price.substring(0, price.indexOf('.') + 3)
}

public class ShopItem(val name: String, val price: Double, val img: String, val id: String)

