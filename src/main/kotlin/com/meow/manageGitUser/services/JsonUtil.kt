package com.meow.manageGitUser.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class JsonUtil(private val mapper: ObjectMapper) {

    private fun <T> readListFromFile(file: File, valueType: Class<T>): MutableList<T> {
        return if (file.length() == 0L) {
            mutableListOf()
        } else {
            mapper.readValue(file, mapper.typeFactory.constructCollectionType(MutableList::class.java, valueType))
        }
    }

    private fun <K, V> readMapFromFile(file: File, keyType: Class<K>, valueType: Class<V>): MutableMap<K, V> {
        return if (file.length() == 0L) {
            mutableMapOf()
        } else {
            mapper.readValue(file, mapper.typeFactory.constructMapType(MutableMap::class.java, keyType, valueType))
        }
    }

    fun <T> writeListToFile(file: File, data: List<T>) {
        mapper.writeValue(file, data)
    }

    fun <K, V> writeMapToFile(file: File, data: Map<K, V>) {
        mapper.writeValue(file, data)
    }

    fun readListUserFromFile(file: File): MutableList<GitUserManager.GitUser> {
        return readListFromFile(file, GitUserManager.GitUser::class.java)
    }

    fun readMapStringUserFromFile(file: File): MutableMap<String, GitUserManager.GitUser> {
        return readMapFromFile(file, String::class.java, GitUserManager.GitUser::class.java)
    }
}