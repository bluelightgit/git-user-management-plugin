package com.meow.manageGitUser.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

import com.meow.manageGitUser.services.GitUserManager.*

@Service(Service.Level.PROJECT)
class GitUserService(project: Project) {

    private val gitUserManager: GitUserManager

    init {
        thisLogger().info("GitUserService initialized for project: ${project.name}")
        val objectMapper = ObjectMapper()
        gitUserManager = GitUserManager(objectMapper, project.basePath!!)
    }

    fun getGitUsers(): List<GitUser> {
        return gitUserManager.getGitUsers()
    }

    fun getGlobalGitUser(): GitUser {
        return gitUserManager.getCurGlobalGitUser()!!
    }

    fun setGitUser(name: String, email: String, env: GitEnv) {
        gitUserManager.
    }

    fun addGitUser(name: String, email: String, env: GitEnv) {
        setGitUser(name, email, env)
    }
}