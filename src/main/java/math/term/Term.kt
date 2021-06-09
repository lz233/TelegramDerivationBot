package math.term

interface Term {
    var unit: Int
    var index: Int
    fun derivative(): Term
    operator fun times(term: Term): Term
    override fun toString(): String
}