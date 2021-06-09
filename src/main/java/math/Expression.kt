package math

import math.operator.Operator
import math.operator.Times
import math.term.Term
import math.term.X
import math.term.times
import java.lang.StringBuilder

operator fun Term.unaryPlus() = Expression().apply { terms.add(this@unaryPlus) }

//operator fun Term.plus(expression: Expression) = Expression().apply { add(this@plus);terms.addAll()  }
operator fun Expression.plus(term: Term) = this.apply { terms.add(term) }
operator fun Expression.times(term: Term) = this.apply { operators.put(terms.size, Times());terms.add(term) }

class Expression {
    val operators = mutableMapOf<Int, Operator>()
    val terms = mutableListOf<Term>()

    fun derivative() = Expression().apply {
        var i = 0
        while (i in this@Expression.terms.indices) {
            if ((i != this@Expression.terms.lastIndex) and (this@Expression.operators[i + 1].toString() == "*")) terms.apply {
                add((this@Expression.terms[i].derivative() as X) * (this@Expression.terms[i + 1] as X))
                add((this@Expression.terms[i + 1].derivative() as X) * (this@Expression.terms[i] as X))
                i++
            } else {
                terms.add(this@Expression.terms[i].derivative())
            }
            for (j in 0 until i) {
                if ((terms[j] is X) and ((terms[j] as X).index == (terms[i] as X).index)) {
                    (terms[j] as X).unit = (terms[j] as X).unit + (terms[i] as X).unit
                    (terms[i] as X).unit = 0
                    break
                }
            }
            i++
        }
        terms.sortByDescending{(it as X).index}
    }

    override fun toString() = StringBuilder().apply {
        for (i in terms.indices) {
            val term = terms[i].toString()
            if (operators[i] != null) {
                append(operators[i].toString())
                if (term.startsWith('-')) append("($term)") else append(term.substring(1))
            } else {
                append(term)
            }
        }
    }.toString()

}