package com.meow.manageGitUser.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.meow.manageGitUser.services.GitUserService
import java.awt.Color
import javax.swing.BoxLayout

class GitUserToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = GitUserToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class GitUserToolWindow(toolWindow: ToolWindow) {

        private val gitUserService = toolWindow.project.service<GitUserService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            val localUsers = gitUserService.getLocalGitUsers()
            val globalUser = gitUserService.getGlobalGitUser()

            val label = JBLabel("Global User: $globalUser")

            localUsers.forEach { user ->
                val label = JBLabel(user)
//                if (user == globalUser) {
//                    label.foreground = Color.CYAN // 高亮显示当前全局用户
//                }
                add(label)
            }
        }
    }
}