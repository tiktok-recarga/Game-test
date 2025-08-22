// MainActivity.kt - Actividad principal del juego con controles táctiles
package com.example.dodgegame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var gameThread: GameThread
    private var player: Player? = null
    private var obstacles = mutableListOf<Obstacle>()
    private var score = 0
    private var touchX: Float = 0f
    private var touchY: Float = 0f
    private var isTouching: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar la vista principal
        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        setContentView(surfaceView)
        
        // Inicializar el jugador
        player = Player(100, 100, 50, Color.BLUE)
    }

    // Manejar eventos táctiles
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                touchX = event.x
                touchY = event.y
                // Mover al jugador inmediatamente al tocar
                player?.x = touchX.toInt()
                player?.y = touchY.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                // Mover al jugador mientras se desliza el dedo
                player?.x = touchX.toInt()
                player?.y = touchY.toInt()
            }
            MotionEvent.ACTION_UP -> {
                isTouching = false
            }
        }
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Iniciar el hilo del juego cuando la superficie esté lista
        gameThread = GameThread(holder)
        gameThread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Actualizar dimensiones si es necesario
        player?.y = height / 2 // Posicionar al jugador en el centro vertical
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Detener el hilo del juego
        gameThread.setRunning(false)
        try {
            gameThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // Hilo principal del juego
    inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread() {
        private var running = true
        
        fun setRunning(isRunning: Boolean) {
            running = isRunning
        }

        override fun run() {
            var canvas: Canvas?
            while (running) {
                canvas = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        // Actualizar lógica del juego
                        updateGame()
                        
                        // Dibujar en el canvas
                        drawGame(canvas)
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    }
                }
                
                // Controlar la velocidad de actualización
                try {
                    sleep(16) // ~60 FPS
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateGame() {
        // Mover obstáculos
        obstacles.forEach { it.update() }
        
        // Eliminar obstáculos que salieron de pantalla
        obstacles.removeAll { it.x + it.size < 0 }
        
        // Generar nuevos obstáculos aleatoriamente
        if (Math.random() < 0.03) {
            val size = (30..80).random()
            val obstacle = Obstacle(surfaceView.width, (0..surfaceView.height - size).random(), size, Color.RED)
            obstacles.add(obstacle)
        }
        
        // Detectar colisiones
        obstacles.forEach { obstacle ->
            if (player!!.collidesWith(obstacle)) {
                // Game Over - reiniciar juego
                gameOver()
            }
        }
        
        // Incrementar puntuación
        score++
    }

    private fun gameOver() {
        // Reiniciar juego
        obstacles.clear()
        score = 0
        player?.x = 100
        player?.y = surfaceView.height / 2
    }

    private fun drawGame(canvas: Canvas) {
        // Fondo
        canvas.drawColor(Color.WHITE)
        
        // Dibujar jugador
        player?.draw(canvas)
        
        // Dibujar obstáculos
        obstacles.forEach { it.draw(canvas) }
        
        // Dibujar puntuación
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 40f
        canvas.drawText("Puntuación: $score", 20f, 50f, paint)
        
        // Dibujar instrucciones de control táctil
        if (score < 50) { // Mostrar solo al inicio
            paint.textSize = 30f
            canvas.drawText("Desliza el dedo para mover el círculo azul", 20f, 100f, paint)
        }
    }

    // Clase para el jugador
    inner class Player(var x: Int, var y: Int, var radius: Int, var color: Int) {
        private val paint = Paint()
        
        init {
            paint.color = color
        }
        
        fun draw(canvas: Canvas) {
            canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), paint)
        }
        
        fun collidesWith(obstacle: Obstacle): Boolean {
            // Detectar colisión circular-rectangular simplificada
            val closestX = x.coerceIn(obstacle.x, obstacle.x + obstacle.size)
            val closestY = y.coerceIn(obstacle.y, obstacle.y + obstacle.size)
            
            val distanceX = x - closestX
            val distanceY = y - closestY
            
            return (distanceX * distanceX + distanceY * distanceY) < (radius * radius)
        }
    }

    // Clase para los obstáculos
    inner class Obstacle(var x: Int, var y: Int, var size: Int, var color: Int) {
        private val paint = Paint()
        private val speed = (5..15).random() // Velocidad aleatoria
        
        init {
            paint.color = color
        }
        
        fun update() {
            x -= speed
        }
        
        fun draw(canvas: Canvas) {
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat(), paint)
        }
    }
}
