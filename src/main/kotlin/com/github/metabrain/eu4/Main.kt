package com.github.metabrain.eu4

import com.google.common.io.Resources
import java.util.*
import java.util.Date
import java.util.function.Supplier
import java.util.regex.Pattern
import kotlin.Number
import kotlin.coroutines.experimental.buildSequence
import kotlin.streams.asStream
import kotlin.streams.toList

fun main(args: Array<String>) {
    val achievements = Resources.getResource("achievements.txt").readText(Charsets.UTF_8)
//    println(achievements)

    val toks = readAllTokens(achievements)
//    toks.forEachIndexed { idx, s -> println("tok[$idx] -> $s") }

    // algo
    var ast = parse(toks.iterator())

//    ast = ast.subList(0,3)
    ast
            .filterIsInstance(ACHIEVEMENT::class.java)
            .forEachIndexed { idx, s -> println("-----------[$idx]--------------\n$s\n") }

//    val t = Tree("k1",
//            listOf(
//                Tree("k2",
//                        listOf(
//                                Const("key", "val")
//                    )
//                )
//            )
//        )
//    println(t)
}

fun parse(seq: Iterator<String>): List<Node> {
    val nodes = mutableListOf<Node>()

    while(seq.hasNext()) {
        val key = seq.next()
        if(key == "}")
            return nodes // stop

        val op = seq.next()
        val value = seq.next()

        if (op != "=")
            println("ups: $key $op $value")

        nodes.add(
                when {
                    value == "{" -> when(key) {
                        "OR" -> OR(key, parse(seq))
                        "AND" -> AND(key, parse(seq))
                        "possible" -> POSSIBLE(key, parse(seq))
                        "happened" -> HAPPENED(key, parse(seq))
                        "custom_trigger_tooltip" -> CUSTOM_TRIGGER_TOOLTIP(key, parse(seq))
                        else -> ACHIEVEMENT(key, parse(seq))
                    }
                    value=="id" -> ID(key, value.toInt())
//                    value.toIntOrNull() != null -> Number(key, value.toInt())
                    value.toLowerCase() == "true" -> Bool(key, true)
                    value.toLowerCase() == "yes" -> Bool(key, true)
                    value.toLowerCase() == "false" -> Bool(key, false)
                    value.toLowerCase() == "no" -> Bool(key, false)
//                    key == "OR" -> Bool(key, false)
                    else -> Const(key, value)
                }
        )
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


sealed class Node(val name: String, val value: Any) {
    override fun toString(): String {
        return name+" = "+value.toString()//+"\n"
    }
}
sealed class Tree(k: String, nodes: List<Node>) : Node(k, nodes) {
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
sealed class Number(k: String, number: Int) : Node(k, number)
class ID(k: String, number: Int) : com.github.metabrain.eu4.Number(k, number)
class Bool(k: String, v: Boolean) : Node(k, v)
class Const(k: String, v: String) : Node(k, v)

class Date(k: String, date: Date) : Node(k, date)

class ACHIEVEMENT(k: String, nodes: List<Node>) : Tree(k, nodes)
class OR(k: String, nodes: List<Node>) : Tree(k, nodes)
class NOT(k: String, nodes: List<Node>) : Tree(k, nodes)
class AND(k: String, nodes: List<Node>) : Tree(k, nodes)
class POSSIBLE(k: String, nodes: List<Node>) : Tree(k, nodes)
class HAPPENED(k: String, nodes: List<Node>) : Tree(k, nodes)
class CUSTOM_TRIGGER_TOOLTIP(k: String, nodes: List<Node>) : Tree(k, nodes)
