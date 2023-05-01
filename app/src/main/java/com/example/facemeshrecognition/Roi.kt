package com.example.facemeshrecognition

import com.google.common.collect.ImmutableSet

class Roi {

    companion object {
        //        val foreHead = mutableListOf<Int>(107,108,336,337)
        val foreHead = arrayOf(108, 337, 336, 107,108)
        val leftCheek = arrayOf(47, 142, 203, 206, 207, 187, 117, 118, 119,47)
        val rightCheek = arrayOf(277, 371, 423, 426, 427, 411, 346, 347, 348,277)
    }

}