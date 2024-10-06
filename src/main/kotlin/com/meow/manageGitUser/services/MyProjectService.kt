package com.meow.manageGitUser.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.meow.manageGitUser.MyBundle

import com.meow.manageGitUser.services.GitUserManager.*
import java.util.*

@Service(Service.Level.PROJECT)
class GitUserService(project: Project) {

    private val gitUserManager: GitUserManager
    private val project: Project = project

    init {
        thisLogger().info("GitUserService initialized for project: ${project.name}")
        val filePath = getGitUserProfileParentPath()
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        gitUserManager = GitUserManager(objectMapper, filePath)
    }

    private fun getGitUserProfileParentPath(): String {
        val properties = Properties()
        properties.load(this::class.java.getResourceAsStream("/messages/MyBundle.properties"))
        val pathTemplate = properties.getProperty("GitUserProfileParentPath")
        return pathTemplate.replace("\${user.home}", System.getProperty("user.home"))
    }

    fun getGitUsers(): List<GitUser> {
        return gitUserManager.getGitUsers()
    }

    fun getGlobalGitUser(): GitUser {
        return gitUserManager.getCurGlobalGitUser()!!
    }

    fun setGitUser(gitUser: GitUser, env: GitEnv) {
        if (env == GitEnv.GLOBAL) {
            gitUserManager.applyGlobalGitUser(gitUser)
        } else {
            gitUserManager.applyLocalGitUser(gitUser, project.basePath!!)
        }
    }

    fun addGitUser(gitUser: GitUser, env: GitEnv, path: String?) {
        gitUserManager.saveGitUsers(gitUser)
        if (env == GitEnv.LOCAL && path != null) {
            gitUserManager.saveLocalGitUsers(gitUser, path)
        }
    }

    fun deleteGitUser(gitUser: GitUser, env: GitEnv) {
        gitUserManager.deleteGitUser(gitUser)
    }

    fun editGitUser(oldUser: GitUser, newUser: GitUser) {
        gitUserManager.editGitUser(oldUser, newUser)
    }
}