package eu.depau.etchdroid.libdepaums.streams

interface Seekable {
    fun seek(offset: Long): Long
}