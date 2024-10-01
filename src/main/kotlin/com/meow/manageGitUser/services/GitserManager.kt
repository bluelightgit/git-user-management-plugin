package com.meow.manageGitUser.services

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class GitUserManager(private val mapper: ObjectMapper, parentPath: String) {

    private object fileNameConstant {
        const val USER_FILE = "/users.json"
        const val USER_LOCAL_FILE = "/localUsers.json"
    }
    private var users: MutableList<GitUser> = mutableListOf()
    private var localGitUserWithPath: MutableMap<String, GitUser> = mutableMapOf()
    private var curGlobalGitUser: GitUser? = null
    private val usersFile: File = File(parentPath + fileNameConstant.USER_FILE)
    private val localGitUsersFile: File = File(parentPath + fileNameConstant.USER_LOCAL_FILE)

    init {
        init()
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun init() {
        curGlobalGitUser = getCurrentGitGitUser(GitEnv.GLOBAL)
        if (!usersFile.exists()) {
            usersFile.createNewFile()
        }
        if (!localGitUsersFile.exists()) {
            localGitUsersFile.createNewFile()
        }

        try {
            loadGitUsers()
            loadLocalGitUsers()
            if (users.isEmpty() || (curGlobalGitUser != null && users.none { it == curGlobalGitUser })) {
                saveGitUsers(curGlobalGitUser!!)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun loadGitUsers() {
        try {
            if (usersFile.length() == 0L) {
                users = mutableListOf()
                saveGitUsers(curGlobalGitUser!!)
                return
            }
            users = mapper.readValue(usersFile, mapper.typeFactory.constructCollectionType(MutableList::class.java, GitUser::class.java))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun loadLocalGitUsers() {
        try {
            if (localGitUsersFile.length() == 0L) {
                localGitUserWithPath = mutableMapOf()
                saveLocalGitUsers(curGlobalGitUser!!, System.getProperty("user.dir"))
                return
            }
            localGitUserWithPath = mapper.readValue(localGitUsersFile, mapper.typeFactory.constructMapType(MutableMap::class.java, String::class.java, GitUser::class.java))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun saveGitUsers(user: GitUser) {
        try {
            users.add(user)
            mapper.writeValue(usersFile, users)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun saveLocalGitUsers(user: GitUser, path: String) {
        try {
            localGitUserWithPath[path] = user
            mapper.writeValue(localGitUsersFile, localGitUserWithPath)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun applyGitUser(user: GitUser, env: GitEnv, dir: File, automaticSave: Boolean, fallbackException: Boolean) {
        var findGitUser = users.find { it == user }
        if (findGitUser == null) {
            if (automaticSave) {
                saveGitUsers(user)
            } else if (fallbackException) {
                throw RuntimeException("GitUser not found")
            } else return
        }

        if (env == GitEnv.LOCAL) {
            findGitUser = localGitUserWithPath[dir.absolutePath]
            if (findGitUser == null || findGitUser != user) {
                if (automaticSave) {
                    saveLocalGitUsers(user, dir.absolutePath)
                } else if (fallbackException) {
                    throw RuntimeException("GitUser not found")
                } else return
            }
        }

        ProcessBuilder("git", "config", env.value, "user.name", user.name)
            .directory(dir)
            .start()
            .waitFor()
        ProcessBuilder("git", "config", env.value, "user.email", user.email)
            .directory(dir)
            .start()
            .waitFor()
    }

    @Throws(IOException::class, InterruptedException::class)
    fun applyGlobalGitUser() {
        applyGitUser(curGlobalGitUser!!, GitEnv.GLOBAL, File(System.getProperty("user.dir")), true, true)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun applyGlobalGitUser(user: GitUser) {
        applyGitUser(user, GitEnv.GLOBAL, File(System.getProperty("user.dir")), true, false)
        curGlobalGitUser = user
    }

    @Throws(IOException::class, InterruptedException::class)
    fun applyLocalGitUser(user: GitUser) {
        val path = System.getProperty("user.dir")
        applyGitUser(user, GitEnv.LOCAL, File(path), true, false)
        localGitUserWithPath[path] = user
    }

    @Throws(IOException::class, InterruptedException::class)
    fun applyLocalGitUser() {
        for ((path, user) in localGitUserWithPath) {
            applyGitUser(user, GitEnv.LOCAL, File(path), true, true)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun getCurrentGitGitUser(env: GitEnv): GitUser? {
        val name = getGitConfigValue("user.name", env)
        val email = getGitConfigValue("user.email", env)
        return if (name != null && email != null) {
            GitUser(name, email)
        } else {
            null
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun getGitConfigValue(key: String, env: GitEnv): String? {
        val process = ProcessBuilder("git", "config", env.value, key).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val value = reader.readLine()
        process.waitFor()
        return value
    }

    fun getCurGlobalGitUser(): GitUser? {
        return curGlobalGitUser
    }

    fun getGitUsers(): List<GitUser> {
        return users
    }

    class GitUser(var name: String, var email: String) {
    }

    enum class GitEnv(val value: String) {
        GLOBAL("global"),
        LOCAL("local")
    }
}