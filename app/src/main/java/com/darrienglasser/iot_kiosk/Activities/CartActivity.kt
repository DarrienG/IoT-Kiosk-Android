package com.darrienglasser.iot_kiosk.Activities

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.edit
import com.darrienglasser.iot_kiosk.Adapters.ShopAdapter
import com.darrienglasser.iot_kiosk.Adapters.ShopItem
import com.darrienglasser.iot_kiosk.Adapters.moneyfi
import com.darrienglasser.iot_kiosk.Consts.BASE_URL
import com.darrienglasser.iot_kiosk.Model.RShopItem
import com.darrienglasser.iot_kiosk.Model.SnackModel
import com.darrienglasser.iot_kiosk.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_cart.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class CartActivity : AppCompatActivity() {

    lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sp = getDefaultSharedPreferences(applicationContext)
        val rawCart = sp.all
        if (rawCart.isEmpty()) {
            nothingInCart()
        } else {
            setUpForBuying(rawCart)
        }
        // Get information from all items from web
    }

    /**
     * Nothing in cart state
     */
    private fun nothingInCart() {
        buy_view.visibility = GONE
        nothing_view.visibility = VISIBLE
        nothing_text.text = getString(R.string.nothing_in_cart)
        loading_layout.visibility = GONE

        buy_button.isEnabled = false
        subtotal_text.text = String.format(getString(R.string.subtotal), moneyfi(0.00))
    }

    data class HolderThing(val name: String, val numItems: Int)

    /**
     * Stuff in the cart state.
     */
    private fun setUpForBuying(rawCart: MutableMap<String, *>?) {
        val itemList = mutableListOf<HolderThing>()
        sp.all.forEach {
            itemList.add(HolderThing(it.key, it.value as Int))
        }

        val cartItems = mutableListOf<ShopItem>()
        val numItems = mutableListOf<Int>()

        val r = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

        val sm = r.create(SnackModel::class.java)

        sm.getSnacks().enqueue(object : Callback<List<Map<String, RShopItem>>> {
            override fun onFailure(call: Call<List<Map<String, RShopItem>>>?, t: Throwable?) {
                Toast.makeText(applicationContext, "I want to die???", LENGTH_LONG).show()
            }

            override fun onResponse(call: Call<List<Map<String, RShopItem>>>, response: Response<List<Map<String, RShopItem>>>) {
                if (response.isSuccessful) {
                    val snackMap = mutableMapOf<String, ShopItem>()
                    val cartList = mutableListOf<ShopItem>()
                    response.body()?.let { l ->
                        l.forEach { m ->
                            m.forEach { e ->
                                snackMap[e.key] = ShopItem(e.key, e.value.price, e.value.image, e.value.id)
                            }
                        }

                        itemList.forEach { h ->
                            snackMap[h.name]?.let { si ->
                                cartList.add(si)
                                numItems.add(h.numItems)
                            }
                        }

                        initBuyViews(cartList, numItems)
                    }
                } else {
                    // no
                }
            }
        })

    }

    fun initBuyViews(cartItems: MutableList<ShopItem>, numItems: MutableList<Int>) {
        assert(cartItems.size == numItems.size)

        val sa = ShopAdapter(cartItems, true, applicationContext, numItems)
        sa.bcl = { si, i, ni ->
            sa.removeItem(i)
            sp.edit { remove(si.name) }
            if (sa.dataSet.isEmpty()) {
                nothingInCart()
                sp.edit().clear().apply()
            }
            var cash = subtotal_text.text.toString().substring(subtotal_text.text.indexOf('$') + 1).toDouble()
            cash -= si.price * ni
            if (cash < 0) cash = 0.00
            subtotal_text.text = String.format(getString(R.string.subtotal), moneyfi(cash))
        }

        buy_view.visibility = VISIBLE
        loading_layout.visibility = GONE

        cart_list.adapter = sa
        cart_list.layoutManager = GridLayoutManager(this,
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 1 else 2)

        var subtotal = 0.0
        for ((i, _) in cartItems.withIndex()) {
            subtotal += cartItems[i].price * numItems[i]
        }

        subtotal_text.text = String.format(getString(R.string.subtotal), moneyfi(subtotal))

        buy_button.isEnabled = true
        buy_button.setOnClickListener {
            buyItems(cartItems, numItems)
            Toast.makeText(applicationContext, "Ordering...", LENGTH_LONG).show()
        }
    }

    /**
     * Puts the view into a bought items state
     * Buys items from the cart, clears them from db, and
     */
    private fun buyItems(cartItems: MutableList<ShopItem>, numItems: MutableList<Int>) {
        // commit purchase to the internet

        buy_view.visibility = GONE
        nothing_view.visibility = VISIBLE
        nothing_text.text = getString(R.string.ordered)
        subtotal_text.text = String.format(getString(R.string.subtotal), moneyfi(0.00))


        val b = Buytem(mutableListOf(), "pending", FirebaseAuth.getInstance().uid ?: "FAILURE")
        for ((i, _) in cartItems.withIndex()) {
            b.items.add(PartialBuytem(numItems[i], "/products/${cartItems[i].name}"))
        }
        buyProducts(b)
        sp.edit().clear().apply()
        buy_button.isEnabled = false
    }

    private inline fun buyProducts(b: Buytem) {
        val fb = FirebaseFirestore.getInstance(FirebaseAuth.getInstance().app)
        fb.collection("orders").add(b)
    }
}

public data class PartialBuytem(val count: Int, val ref: String)
public data class Buytem(val items: MutableList<PartialBuytem>, val state: String, val user: String)
