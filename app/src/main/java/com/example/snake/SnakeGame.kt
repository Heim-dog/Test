package com.example.snake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class SnakeGame(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private val surfaceHolder = holder
    private var gameThread: Thread? = null
    private var isRunning = false
    private var isGameOver = false
    private var isPaused = false

    private val gridSize = 20
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f
    private var boardSize = 0f

    private val snake = ArrayDeque<Pair<Int, Int>>()
    private var direction = Direction.RIGHT
    private var nextDirection = Direction.RIGHT
    private var food = Pair(0, 0)
    private var score = 0

    private val snakePaint = Paint().apply { color = Color.parseColor("#4CAF50") }
    private val snakeHeadPaint = Paint().apply { color = Color.parseColor("#2E7D32") }
    private val foodPaint = Paint().apply { color = Color.parseColor("#F44336") }
    private val bgPaint = Paint().apply { color = Color.parseColor("#1A1A2E") }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#16213E")
        strokeWidth = 1f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val scorePaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        textAlign = Paint.Align.LEFT
    }

    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThreshold = 50f

    private enum class Direction { UP, DOWN, LEFT, RIGHT }

    init {
        surfaceHolder.addCallback(this)
        isFocusable = true
        resetGame()
    }

    private fun resetGame() {
        snake.clear()
        snake.addFirst(Pair(gridSize / 2, gridSize / 2))
        snake.addFirst(Pair(gridSize / 2 + 1, gridSize / 2))
        snake.addFirst(Pair(gridSize / 2 + 2, gridSize / 2))
        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT
        score = 0
        isGameOver = false
        spawnFood()
    }

    private fun spawnFood() {
        val occupied = snake.toSet()
        var pos: Pair<Int, Int>
        do {
            pos = Pair((0 until gridSize).random(), (0 until gridSize).random())
        } while (occupied.contains(pos))
        food = pos
    }

    override fun run() {
        var lastUpdate = System.currentTimeMillis()
        val updateInterval = 150L

        while (isRunning) {
            val now = System.currentTimeMillis()
            if (!isPaused && !isGameOver && now - lastUpdate >= updateInterval) {
                update()
                lastUpdate = now
            }
            draw()
            val frameTime = System.currentTimeMillis() - now
            if (frameTime < 16) Thread.sleep(16 - frameTime)
        }
    }

    private fun update() {
        direction = nextDirection

        val head = snake.first()
        val newHead = when (direction) {
            Direction.UP -> Pair(head.first, head.second - 1)
            Direction.DOWN -> Pair(head.first, head.second + 1)
            Direction.LEFT -> Pair(head.first - 1, head.second)
            Direction.RIGHT -> Pair(head.first + 1, head.second)
        }

        if (newHead.first < 0 || newHead.first >= gridSize ||
            newHead.second < 0 || newHead.second >= gridSize ||
            snake.contains(newHead)
        ) {
            isGameOver = true
            return
        }

        snake.addFirst(newHead)
        if (newHead == food) {
            score += 10
            spawnFood()
        } else {
            snake.removeLast()
        }
    }

    private fun draw() {
        val canvas = surfaceHolder.lockCanvas() ?: return
        try {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()

            boardSize = minOf(w, h * 0.8f)
            cellSize = boardSize / gridSize
            offsetX = (w - boardSize) / 2f
            offsetY = (h - boardSize) / 2f + h * 0.04f

            canvas.drawRect(0f, 0f, w, h, bgPaint)
            drawGrid(canvas)
            drawFood(canvas)
            drawSnake(canvas)
            drawHUD(canvas, w, h)

            if (isGameOver) drawGameOver(canvas, w, h)
            if (isPaused && !isGameOver) drawPaused(canvas, w, h)
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        for (i in 0..gridSize) {
            val x = offsetX + i * cellSize
            val y = offsetY + i * cellSize
            canvas.drawLine(offsetX, y, offsetX + boardSize, y, gridPaint)
            canvas.drawLine(x, offsetY, x, offsetY + boardSize, gridPaint)
        }
    }

    private fun drawFood(canvas: Canvas) {
        val margin = cellSize * 0.15f
        val rect = cellRect(food.first, food.second, margin)
        canvas.drawOval(rect, foodPaint)
    }

    private fun drawSnake(canvas: Canvas) {
        snake.forEachIndexed { i, seg ->
            val margin = cellSize * 0.08f
            val rect = cellRect(seg.first, seg.second, margin)
            val paint = if (i == 0) snakeHeadPaint else snakePaint
            canvas.drawRoundRect(rect, cellSize * 0.25f, cellSize * 0.25f, paint)
        }
    }

    private fun cellRect(col: Int, row: Int, margin: Float): RectF {
        return RectF(
            offsetX + col * cellSize + margin,
            offsetY + row * cellSize + margin,
            offsetX + (col + 1) * cellSize - margin,
            offsetY + (row + 1) * cellSize - margin
        )
    }

    private fun drawHUD(canvas: Canvas, w: Float, h: Float) {
        val topY = offsetY * 0.6f
        scorePaint.textSize = h * 0.04f
        canvas.drawText("Score: $score", offsetX, topY, scorePaint)

        val pauseLabel = if (isPaused) "▶" else "⏸"
        scorePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(pauseLabel, offsetX + boardSize, topY, scorePaint)
        scorePaint.textAlign = Paint.Align.LEFT
    }

    private fun drawGameOver(canvas: Canvas, w: Float, h: Float) {
        val overlay = Paint().apply { color = Color.parseColor("#AA000000") }
        canvas.drawRect(0f, 0f, w, h, overlay)

        textPaint.textSize = h * 0.07f
        canvas.drawText("GAME OVER", w / 2, h / 2 - h * 0.08f, textPaint)

        textPaint.textSize = h * 0.04f
        canvas.drawText("Score: $score", w / 2, h / 2, textPaint)

        textPaint.textSize = h * 0.035f
        canvas.drawText("Tap to Restart", w / 2, h / 2 + h * 0.08f, textPaint)
    }

    private fun drawPaused(canvas: Canvas, w: Float, h: Float) {
        val overlay = Paint().apply { color = Color.parseColor("#88000000") }
        canvas.drawRect(0f, 0f, w, h, overlay)
        textPaint.textSize = h * 0.07f
        canvas.drawText("PAUSED", w / 2, h / 2, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (isGameOver) {
                    resetGame()
                    return true
                }

                val dx = event.x - touchStartX
                val dy = event.y - touchStartY

                // Tap on pause button area
                val pauseX = offsetX + boardSize
                val pauseY = offsetY * 0.6f
                val tapArea = cellSize * 1.5f
                if (event.x in (pauseX - tapArea)..(pauseX + tapArea) &&
                    event.y in (pauseY - tapArea)..(pauseY + tapArea)
                ) {
                    isPaused = !isPaused
                    return true
                }

                if (isPaused) {
                    isPaused = false
                    return true
                }

                if (Math.abs(dx) < swipeThreshold && Math.abs(dy) < swipeThreshold) return true

                if (Math.abs(dx) > Math.abs(dy)) {
                    nextDirection = if (dx > 0) {
                        if (direction != Direction.LEFT) Direction.RIGHT else direction
                    } else {
                        if (direction != Direction.RIGHT) Direction.LEFT else direction
                    }
                } else {
                    nextDirection = if (dy > 0) {
                        if (direction != Direction.UP) Direction.DOWN else direction
                    } else {
                        if (direction != Direction.DOWN) Direction.UP else direction
                    }
                }
            }
        }
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        gameThread?.join()
    }
}
