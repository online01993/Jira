import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.history.ChangeItemBean
import com.atlassian.jira.user.ApplicationUser

def changeHistoryManager = ComponentAccessor.changeHistoryManager
def userManager = ComponentAccessor.userManager

// 1. Получаем ВСЮ историю изменений задачи
def allChangeItems = changeHistoryManager.getAllChangeItems(issue)

// 2. Фильтруем: только изменения поля "assignee"
def assigneeChanges = allChangeItems.findAll { it.field == "assignee" }

if (assigneeChanges.isEmpty()) {
    log.warn("Нет истории изменений исполнителя для задачи ${issue.key}")
    return
}

// 3. Берём ПОСЛЕДНЕЕ изменение (самое свежее)
def lastChange = assigneeChanges.last()

// 4. Кто был исполнителем ДО этого последнего изменения?
def previousAssigneeUsername = lastChange.userKey
def previousAssignee = userManager.getUserByKey(previousAssigneeUsername)

if (previousAssignee == null) {
    log.warn("Предыдущий исполнитель не найден (username: ${previousAssigneeUsername})")
    return
}

// 5. Назначаем этого пользователя обратно
issue.setAssignee(previousAssignee)

// 6. Сохраняем изменения
def issueManager = ComponentAccessor.issueManager
issueManager.updateIssue(
    userManager.getUserByName("vi-robot-jira"), 
    issue, 
    EventDispatchOption.ISSUE_UPDATED, 
    false
)

log.warn("Задача ${issue.key}: назначен последний исполнитель ${previousAssignee.displayName}")
