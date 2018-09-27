package com.jiandanlangman.ageh.demo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun crash(view : View) {
        Thread{
            throw RuntimeException()
        }.start()

    }
}
