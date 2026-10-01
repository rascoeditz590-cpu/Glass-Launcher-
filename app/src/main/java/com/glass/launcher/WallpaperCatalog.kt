package com.glass.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes

data class PhotoWallpaper(
    val id: String,
    val name: String,
    val category: String, // "Glass" ya "Anime"
    @DrawableRes val full: Int,
    @DrawableRes val thumb: Int
)

object PhotoWallpapers {
    val all: List<PhotoWallpaper> = listOf(
        PhotoWallpaper("anime_water", "Water Breath", "Anime", R.drawable.wp_anime_water, R.drawable.thumb_anime_water),
        PhotoWallpaper("anime_thunder", "Thunder Flash", "Anime", R.drawable.wp_anime_thunder, R.drawable.thumb_anime_thunder),
        PhotoWallpaper("anime_beast", "Beast Forest", "Anime", R.drawable.wp_anime_beast, R.drawable.thumb_anime_beast),
        PhotoWallpaper("anime_pinkflame", "Pink Flame", "Anime", R.drawable.wp_anime_pinkflame, R.drawable.thumb_anime_pinkflame),
        PhotoWallpaper("anime_crowmoon", "Crow Moon", "Anime", R.drawable.wp_anime_crowmoon, R.drawable.thumb_anime_crowmoon),
        PhotoWallpaper("anime_frost", "Frost Eyes", "Anime", R.drawable.wp_anime_frost, R.drawable.thumb_anime_frost),
        PhotoWallpaper("anime_firestorm", "Fire Storm", "Anime", R.drawable.wp_anime_firestorm, R.drawable.thumb_anime_firestorm),
        PhotoWallpaper("anime_silveraura", "Silver Aura", "Anime", R.drawable.wp_anime_silveraura, R.drawable.thumb_anime_silveraura),
        PhotoWallpaper("glass_amber_horizon", "Amber Horizon", "Glass", R.drawable.wp_glass_amber_horizon, R.drawable.thumb_glass_amber_horizon),
        PhotoWallpaper("glass_mono_ripple", "Mono Ripple", "Glass", R.drawable.wp_glass_mono_ripple, R.drawable.thumb_glass_mono_ripple),
        PhotoWallpaper("glass_violet_orb", "Violet Orb", "Glass", R.drawable.wp_glass_violet_orb, R.drawable.thumb_glass_violet_orb),
        PhotoWallpaper("glass_sapphire_twins", "Sapphire Twins", "Glass", R.drawable.wp_glass_sapphire_twins, R.drawable.thumb_glass_sapphire_twins),
        PhotoWallpaper("glass_gold_hourglass", "Gold Hourglass", "Glass", R.drawable.wp_glass_gold_hourglass, R.drawable.thumb_glass_gold_hourglass),
        PhotoWallpaper("glass_emerald_infinity", "Emerald Infinity", "Glass", R.drawable.wp_glass_emerald_infinity, R.drawable.thumb_glass_emerald_infinity),
        PhotoWallpaper("glass_rose_wings", "Rose Wings", "Glass", R.drawable.wp_glass_rose_wings, R.drawable.thumb_glass_rose_wings),
        PhotoWallpaper("glass_jade_drop", "Jade Drop", "Glass", R.drawable.wp_glass_jade_drop, R.drawable.thumb_glass_jade_drop),
        PhotoWallpaper("glass_crystal_knot", "Crystal Knot", "Glass", R.drawable.wp_glass_crystal_knot, R.drawable.thumb_glass_crystal_knot),
        PhotoWallpaper("glass_amber_hourglass", "Amber Hourglass", "Glass", R.drawable.wp_glass_amber_hourglass, R.drawable.thumb_glass_amber_hourglass),
    )

    val categories: List<String> = listOf("All") + all.map { it.category }.distinct()

    fun byCategory(category: String): List<PhotoWallpaper> =
        if (category == "All") all else all.filter { it.category == category }

    // which: WallpaperManager.FLAG_SYSTEM (home), FLAG_LOCK, ya dono ke liye FLAG_SYSTEM or FLAG_LOCK
    fun apply(context: Context, item: PhotoWallpaper, which: Int = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) {
        val bmp = BitmapFactory.decodeResource(context.resources, item.full)
        WallpaperManager.getInstance(context).setBitmap(bmp, null, true, which)
    }
}
