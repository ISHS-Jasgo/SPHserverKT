package util

class Collider(var x: Double, var y: Double, var width: Double, var height: Double) {
    fun isXColliding(otherX: Double, radius: Double): Boolean {
        return otherX + radius > x - width / 2 && otherX - radius < x + width / 2
    }

    fun isYColliding(otherY: Double, radius: Double): Boolean {
        return otherY + radius > y - height / 2 && otherY - radius < y + height / 2
    }

    fun moveTo(x: Double, y: Double) {
        this.x = x
        this.y = y
    }
}