package com.glass.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes

data class WallpaperItem(
    val id: String,
    val name: String,
    val category: String, // "Glass" ya "Anime"
    @DrawableRes val full: Int,
    @DrawableRes val thumb: Int
)

object WallpaperCatalog {
    val all: List<WallpaperItem> = listOf(
        WallpaperItem("anime_water", "Water Breath", "Anime", R.drawable.wp_anime_water, R.drawable.thumb_anime_water),
        WallpaperItem("anime_thunder", "Thunder Flash", "Anime", R.drawable.wp_anime_thunder, R.drawable.thumb_anime_thunder),
        WallpaperItem("anime_beast", "Beast Forest", "Anime", R.drawable.wp_anime_beast, R.drawable.thumb_anime_beast),
        WallpaperItem("anime_pinkflame", "Pink Flame", "Anime", R.drawable.wp_anime_pinkflame, R.drawable.thumb_anime_pinkflame),
        WallpaperItem("anime_crowmoon", "Crow Moon", "Anime", R.drawable.wp_anime_crowmoon, R.drawable.thumb_anime_crowmoon),
        WallpaperItem("anime_frost", "Frost Eyes", "Anime", R.drawable.wp_anime_frost, R.drawable.thumb_anime_frost),
        WallpaperItem("anime_firestorm", "Fire Storm", "Anime", R.drawable.wp_anime_firestorm, R.drawable.thumb_anime_firestorm),
        WallpaperItem("anime_silveraura", "Silver Aura", "Anime", R.drawable.wp_anime_silveraura, R.drawable.thumb_anime_silveraura),
        WallpaperItem("glass_amber_horizon", "Amber Horizon", "Glass", R.drawable.wp_glass_amber_horizon, R.drawable.thumb_glass_amber_horizon),
        WallpaperItem("glass_mono_ripple", "Mono Ripple", "Glass", R.drawable.wp_glass_mono_ripple, R.drawable.thumb_glass_mono_ripple),
        WallpaperItem("glass_violet_orb", "Violet Orb", "Glass", R.drawable.wp_glass_violet_orb, R.drawable.thumb_glass_violet_orb),
        WallpaperItem("glass_sapphire_twins", "Sapphire Twins", "Glass", R.drawable.wp_glass_sapphire_twins, R.drawable.thumb_glass_sapphire_twins),
        WallpaperItem("glass_gold_hourglass", "Gold Hourglass", "Glass", R.drawable.wp_glass_gold_hourglass, R.drawable.thumb_glass_gold_hourglass),
        WallpaperItem("glass_emerald_infinity", "Emerald Infinity", "Glass", R.drawable.wp_glass_emerald_infinity, R.drawable.thumb_glass_emerald_infinity),
        WallpaperItem("glass_rose_wings", "Rose Wings", "Glass", R.drawable.wp_glass_rose_wings, R.drawable.thumb_glass_rose_wings),
        WallpaperItem("glass_jade_drop", "Jade Drop", "Glass", R.drawable.wp_glass_jade_drop, R.drawable.thumb_glass_jade_drop),
        WallpaperItem("glass_crystal_knot", "Crystal Knot", "Glass", R.drawable.wp_glass_crystal_knot, R.drawable.thumb_glass_crystal_knot),
        WallpaperItem("glass_amber_hourglass", "Amber Hourglass", "Glass", R.drawable.wp_glass_amber_hourglass, R.drawable.thumb_glass_amber_hourglass),
    )

    val categories: List<String> = listOf("All") + all.map { it.category }.distinct()

    fun byCategory(category: String): List<WallpaperItem> =
        if (category == "All") all else all.filter { it.category == category }

    // which: WallpaperManager.FLAG_SYSTEM (home), FLAG_LOCK, ya dono ke liye FLAG_SYSTEM or FLAG_LOCK
    fun apply(context: Context, item: WallpaperItem, which: Int = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) {
        val bmp = BitmapFactory.decodeResource(context.resources, item.full)
        WallpaperManager.getInstance(context).setBitmap(bmp, null, true, which)
    }
}
