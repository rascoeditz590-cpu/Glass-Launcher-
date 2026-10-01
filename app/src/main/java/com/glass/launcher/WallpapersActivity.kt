package com.glass.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.WallpaperManager
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class WallpapersActivity : Activity() {

    private val bg = Color.parseColor("#060B1F")
    private val accent = Color.parseColor("#3B82F6")
    private val tabViews = mutableMapOf<String, TextView>()
    private lateinit var grid: GridView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(0, dp(40), 0, 0)
        }

        root.addView(TextView(this).apply {
            text = "Wallpapers"
            setTextColor(Color.WHITE)
            textSize = 28f
            setPadding(dp(20), dp(8), dp(20), dp(12))
        })

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(12))
        }
        WallpaperCatalog.categories.forEach { cat ->
            val tv = TextView(this).apply {
                text = cat
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(10), dp(20), dp(10))
                setOnClickListener { select(cat) }
            }
            tabViews[cat] = tv
            tabs.addView(tv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = dp(8) })
        }
        root.addView(tabs)

        grid = GridView(this).apply {
            numColumns = 2
            horizontalSpacing = dp(12)
            verticalSpacing = dp(12)
            setPadding(dp(16), dp(4), dp(16), dp(16))
            clipToPadding = false
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        }
        root.addView(grid, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        select("All")
    }

    private fun select(cat: String) {
        tabViews.forEach { (name, tv) ->
            val on = name == cat
            tv.setTextColor(if (on) Color.WHITE else Color.parseColor("#9FB0D9"))
            tv.background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(if (on) accent else Color.parseColor("#141C3A"))
                setStroke(dp(1), Color.parseColor("#2A3560"))
            }
        }
        grid.adapter = WallAdapter(WallpaperCatalog.byCategory(cat))
    }

    private inner class WallAdapter(val items: List<WallpaperItem>) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = items[position]
            val w = (resources.displayMetrics.widthPixels - dp(16) * 2 - dp(12)) / 2
            val h = (w * 16f / 9f).toInt()

            val card = FrameLayout(this@WallpapersActivity).apply {
                layoutParams = AbsListView.LayoutParams(w, h)
                isClickable = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, dp(20).toFloat())
                    }
                }
                clipToOutline = true
            }
            card.addView(ImageView(this@WallpapersActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(item.thumb)
            }, FrameLayout.LayoutParams(-1, -1))
            card.addView(TextView(this@WallpapersActivity).apply {
                text = item.name
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(dp(12), dp(24), dp(12), dp(10))
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.TRANSPARENT, Color.parseColor("#CC000000"))
                )
            }, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

            // touch animation: press par halka dab ke wapas aata hai
            card.setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(120).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
                }
                false
            }
            card.setOnClickListener { preview(item) }
            return card
        }
    }

    private fun preview(item: WallpaperItem) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        frame.addView(ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(item.full)
        }, FrameLayout.LayoutParams(-1, -1))

        val setBtn = TextView(this).apply {
            text = "Set Wallpaper"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.parseColor("#CC3B82F6"))
                setStroke(dp(1), Color.parseColor("#88FFFFFF"))
            }
            setOnClickListener { chooseTarget(item, dialog) }
        }
        frame.addView(setBtn, FrameLayout.LayoutParams(-1, dp(56), Gravity.BOTTOM).apply {
            setMargins(dp(24), 0, dp(24), dp(40))
        })
        frame.addView(TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#66000000"))
            }
            setOnClickListener { dialog.dismiss() }
        }, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END).apply {
            setMargins(0, dp(40), dp(16), 0)
        })
        dialog.setContentView(frame)
        dialog.show()
    }

    private fun chooseTarget(item: WallpaperItem, dialog: Dialog) {
        val options = arrayOf("Home screen", "Lock screen", "Both")
        val flags = intArrayOf(
            WallpaperManager.FLAG_SYSTEM,
            WallpaperManager.FLAG_LOCK,
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        )
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                Toast.makeText(this, "Applying...", Toast.LENGTH_SHORT).show()
                Thread {
                    val msg = try {
                        WallpaperCatalog.apply(this, item, flags[which]); "Wallpaper set"
                    } catch (e: Exception) {
                        "Failed: " + e.message
                    }
                    runOnUiThread {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        if (msg == "Wallpaper set") dialog.dismiss()
                    }
                }.start()
            }.show()
    }
}
