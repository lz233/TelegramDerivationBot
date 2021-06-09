package math.term

import math.term.Term

operator fun X.times(x: X) = X(unit * x.unit, index + x.index)

class X(override var unit: Int = 0, override var index: Int = 1) : Term {

    init {
        this.unit = unit
        this.index = index
    }

    override fun derivative() = X(unit * index, index - 1)

    override fun times(term: Term) = if (term is X)
        X(unit * term.unit, index + term.index)
    else
        throw Throwable("UnSupport")

    override fun toString() =
        if (unit == 0)
            ""
        else
            "${if (unit >= 0) "+${unit}" else "$unit"}${
                when {
                    index > 1 -> "x^$index"
                    index == 1 -> "x"
                    index == 0 -> ""
                    else -> "x^($index)"
                }
            }"
}