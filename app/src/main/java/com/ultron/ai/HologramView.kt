package com.ultron.ai

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** The glowing holographic core. mode: idle, listening, thinking, speaking. */
class HologramView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    var mode = "idle"
    var level = 0f

    private val cyan = 0xFF3FE0FF.toInt()
    private val amber = 0xFFFFB347.toInt()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var energy = 0.25f
    private var spin = 1f
    private var rot = 0f
    private var t = 0f
    private var last = System.nanoTime()

    private class Part(val a: Float, val r: Float, val s: Float, val z: Float)

    private val parts = List(28) {
        Part(
            Random.nextFloat() * 6.283f,
            0.3f + Random.nextFloat() * 0.62f,
            (0.2f + Random.nextFloat() * 0.6f) * (if (Random.nextBoolean()) -1f else 1f),
            1f + Random.nextFloat() * 1.6f
        )
    }

    private fun ac(base: Int, a: Float): Int =
        (base and 0xFFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = ((now - last) / 1e9f).coerceAtMost(0.1f)
        last = now
        t += dt

        val target = when (mode) {
            "listening" -> 0.5f + level * 0.5f
            "thinking" -> 1f
            "speaking" -> 0.85f
            else -> 0.25f
        }
        val tspin = when (mode) {
            "thinking" -> 3.2f
            "listening" -> 1.6f
            else -> 1f
        }
        energy += (target - energy) * 0.06f
        spin += (tspin - spin) * 0.05f
        rot += dt * spin

        val base = if (mode == "thinking") amber else cyan
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val r = min(w, h) / 2f * 0.94f
        val deg = 57.2958f

        c.save()
        c.translate(w / 2f, h / 2f)

        // soft glow
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(0f, 0f, r * 0.7f, ac(base, 0.38f + energy * 0.3f), ac(base, 0f), Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, r * 0.7f, p)

        // core
        val pulse = 1f + 0.05f * sin(t * 3f) * (0.4f + energy)
        val cr = r * 0.22f * pulse
        p.shader = RadialGradient(
            0f, 0f, cr,
            intArrayOf(0xF2FFFFFF.toInt(), ac(base, 0.9f), ac(base, 0.1f)),
            floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(0f, 0f, cr, p)
        p.shader = null

        // outer ring and tick marks
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 1f * d
        p.color = ac(base, 0.35f)
        c.drawCircle(0f, 0f, r, p)
        for (k in 0 until 72) {
            val a = k / 72f * 6.283f
            val long = k % 6 == 0
            val hi = max(0f, cos(a - rot * 1.5f))
            p.color = ac(base, (if (long) 0.55f else 0.25f) + hi * 0.4f)
            p.strokeWidth = (if (long) 1.6f else 1f) * d
            val r1 = r * 0.93f
            val r2 = r * (if (long) 0.86f else 0.89f)
            c.drawLine(cos(a) * r1, sin(a) * r1, cos(a) * r2, sin(a) * r2, p)
        }

        // dashed ring
        c.save()
        c.rotate(-rot * 0.8f * deg)
        p.pathEffect = DashPathEffect(floatArrayOf(r * 0.05f, r * 0.04f), 0f)
        p.strokeWidth = 1.4f * d
        p.color = ac(base, 0.5f)
        c.drawCircle(0f, 0f, r * 0.78f, p)
        p.pathEffect = null
        c.restore()

        // three bold arcs
        c.save()
        c.rotate(rot * 1.2f * deg)
        p.strokeWidth = 4f * d
        p.color = ac(base, 0.75f)
        val rect = RectF(-r * 0.66f, -r * 0.66f, r * 0.66f, r * 0.66f)
        for (s in 0 until 3) c.drawArc(rect, s * 120f, 51f, false, p)
        c.restore()

        // live waveform ring
        val path = Path()
        for (i in 0..120) {
            val a = i / 120f * 6.283f
            val wob = (sin(a * 6f + t * 4f) * 0.052f + sin(a * 13f - t * 3f) * 0.03f) * r * energy
            val rr = r * 0.5f + wob
            val x = cos(a) * rr
            val y = sin(a) * rr
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        p.strokeWidth = 1.6f * d
        p.color = ac(base, 0.9f)
        c.drawPath(path, p)

        // orbiting particles
        p.style = Paint.Style.FILL
        p.color = ac(base, 0.75f)
        for (pt in parts) {
            val ang = pt.a + t * pt.s * spin * 0.5f
            c.drawCircle(cos(ang) * r * pt.r, sin(ang) * r * pt.r, pt.z * d, p)
        }

        c.restore()
        postInvalidateOnAnimation()
    }
}
