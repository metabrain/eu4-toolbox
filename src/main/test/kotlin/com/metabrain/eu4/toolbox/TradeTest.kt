package com.metabrain.eu4.toolbox

import com.github.metabrain.eu4.parser.EU4Parser
import com.google.common.io.Resources
import org.junit.Test

/**
 * Created by daniel.parreira on 14/01/2018.
 */
class TradeTest {
    @Test
    fun loadTradeModel() {

        val raw = Resources.getResource("tradenodes.txt").readText(Charsets.UTF_8)
        val ast = EU4Parser().parse(raw)
        ast
                .forEachIndexed { idx, s -> println("-----------[$idx]--------------\n$s\n") }

    }

}