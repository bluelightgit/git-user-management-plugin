package com.meow.manageGitUser.services

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class GitUserManager(private val mapper: ObjectMapper, parentPath: String) {

    private object FileNameConstant {
        const val USER_FILE = "/users.json"
        const val USER_LOCAL_FILE = "/localUsers.json"
    }
    private val jsonUtil = JsonUtil(mapper)
    private var users: MutableList<GitUser> = mutableListOf()
    private var localGitUserWithPath: MutableMap<String, GitUser> = mutableMapOf()
    private var curGlobalGitUser: GitUser? = null
    private val usersFile: File = File(parentPath + FileNameConstant.USER_FILE)
    private val localGitUsersFile: File = File(parentPath + FileNameConstant.USER_LOCAL_FILE)

    init {
        init()
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun init() {
        curGlobalGitUser = getCurrentGitUser(GitEnv.GLOBAL)
        if (!usersFile.exists()) {
            usersFile.createNewFile()
        }
//        if (!localGitUsersFile.exists()) {
//            localGitUsersFile.createNewFile()
//        }

        try {
            loadGitUsers()
//            loadLocalGitUsers()
            if (users.isEmpty() || (curGlobalGitUser != null && users.none { it == curGlobalGitUser })) {
                saveGitUsers(curGlobalGitUser!!)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun loadGitUsers() {
        try {
            users = jsonUtil.readListUserFromFile(usersFile)
            if (users.isEmpty()) {
                saveGitUsers(curGlobalGitUser!!)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun loadLocalGitUsers() {
        try {
            localGitUserWithPath = jsonUtil.readMapStringUserFromFile(localGitUsersFile)
            if (localGitUserWithPath.isEmpty()) {
                saveLocalGitUsers(curGlobalGitUser!!, System.getProperty("user.dir"))
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

     fun saveGitUsers(user: GitUser): GitUser {
        try {
            users.add(user)
            jsonUtil.writeListToFile(usersFile, users)
            return user
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun editGitUser(oldUser: GitUser, newUser: GitUser): GitUser {
        try {
            val index = users.indexOf(oldUser)
            if (index == -1) {
                throw RuntimeException("User not found")
            }
            users[index] = newUser
            jsonUtil.writeListToFile(usersFile, users)
            return newUser
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun deleteGitUser(user: GitUser): GitUser {
        if (user.equals(curGlobalGitUser)) {
            throw RuntimeException("Cannot delete current global user")
        }
        try {
            users.remove(user)
            jsonUtil.writeListToFile(usersFile, users)
            return user
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun saveLocalGitUsers(user: GitUser, path: String) {
        try {
            localGitUserWithPath[path] = user
            jsonUtil.writeMapToFile(localGitUsersFile, localGitUserWithPath)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun applyGitUser(user: GitUser, env: GitEnv, dir: File, automaticSave: Boolean, fallbackException: Boolean) {
        var findGitUser = users.find { user.equals(it) }
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

        ProcessBuilder("git", "config", "--" + env.value, "user.name", user.name)
            .directory(dir)
            .start()
            .waitFor()
        ProcessBuilder("git", "config", "--" + env.value, "user.email", user.email)
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
    fun applyLocalGitUser(user: GitUser, path: String) {
        val inPath = System.getProperty(path)
        applyGitUser(user, GitEnv.LOCAL, File(inPath), true, false)
        localGitUserWithPath[path] = user
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
    fun getCurrentGitUser(env: GitEnv): GitUser? {
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
        val process = ProcessBuilder("git", "config", "--" + env.value, key).start()
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GitUser

            if (name != other.name) return false
            if (email != other.email) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + email.hashCode()
            return result
        }

        override fun toString(): String {
            return "$name <$email>"
        }
    }

    enum class GitEnv(val value: String) {
        GLOBAL("global"),
        LOCAL("local")
    }
}