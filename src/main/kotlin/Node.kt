class Node {
    var coords: Vector = Vector()

    var left: Node? = null

    var right: Node? = null

    constructor() {
        coords = Vector()
    }

    constructor(coords: Vector) {
        this.coords = coords
    }
}