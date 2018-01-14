package com.github.metabrain.eu4

import com.github.metabrain.eu4.parser.ACHIEVEMENT
import com.github.metabrain.eu4.parser.EU4Parser
import com.google.common.io.Resources
import java.util.*
import java.util.Date
import kotlin.coroutines.experimental.buildSequence
import kotlin.streams.asStream
import kotlin.streams.toList

fun main(args: Array<String>) {
    val raw = Resources.getResource("tradenodes.txt").readText(Charsets.UTF_8)
//    val achievements = Resources.getResource("achievements.txt").readText(Charsets.UTF_8)
//    println(achievements)

    val ast = EU4Parser().parse(raw)
//    toks.forEachIndexed { idx, s -> println("tok[$idx] -> $s") }

    // algo

//    ast = ast.subList(0,3)
    ast
//            .filterIsInstance(ACHIEVEMENT::class.java)
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

