package simulation

import util.Collider
import util.KDTree
import util.Node
import util.Vector
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

val objects: List<Collider> = listOf(

)

fun realSPH(maxParticles: Int, domainWidth: Int, domainHeight: Int, gradient: Double): Double {
    if (maxParticles <= 0) return 0.0
    val DOMAIN_X_LIM = doubleArrayOf(SMOOTHING_LENGTH.toDouble(), (domainWidth - SMOOTHING_LENGTH).toDouble())
    val DOMAIN_Y_LIM = doubleArrayOf(SMOOTHING_LENGTH.toDouble(), (domainHeight - SMOOTHING_LENGTH).toDouble())

    val maxForceList = ArrayList<Double>()

    var nParticles = 0

    val positions = ArrayList<Vector>()
    val velocities = ArrayList<Vector>()
    var count = 0
    for (iter in 0 until N_TIME_STEPS) {
        if (nParticles < maxParticles) {
            val newPositions = ArrayList<Vector>()
            for (i in 0 until maxParticles) {
                var random = Math.random()
                val x = DOMAIN_X_LIM[0] + random * (DOMAIN_X_LIM[1] - DOMAIN_X_LIM[0])
                random = Math.random()
                val y = DOMAIN_Y_LIM[0] + random * (DOMAIN_Y_LIM[1] - DOMAIN_Y_LIM[0])
                newPositions.add(Vector(x, y))
            }
            val newVelocities = ArrayList<Vector>()
            for (i in 0 until maxParticles) {
                newVelocities.add(Vector())
            }
            nParticles += maxParticles

            positions.addAll(newPositions)
            velocities.addAll(newVelocities)
            // print positions
//            for (i in 0 until nParticles) {
//                println(positions[i].x.toString() + " " + positions[i].y.toString())
//            }
//            println(positions.size)
        }
        val nodes = ArrayList<Node>()
        for (i in 0 until nParticles) {
            nodes.add(Node(positions[i]))
        }
        val tree = KDTree(nodes)
        val densities = DoubleArray(maxParticles)
        // calculate densities
        for (i in 0 until nParticles) {
            val neighbors = tree.rangeSearch(positions[i], SMOOTHING_LENGTH.toDouble())
            var density = 0.0
            for (element in neighbors) {
                density += (SMOOTHING_LENGTH.toDouble().pow(2.0) - element.first.pow(2.0)).pow(3.0)
            }
            densities[i] = density * NORMALIZATION_DENSITY
        }
        val pressures = DoubleArray(maxParticles)
        for (i in 0 until maxParticles) {
            pressures[i] = ISOTROPIC_EXPONENT * (densities[i] - BASE_DENSITY)
        }

        val forces = ArrayList<Vector>()
        for (i in 0 until maxParticles) {
            forces.add(Vector())
        }
        // calculate pressure force and viscous force
        for (i in 0 until maxParticles) {
            val neighbors = tree.rangeSearch(positions[i], SMOOTHING_LENGTH.toDouble())
            for (j in neighbors.indices) {
                if (neighbors[j].second == positions[i]) {
                    neighbors.removeAt(j)
                    break
                }
            }
            for (j in 0 until neighbors.size) {
                val distance = neighbors[j].first
                var pressureForce =
                    (positions[i] - neighbors[j].second) / distance * (pressures[i] + pressures[j]) / (2 * densities[j]) * NORMALIZATION_PRESSURE_FORCE * (SMOOTHING_LENGTH.toDouble() - distance).pow(2)
                pressureForce = pressureForce.rotate()
//                println(pressureForce.size())
                val viscousForce =
                    (velocities[j] - velocities[i]) / densities[j] * NORMALIZATION_VISCOUS_FORCE * (SMOOTHING_LENGTH.toDouble() - distance)
                forces[i] += (pressureForce + viscousForce)
            }
        }

        // calculate gravity force
        for (i in 0 until maxParticles) {
            forces[i] += Vector(0.0, 10 * sin(PI * gradient / 180))
        }

        // calculate new velocities and positions
        for (i in 0 until maxParticles) {
            velocities[i] += forces[i] * simulation.TIME_STEP_LENGTH / densities[i]
            positions[i] += velocities[i] * simulation.TIME_STEP_LENGTH
        }

        // check for collisions with walls
        for (i in 0 until maxParticles) {
            if (positions[i].x < DOMAIN_X_LIM[0]) {
                positions[i].x = DOMAIN_X_LIM[0]
                velocities[i].x *= DAMPING_COEFFICIENT
            }
            if (positions[i].x > DOMAIN_X_LIM[1]) {
                positions[i].x = DOMAIN_X_LIM[1]
                velocities[i].x *= DAMPING_COEFFICIENT
            }
            if (positions[i].y < DOMAIN_Y_LIM[0]) {
                positions[i].y = DOMAIN_Y_LIM[0]
                velocities[i].y *= DAMPING_COEFFICIENT
            }
            if (positions[i].y > DOMAIN_Y_LIM[1]) {
                positions[i].y = DOMAIN_Y_LIM[1]
                velocities[i].y *= DAMPING_COEFFICIENT
            }
            objects.forEach { collider ->
                if (collider.isXColliding(positions[i].x, SMOOTHING_LENGTH.toDouble())) {
                    positions[i].x = collider.x
                    velocities[i].x *= DAMPING_COEFFICIENT
                }
                if (collider.isYColliding(positions[i].y, SMOOTHING_LENGTH.toDouble())) {
                    positions[i].y = collider.y
                    velocities[i].y *= DAMPING_COEFFICIENT
                }
            }
        }

//        print top 10 forces size
        val sortedForces = forces.sortedByDescending { it.size() }
        // return mean value of top 10 forces
        maxForceList.add(sortedForces[0].size())
    }
    var result = 0.0
    for (i in 0 until maxForceList.size) {
        result += maxForceList[i]
    }
    return result / maxForceList.size
}
fun main(args: Array<String>) {
    val domainWidth = 4
    val domainHeight = 190
    val maxParticles = 1000
    val objects = ArrayList<Collider>()
//    objects.add(Collider(1.25, 55.0, 2.5, 110.0))
//    objects.add(Collider(9.75, 55.0, 2.5, 110.0))
//    objects.add(Collider(5.5, 160.0, 2.0, 3.0))
//    objects.add(Collider(5.5, 200.0, 2.0, 3.0))
    for (i in 0 until 5) {
        val randomX = floor(Math.random() * 10)
        val randomY = floor(Math.random() * 500)
        objects.add(Collider(randomX, randomY, 1.0, 3.0))
    }
    val result = realSPH(maxParticles, domainWidth, domainHeight, 10.0)
    println(result)
    println(SPH(1000, 4, 190))
}