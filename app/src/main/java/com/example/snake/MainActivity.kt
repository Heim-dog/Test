package com.example.snake

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var snakeGame: SnakeGame

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        snakeGame = SnakeGame(this)
        setContentView(snakeGame)
    }

    override fun onPause() {
        super.onPause()
        snakeGame.holder.surface?.let { }
    }
}
