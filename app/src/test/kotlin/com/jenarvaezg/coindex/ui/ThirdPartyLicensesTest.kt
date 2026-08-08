package com.jenarvaezg.coindex.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThirdPartyLicensesTest {
    private val licenses = File("src/main/assets/licenses")

    @Test
    fun `every resolved release dependency group has exactly one declared license`() {
        val resolved = File("build/generated/licenses/release-runtime-groups.txt")
            .readLines()
            .filter(String::isNotBlank)
            .toSet()
        val declared = File(licenses, "dependency-groups.tsv")
            .readLines()
            .filter(String::isNotBlank)
            .associate { line ->
                val (group, license) = line.split('\t')
                group to license
            }

        assertEquals(declared.keys, resolved)
        assertEquals("MIT", declared.getValue("org.slf4j"))
        assertTrue(declared.filterKeys { it != "org.slf4j" }.values.all { it == "Apache-2.0" })
    }

    @Test
    fun `the three complete license texts are packaged for reading`() {
        val apache = File(licenses, "apache-2.0.txt").readText()
        val mit = File(licenses, "slf4j-mit.txt").readText()
        val ofl = File(licenses, "ofl-1.1.txt").readText()

        assertTrue(apache.contains("Apache License\n                           Version 2.0, January 2004"))
        assertTrue(apache.contains("END OF TERMS AND CONDITIONS"))
        assertTrue(mit.contains("Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland)"))
        assertTrue(mit.replace(Regex("\\s+"), " ").contains("THE SOFTWARE IS PROVIDED \"AS IS\""))
        assertTrue(ofl.contains("SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007"))
        assertTrue(ofl.contains("Copyright 2011 The Bitter Project Authors"))
        assertTrue(ofl.contains("Copyright 2017 The Barlow Project Authors"))
        assertTrue(ofl.contains("PERMISSION & CONDITIONS"))
        assertTrue(ofl.contains("TERMINATION\nThis license becomes null and void"))
    }
}
