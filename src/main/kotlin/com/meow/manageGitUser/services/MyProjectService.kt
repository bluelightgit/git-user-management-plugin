package com.meow.manageGitUser.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.meow.manageGitUser.MyBundle

import com.meow.manageGitUser.services.GitUserManager.*

@Service(Service.Level.PROJECT)
class GitUserService(project: Project) {

    private val gitUserManager: GitUserManager
    private val project: Project = project

    init {
        thisLogger().info("GitUserService initialized for project: ${project.name}")
        val filePath = MyBundle.message("GitUserProfileParentPath")
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        gitUserManager = GitUserManager(objectMapper, filePath)
    }

    fun getGitUsers(): List<GitUser> {
        return gitUserManager.getGitUsers()
    }

    fun getGlobalGitUser(): GitUser {
        return gitUserManager.getCurGlobalGitUser()!!
    }

    fun setGitUser(name: String, email: String, env: GitEnv) {
        val gitUser = GitUser(name, email)
        if (env == GitEnv.GLOBAL) {
            gitUserManager.applyGlobalGitUser(gitUser)
        } else {
            gitUserManager.applyLocalGitUser(gitUser, project.basePath!!)
        }
    }

    fun addGitUser(name: String, email: String, env: GitEnv, path: String?) {
        val gitUser = GitUser(name, email)
        gitUserManager.saveGitUsers(gitUser)
        if (env == GitEnv.LOCAL && path != null) {
            gitUserManager.saveLocalGitUsers(gitUser, path)
        }
    }

    fun deleteGitUser(gitUser: GitUser, env: GitEnv) {
        gitUserManager.deleteGitUser(gitUser)
    }
}