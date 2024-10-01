package com.meow.manageGitUser.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.meow.manageGitUser.services.GitUserManager
import com.meow.manageGitUser.services.GitUserService
import java.awt.Color
import java.awt.event.ActionEvent
import javax.swing.*

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
            val localUsers = gitUserService.getGitUsers()
            val globalUser = gitUserService.getGlobalGitUser()

            localUsers.forEach { user ->
                val label = JBLabel("${user.name} <${user.email}>")
                if (user == globalUser) {
                    label.foreground = Color.RED // 高亮显示当前全局用户
                }
                add(label)
            }

            val addButton = JButton("Add User")
            addButton.addActionListener { showAddUserDialog() }
            add(addButton)
        }

        private fun showAddUserDialog() {
            val dialog = JDialog()
            dialog.title = "Add Git User"
            dialog.layout = BoxLayout(dialog.contentPane, BoxLayout.Y_AXIS)

            val nameField = JTextField()
            val emailField = JTextField()
            val globalRadio = JRadioButton("Global")
            val localRadio = JRadioButton("Local")
            val buttonGroup = ButtonGroup()
            buttonGroup.add(globalRadio)
            buttonGroup.add(localRadio)

            dialog.add(JLabel("Name:"))
            dialog.add(nameField)
            dialog.add(JLabel("Email:"))
            dialog.add(emailField)
            dialog.add(globalRadio)
            dialog.add(localRadio)

            val addButton = JButton("Add")
            addButton.addActionListener {
                val name = nameField.text
                val email = emailField.text
                val env = if (globalRadio.isSelected) GitUserManager.GitEnv.GLOBAL else GitUserManager.GitEnv.LOCAL
                gitUserService.addGitUser(name, email, env)
                dialog.dispose()
            }
            dialog.add(addButton)

            dialog.pack()
            dialog.isVisible = true
        }
    }
}