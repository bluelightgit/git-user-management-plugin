package com.meow.manageGitUser.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.meow.manageGitUser.services.GitUserManager
import com.meow.manageGitUser.services.GitUserService
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.*

class GitUserToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = GitUserToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class GitUserToolWindow(private val toolWindow: ToolWindow) {

        private val gitUserService = toolWindow.project.service<GitUserService>()
        private val userListModel = DefaultListModel<GitUserManager.GitUser>()
        private val userList = JBList(userListModel)

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            refreshContent()

            val scrollPane = JScrollPane(userList)
            add(scrollPane, BorderLayout.CENTER)

            val buttonPanel = JPanel()
            buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.Y_AXIS)

            val addButton = JButton("+")
            addButton.addActionListener { showAddUserDialog() }
            buttonPanel.add(addButton, BorderLayout.EAST)

            val deleteButton = JButton("-")
            deleteButton.addActionListener { deleteUser() }
            buttonPanel.add(deleteButton, BorderLayout.EAST)

            val applyButton = JButton("Apply")
            applyButton.addActionListener { applySelectedUser() }
            buttonPanel.add(applyButton, BorderLayout.EAST)

            add(buttonPanel, BorderLayout.NORTH)

            userList.cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value is GitUserManager.GitUser) {
                        if (value.equals(gitUserService.getGlobalGitUser())) {
                            background = JBColor.background().brighter()
                            foreground = JBColor.foreground().brighter()
                            font = font.deriveFont(font.style or Font.BOLD)
                        }
                    }
                    return component
                }
            }
        }

        private fun refreshContent() {
            userListModel.clear()
            val localUsers = gitUserService.getGitUsers()
            val globalUser = gitUserService.getGlobalGitUser()

            localUsers.forEach { user ->
                userListModel.addElement(user)
            }
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
//            dialog.add(localRadio)

            val addButton = JButton("Add")
            addButton.addActionListener {
                val name = nameField.text
                val email = emailField.text
                val env = if (globalRadio.isSelected) GitUserManager.GitEnv.GLOBAL else GitUserManager.GitEnv.LOCAL
                gitUserService.setGitUser(name, email, env)
                refreshContent()
                dialog.dispose()
            }
            dialog.add(addButton)

            dialog.pack()
            dialog.isVisible = true
        }

        private fun applySelectedUser() {
            val selectedUser = userList.selectedValue
            if (selectedUser != null) {
                gitUserService.setGitUser(selectedUser.name, selectedUser.email, GitUserManager.GitEnv.GLOBAL)
                refreshContent()
            }
        }

        private fun deleteUser() {
            val selectedUser = userList.selectedValue
            if (selectedUser != null) {
                gitUserService.deleteGitUser(selectedUser, GitUserManager.GitEnv.GLOBAL)
                refreshContent()
            }
        }
    }
}