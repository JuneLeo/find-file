package com.cheetah.kotlin

import android.content.Context
import android.graphics.PixelFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

class ShareActivity : AppCompatActivity() {
    lateinit var coordinateLayout: CoordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        coordinateLayout = findViewById(R.id.root)
    }

    fun onFloatingClick(view: View) {
        Snackbar.make(coordinateLayout, "haha", Snackbar.LENGTH_LONG).show()
    }


}