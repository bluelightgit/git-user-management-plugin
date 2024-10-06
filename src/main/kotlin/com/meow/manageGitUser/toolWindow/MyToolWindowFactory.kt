package com.meow.manageGitUser.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.meow.manageGitUser.services.GitUserManager
import com.meow.manageGitUser.services.GitUserService
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
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

            val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))

            val addButton = JButton("+")
            addButton.addActionListener { showDialog(OPRT.ADD) }
            buttonPanel.add(addButton)

            val deleteButton = JButton("-")
            deleteButton.addActionListener { deleteUser() }
            buttonPanel.add(deleteButton)

            val editButton = JButton("Edit")
            editButton.addActionListener { showDialog(OPRT.EDIT) }
            buttonPanel.add(editButton)

            val applyButton = JButton("Apply")
            applyButton.addActionListener { applySelectedUser() }
            buttonPanel.add(applyButton)

            add(buttonPanel, BorderLayout.NORTH)

            userList.cellRenderer = RoundedCellRenderer(gitUserService)
        }

        private fun refreshContent() {
            userListModel.clear()
            val localUsers = gitUserService.getGitUsers()
            val globalUser = gitUserService.getGlobalGitUser()

            localUsers.forEach { user ->
                userListModel.addElement(user)
            }
        }

        private fun showDialog(operation: OPRT) {
            val dialog = object : DialogWrapper(true) {
                private val nameField = JBTextField()
                private val emailField = JBTextField()
                private val globalRadio = JBRadioButton("Apply as global user")
                private val localRadio = JBRadioButton("Apply as Local user")
                private val buttonGroup = ButtonGroup()

                init {
                    // if edit but no user selected, show warning
                    if (operation == OPRT.EDIT && userList.selectedValue == null) {
                        title = "Please select a user to edit"
                        isOKActionEnabled = false
                    } else if (operation == OPRT.ADD) {
                        title = "Add User"
                    } else {
                        title = "Edit User"
                        val selectedUser = userList.selectedValue
                        nameField.text = selectedUser?.name
                        emailField.text = selectedUser?.email
                    }
                    init()
                }

                override fun createCenterPanel(): JComponent {
                    val panel = JPanel()
                    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

                    buttonGroup.add(globalRadio)
                    buttonGroup.add(localRadio)

                    panel.add(JBLabel("Name:"))
                    panel.add(nameField)
                    panel.add(JBLabel("Email:"))
                    panel.add(emailField)
                    panel.add(globalRadio)
//                    panel.add(localRadio)

                    return panel
                }

                override fun doOKAction() {
                    val env = if (globalRadio.isSelected) GitUserManager.GitEnv.GLOBAL else GitUserManager.GitEnv.LOCAL
                    val curGitUser = GitUserManager.GitUser(nameField.text, emailField.text)
                    if (operation == OPRT.ADD) {
                        gitUserService.addGitUser(curGitUser, env, toolWindow.project.basePath)
                    } else {
                        val selectedUser = userList.selectedValue
                        if (selectedUser != null) {
                            gitUserService.editGitUser(selectedUser, curGitUser)
                        }
                    }
                    if (globalRadio.isSelected) {
                        gitUserService.setGitUser(curGitUser, env)
                    }
                    refreshContent()
                    super.doOKAction()
                }
            }
            dialog.show()
        }

        private fun applySelectedUser() {
            val selectedUser = userList.selectedValue
            if (selectedUser != null) {
                gitUserService.setGitUser(selectedUser, GitUserManager.GitEnv.GLOBAL)
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

    enum class OPRT {
        ADD,
//        DELETE,
        EDIT
    }

    class RoundedCellRenderer(private val gitUserService: GitUserService) : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            val panel = JPanel(BorderLayout())
            panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            panel.add(component, BorderLayout.CENTER)

            if (isSelected) {
                panel.background = JBColor.background()
                panel.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(JBColor.background().brighter(), 5, true),
                    BorderFactory.createEmptyBorder(0,0,0,0)
                )
            } else {
                panel.background = list.background
            }

            if (value is GitUserManager.GitUser) {
                if (value == gitUserService.getGlobalGitUser()) {
                    component.font = component.font.deriveFont(Font.BOLD)
                }
            }

            return panel
        }
    }
}