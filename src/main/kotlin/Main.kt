import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow

const val PARTICLE_MASS = 70
const val ISOTROPIC_EXPONENT = 20
const val BASE_DENSITY = 1
const val SMOOTHING_LENGTH = 5
const val DYNAMIC_VISCOSITY = 0.1
const val DAMPING_COEFFICIENT = -0.9
//const val GRAVITY = -9.8

const val TIME_STEP_LENGTH = 0.01
const val N_TIME_STEPS = 5

val NORMALIZATION_DENSITY = (4 * PARTICLE_MASS) / (PI * SMOOTHING_LENGTH.toDouble().pow(8.0))
val NORMALIZATION_PRESSURE_FORCE = (10 * PARTICLE_MASS) / (PI * SMOOTHING_LENGTH.toDouble().pow(5)) * (-1)
val NORMALIZATION_VISCOUS_FORCE = (40 * DYNAMIC_VISCOSITY * PARTICLE_MASS) / (PI * SMOOTHING_LENGTH.toDouble().pow(5))

fun SPH(maxParticles: Int, domainWidth: Int, domainHeight: Int): Double {
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
//        for (i in 0 until maxParticles) {
//            forces[i] += Vector(0.0, GRAVITY)
//        }

        // calculate new velocities and positions
        for (i in 0 until maxParticles) {
            velocities[i] += forces[i] * TIME_STEP_LENGTH / densities[i]
            positions[i] += velocities[i] * TIME_STEP_LENGTH
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

fun calculateMeanMaxSPH(width: Int, height: Int, meanPeopleCount: Int, maxPeopleCount: Int): Pair<Double, Double> {
    val mean = floor(SPH(meanPeopleCount, width, height) * 100) / 100
    val max = floor(SPH(maxPeopleCount, width, height)* 100) / 100
    return Pair(mean, max)
}

fun calculateSPH(width: Int, height: Int, current: Int, peopleCount: List<Int>): List<Double> {
    val result = ArrayList<Double>()
    result.add(floor(SPH(current, width, height) * 100) / 100)
    for (element in peopleCount) {
        result.add(floor(SPH(element, width, height) * 100) / 100)
    }
    return result
}