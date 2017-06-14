package com.lykke.box.options.rabbit.parser

interface IncomingParser<out T> {
    fun parse(input: String): List<T>
}