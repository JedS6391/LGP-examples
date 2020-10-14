package nz.co.jedsimson.lgp.examples.kotlin

import java.lang.StringBuilder

/**
 * Simulates the ant as it moves through the grid.
 *
 * @param grid A grid that ant will move its way through.
 * @property maximumMoves A limit on the number of moves permitted by the ant.
 */
class Ant(grid: Grid, val maximumMoves: Int) {
    var state: AntState

    /**
     * Gets details about the current position of the ant.
     */
    val position: Position
        get() = Position(
            row = this.state.row,
            column =  this.state.column,
            direction = this.state.direction
        )

    /**
     * Gets the grid the ant moves its way through.
     */
    val grid: Grid
        get() = this.state.grid

    init {
        this.state = AntState(
            movesMade = 0,
            foodEaten = 0,
            row = 0,
            column = 0,
            direction = Direction.North,
            grid = grid
        )
    }

    /**
     * Resets the ant and the grid back to the initial state.
     */
    fun reset() {
        this.state.grid.reset()
        this.state = AntState(
            movesMade = 0,
            foodEaten = 0,
            row = 0,
            column = 0,
            direction = Direction.North,
            grid = this.state.grid
        )
    }

    /**
     * Turns the ant to the left if there are moves remaining.
     */
    fun turnLeft() {
        if (this.state.movesMade < this.maximumMoves) {
            this.state = this.state.copy(
                movesMade = this.state.movesMade + 1,
                direction = when (this.state.direction) {
                    Direction.North -> Direction.West
                    Direction.West -> Direction.South
                    Direction.South -> Direction.East
                    Direction.East -> Direction.North
                }
            )
        }
    }

    /**
     * Turns the ant to the right if there are moves remaining.
     */
    fun turnRight() {
        if (this.state.movesMade < this.maximumMoves) {
            this.state = this.state.copy(
                movesMade = this.state.movesMade + 1,
                direction = when (this.state.direction) {
                    Direction.North -> Direction.East
                    Direction.East -> Direction.South
                    Direction.South -> Direction.West
                    Direction.West -> Direction.North
                }
            )
        }
    }

    /**
     * Moves the ant forward if there are moves remaining.
     *
     * If the new position in the grid contains food, the food will be eaten.
     */
    fun moveForward() {
        if (this.state.movesMade < this.maximumMoves) {
            val nextPosition = this.nextPosition()

            this.state = this.state.copy(
                movesMade = this.state.movesMade + 1,
                row = nextPosition.row,
                column = nextPosition.column
            )

            if (this.state.grid.containsFood(this.state.row, this.state.column)) {
                this.state.grid.eatFood(this.state.row, this.state.column)

                this.state = this.state.copy(foodEaten = this.state.foodEaten + 1)
            }
        }
    }

    /**
     * Checks whether there is food ahead in the next position.
     */
    fun isFoodAhead(): Boolean {
        val nextPosition = this.nextPosition()

        return this.state.grid.containsFood(nextPosition.row, nextPosition.column)
    }

    fun copy(): Ant = Ant(
        grid = this.state.grid.clone(),
        maximumMoves = this.maximumMoves
    )

    override fun toString(): String =
        "Ant(movesMade = ${this.state.movesMade}, foodEaten = ${this.state.foodEaten}, foodRemaining = ${this.state.grid.foodRemaining()}, position = ${this.position})"

    private fun nextPosition(): Position {
        val rowOffset = when (this.state.direction) {
            Direction.North -> -1
            Direction.South -> 1
            Direction.West, Direction.East -> 0
        }
        val columnOffset = when (this.state.direction) {
            Direction.East -> 1
            Direction.West -> -1
            Direction.North, Direction.South -> 0
        }

        return Position(
            row = Math.floorMod(this.state.row + rowOffset, this.state.grid.numberOfRows),
            column = Math.floorMod(this.state.column + columnOffset, this.state.grid.numberOfColumns),
            direction = this.state.direction
        )
    }

    data class Position(
        val row: Int,
        val column: Int,
        val direction: Direction
    )

    enum class Direction {
        North,
        East,
        South,
        West
    }

    data class AntState(
        val movesMade: Int,
        val foodEaten: Int,
        val row: Int,
        val column: Int,
        val direction: Direction,
        val grid: Grid
    )
}

/**
 * Represents the grid an [Ant] is moving through.
 *
 * @property initialGridData A 2-D array of cells that contains the initial grid data before any ant movements.
 */
class Grid(private val initialGridData: Array<Array<Cell>>) {
    private var grid: Array<Array<Cell>> = initialGridData.map { row -> row.clone() }.toTypedArray()

    init {
        val numberOfColumns = initialGridData[0].size
        assert(initialGridData.all { row -> row.size == numberOfColumns }) {
            "All rows expected to have $numberOfColumns columns"
        }
    }

    /**
     * Gets the number of rows in the grid.
     */
    val numberOfRows: Int = grid.size

    /**
     * Gets the number of columns in the grid.
     */
    val numberOfColumns: Int = grid[0].size

    /**
     * Gets the amount of food remaining in the grid.
     */
    fun foodRemaining(): Int = grid.sumBy { row ->
        row.filter { cell -> cell == Cell.Food }.count()
    }

    /**
     * Checks if the cell at the given co-ordinates contains food.
     */
    fun containsFood(row: Int, column: Int) = when (grid[row][column]) {
        Cell.Empty -> false
        Cell.Food -> true
    }

    /**
     * Eats the food in the cell at the given co-ordinates.
     */
    fun eatFood(row: Int, column: Int) {
        grid[row][column] = Cell.Empty
    }

    /**
     * Resets the grid back to the [initialGridData].
     */
    fun reset() {
        this.grid = this.initialGridData.map { row -> row.clone() }.toTypedArray()
    }

    /**
     * Creates a new grid instance seeded with [initialGridData].
     */
    fun clone(): Grid {
        return Grid(this.initialGridData.map { row -> row.clone() }.toTypedArray())
    }

    override fun toString(): String {
        val sb = StringBuilder()

        this.grid.forEach { row ->
            row.forEach { cell ->
                sb.append(when (cell) {
                    Cell.Empty -> '.'
                    Cell.Food  -> '#'
                })
            }

            sb.append(System.lineSeparator())
        }

        return sb.toString()
    }

    /**
     * Represents the state of the cells in the grid.
     */
    enum class Cell {
        /**
         * The cell contains food.
         */
        Food,

        /**
         * The cell is empty (i.e. it does not contain food).
         */
        Empty
    }
}