package com.darrienglasser.iot_kiosk.Activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.edit
import com.darrienglasser.iot_kiosk.Adapters.ShopAdapter
import com.darrienglasser.iot_kiosk.Adapters.ShopItem
import com.darrienglasser.iot_kiosk.Consts.BASE_URL
import com.darrienglasser.iot_kiosk.Model.RShopItem
import com.darrienglasser.iot_kiosk.Model.SnackModel
import com.darrienglasser.iot_kiosk.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_shop.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ShopActivity : AppCompatActivity() {

    lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        sp = getDefaultSharedPreferences(applicationContext)

        val r = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

        val sm = r.create(SnackModel::class.java)
        sm.getSnacks().enqueue(object : Callback<List<Map<String, RShopItem>>> {
            override fun onFailure(call: Call<List<Map<String, RShopItem>>>?, t: Throwable?) {
                // no
            }

            override fun onResponse(call: Call<List<Map<String, RShopItem>>>, response: Response<List<Map<String, RShopItem>>>) {
                if (response.isSuccessful) {
                    val snackList = mutableListOf<ShopItem>()
                    response.body()?.let { l ->
                        l.forEach { m ->
                            m.forEach { e ->
                                snackList.add(ShopItem(e.key, e.value.price, e.value.image, e.value.id))
                            }
                        }
                    }
                    setUpWithData(snackList)
                } else {
                    // no
                }
            }
        })
    }

    fun setUpWithData(snackList: MutableList<ShopItem>) {
        val sa = ShopAdapter(snackList, false, applicationContext)
        sa.scl = object : ShopAdapter.OnShopClickListener {
            override fun onCartClick(si: ShopItem) {
                sp.edit {
                    putInt(si.name, sp.getInt(si.name, 0) + 1)
                }
                Toast.makeText(applicationContext, "Added ${si.name} to cart!", LENGTH_LONG).show()
            }

            override fun onBuyClick(si: ShopItem) {
                val b = Buytem(mutableListOf(PartialBuytem(1, "/products/${si.name}")), "pending", FirebaseAuth.getInstance().uid ?: "FAILURE")
                val fb = FirebaseFirestore.getInstance(FirebaseAuth.getInstance().app)
                fb.collection("orders").add(b)
                Toast.makeText(applicationContext,
                        "Thanks for buying ${si.name}! Your purchase has been ordered",
                        LENGTH_LONG).show()
            }
        }

        loading_layout.visibility = GONE
        snack_list.visibility = VISIBLE

        snack_list.adapter = sa
        snack_list.layoutManager = GridLayoutManager(this,
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 1 else 2)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.shopping_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cart -> startActivity(Intent(this, CartActivity::class.java))
            else -> return false
        }
        return true
    }
}
