package com.github.metabrain.eu4.parser

import java.text.ParseException
import java.util.*
import kotlin.coroutines.experimental.buildSequence
import kotlin.streams.asStream
import kotlin.streams.toList

/**
 * Created by daniel.parreira on 14/01/2018.
 */
class EU4Parser() {
    fun parse(raw: String): List<Node> {
        return parse(readAllTokens(raw).iterator())
    }

    fun parse(seq: Iterator<String>): List<Node> {
        val nodes = mutableListOf<Node>()

        while(seq.hasNext()) {
            val key = seq.next()
            if(key == "}")
                return nodes // stop

            val op = seq.next()
            val value = seq.next()

            if(op == "=") {
                nodes.add(
                        when {
                            value == "{" -> when (key) {
                                "OR" -> OR(key, parse(seq))
                                "AND" -> AND(key, parse(seq))
//                            "possible" -> POSSIBLE(key, parse(seq))
//                            "happened" -> HAPPENED(key, parse(seq))
//                            "custom_trigger_tooltip" -> CUSTOM_TRIGGER_TOOLTIP(key, parse(seq))
//                            else -> ACHIEVEMENT(key, parse(seq))
                                else -> Item(key, parse(seq))
                            }
                            value == "id" -> ID(value.toInt())
//                    value.toIntOrNull() != null -> Number(key, value.toInt())
                            value.toLowerCase() == "true" -> Bool(key, true)
                            value.toLowerCase() == "yes" -> Bool(key, true)
                            value.toLowerCase() == "false" -> Bool(key, false)
                            value.toLowerCase() == "no" -> Bool(key, false)
//                    key == "OR" -> Bool(key, false)
                            else -> Const(key, value)
                        }
                )
            } else {
//                throw InputMismatchException("ups: key:$key op:$op value:$value")

                // is a list of values
                val l: MutableList<String> = mutableListOf()
                l.add(key) // was not op but actually first element
                l.add(op) // was not op but actually first element
                l.add(value) // was not op but actually first element
                do {
                    val next = seq.next()
                    if(next!="}")
                        l.add(next)
                } while(next!="}")

                l.forEach { value ->
                    nodes.add(
                            when {
                                value.toIntOrNull() != null -> ID(value.toInt())
                                value.toLowerCase() == "true" -> Bool(key, true)
                                value.toLowerCase() == "yes" -> Bool(key, true)
                                value.toLowerCase() == "false" -> Bool(key, false)
                                value.toLowerCase() == "no" -> Bool(key, false)
                                else -> Const(key, value)
                            }
                    )
                }

            }
        }

        return nodes
    }


    fun readAllTokens(achievements: String): Sequence<String> =
            buildSequence {
                achievements.lines().forEach outer@ { line ->
                    val scanner = Scanner(line)
                    var tokens = scanner.asSequence().asStream().map { it.toString() }.toList()
                    //        tokens.forEach { println(it) }

                    // conflate tokens that are bounded by quotes
                    tokens = conflateQuotedTokens(tokens)
                    tokens = splitUnspacedOperators(tokens)

                    // everything before first comment #
                    tokens.forEach {
                        if(it.startsWith("#"))
                            return@outer
                        // separate consecutive control characters
                        if(it=="={") {
                            yield("=")
                            yield("{")
                        } else {
                            yield(it)
                        }
                    }
                }
            }

    fun conflateQuotedTokens(tokens: List<String>): List<String> {
        var insideQuote = false
        val conflated = mutableListOf<String>()
        tokens.forEach {
            if(insideQuote) {
                if(it.endsWith("\"")) {
                    insideQuote = false
                }
                // keep concatenating even, including last one
                val last_idx = conflated.lastIndex
                conflated[last_idx] += " ${it.replace("\"","")}"
            } else {
                if(it.startsWith("\"")) {
                    insideQuote = true
                }
                // add items outside quotes or the first one, if we are starting a quoted word now
                conflated.add(it.replace("\"",""))
            }
        }
        return conflated
    }

    fun splitUnspacedOperators(tokens: List<String>): List<String> {
        var oldToks: List<String>
        var newToks = mutableListOf<String>().apply { this.addAll(tokens) }
        do {
            oldToks = newToks
            newToks = mutableListOf<String>()
            oldToks.forEach {
                newToks.addAll(splitUnspacedOperators(it))
            }
        } while(oldToks.size!=newToks.size)
        return newToks
    }

    val operators = listOf("=", "{", "}")
    fun splitUnspacedOperators(word: String): List<String> {
        operators.forEach { op ->
            if (word!=op && word.split(op).size > 1)
                return word.split(op, ignoreCase = false, limit = 2).toMutableList().apply { this.add(1, op) }
        }
        return listOf(word)
    }


}

sealed class Node(val name: String, val value: Any) {
    override fun toString(): String {
        return name+" = "+value.toString()//+"\n"
    }
}
sealed class Tree constructor(k: String, nodes: List<Node>) : Node(k, nodes) {
    override fun toString(): String =
            name+" =\n"+ (
                    (value as List<Node>)
                            .map { it.toString() }
                            .map {
                                it.lines()
                                        .map { "\t$it" }
                                        .joinToString("\n")
                            }
                            .joinToString("\n")
                    )
}
open class Number(k: String, number: Int) : Node(k, number)
//class ID(k: String, number: Int) : Number(k, number)
class ID(number: Int) : Node("id", number)
class Bool(k: String, v: Boolean) : Node(k, v)
class Const(k: String, v: String) : Node(k, v)

class Date(k: String, date: java.util.Date) : Node(k, date)

class ACHIEVEMENT(k: String, nodes: List<Node>) : Tree(k, nodes)
class OR(k: String, nodes: List<Node>) : Tree(k, nodes)
class NOT(k: String, nodes: List<Node>) : Tree(k, nodes)
class AND(k: String, nodes: List<Node>) : Tree(k, nodes)
class Item(k: String, nodes: List<Node>) : Tree(k, nodes)
class POSSIBLE(k: String, nodes: List<Node>) : Tree(k, nodes)
class HAPPENED(k: String, nodes: List<Node>) : Tree(k, nodes)
class CUSTOM_TRIGGER_TOOLTIP(k: String, nodes: List<Node>) : Tree(k, nodes)