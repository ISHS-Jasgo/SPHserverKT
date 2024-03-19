package util

import java.util.Random
import kotlin.math.*

class Vector {
    var x: Double = 0.0
    var y: Double = 0.0

    constructor() {
        this.x = 0.0
        this.y = 0.0
    }

    constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    operator fun plus(v: Vector): Vector {
        return Vector(x + v.x, y + v.y)
    }

    operator fun minus(v: Vector): Vector {
        return Vector(x - v.x, y - v.y)
    }

    operator fun times(d: Double): Vector {
        return Vector(x * d, y * d)
    }

    operator fun div(d: Double): Vector {
        return Vector(x / d, y / d)
    }

    fun dot(v: Vector): Double {
        return x * v.x + y * v.y
    }

    fun cross(v: Vector): Vector {
        return Vector(x * v.y, -y * v.x)
    }

    fun normalize() {
        val s = size()
        x /= s
        y /= s
    }

    fun normalized(): Vector {
        val s = size()
        return Vector(x / s, y / s)
    }

    fun distance(v: Vector): Double {
        return sqrt((v.x - x).pow(2.0) + (v.y - y).pow(2.0))
    }

    fun size(): Double {
        return sqrt(x.pow(2.0) + y.pow(2.0))
    }

    fun rotate(): Vector {
        // random with normal distribution
        var rand = Random().nextGaussian()
        if (rand < -1) rand = -1.0
        if (rand > 1) rand = 1.0
        val angle = rand * PI / 2
        val cos = sqrt(1 - sin(angle).pow(2.0))
        val sin = sin(angle)
        return Vector(x * cos - y * sin, x * sin + y * cos)
    }
}

fun main() {
    val v1 = Vector(1.0, 0.0).rotate()
    println(v1.x.toString() + " " + v1.y.toString())
}