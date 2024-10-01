package com.meow.manageGitUser.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.InputStreamReader

@Service(Service.Level.PROJECT)
class GitUserService(project: Project) {

    init {
        thisLogger().info("GitUserService initialized for project: ${project.name}")
    }

    fun getLocalGitUsers(): List<GitUser> {
        val users = mutableListOf<GitUser>()
        val process = Runtime.getRuntime().exec("git config --list --local")
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.lines().forEach { line ->
                if (line.startsWith("user.name=")) {
                    users.add(GitUser().apply {
                        name = line.substringAfter("=")
                        email = line.substringAfter("=")
                    })
                }
            }
        }
        return users
    }

    fun getGlobalGitUser(): GitUser {
        val process = Runtime.getRuntime().exec("git config --list --global")
        var gitUser = GitUser()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.lines().forEach { line ->
                if (line.startsWith("user.name=")) {
                    gitUser.name = line.substringAfter("=")
                    gitUser.email = line.substringAfter("=")
                }
            }
        }
        return gitUser
    }
}