import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.math.abs

class KDTree(nodes: List<Node>?) {
    private val cmpX: Comparator<Node> = Comparator { p1, p2 -> p1.coords.x.compareTo(p2.coords.x) }
    private val cmpY: Comparator<Node> = Comparator { p1, p2 -> p1.coords.y.compareTo(p2.coords.y) }
    private var root: Node? = null

    init {
        buildTree(nodes)
    }

    private fun buildTree(nodes: List<Node>?) {
        root = object : Any() {
            fun buildTree(divX: Boolean, nodes: List<Node>?): Node? {
                if (nodes.isNullOrEmpty()) return null
                Collections.sort(nodes, if (divX) cmpX else cmpY)
                val mid = nodes.size shr 1
                val node = Node()
                node.coords = nodes[mid].coords
                node.left = buildTree(!divX, nodes.subList(0, mid))
                if (mid + 1 <= nodes.size - 1) node.right = buildTree(!divX, nodes.subList(mid + 1, nodes.size))
                return node
            }
        }.buildTree(true, nodes)
    }
    fun rangeSearch(start: Vector, range: Double): MutableList<Pair<Double, Vector>> {
        return object : Any() {
            var result: MutableList<Pair<Double, Vector>> = LinkedList()
            var radius = range
            fun rangeSearch(node: Node?, divX: Boolean): MutableList<Pair<Double, Vector>> {
                if (node == null) return result
                val d = node.coords.distance(start)
                if (radius >= d) result.add(Pair(d, node.coords))
                val delta: Double = if (divX) start.x - node.coords.x else start.y - node.coords.y
                val delta2 = abs(delta)
                val node1: Node? = if (delta < 0) node.left else node.right
                val node2: Node? = if (delta < 0) node.right else node.left
                rangeSearch(node1, !divX)
                if (delta2 < radius) {
                    rangeSearch(node2, !divX)
                }
                return result
            }
        }.rangeSearch(root, true)
    }
    fun insert(coords: Vector) {
        val node: Node = Node(coords)
        if (root == null) root = node else insert(node, root!!, true)
    }

    private fun insert(node: Node?, currentNode: Node, divX: Boolean) {
        if (node == null) return
        val cmpResult = (if (divX) cmpX else cmpY).compare(node, currentNode)
        if (cmpResult == -1) if (currentNode.left == null) currentNode.left = node else insert(node, currentNode.left!!, !divX) else if (currentNode.right == null) currentNode.right = node else insert(node, currentNode.right!!, !divX)
    }

    val size: Int
        get() = object : Any() {
            var cnt = 0
            fun getSize(node: Node?): Int {
                if (node == null) return cnt - 1
                if (node.left != null) {
                    cnt++
                    getSize(node.left)
                }
                if (node.right != null) {
                    cnt++
                    getSize(node.right)
                }
                return cnt
            }
        }.getSize(root) + 1
}