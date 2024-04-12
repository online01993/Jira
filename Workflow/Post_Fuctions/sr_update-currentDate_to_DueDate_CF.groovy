//import com.atlassian.jira.component.ComponentAccessor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

/*
def issueManager = ComponentAccessor.getIssueManager()
def issue = issueManager.getIssueObject("ITS-87885")
*/

// Получаем текущее время и текущий час
def currentDateTime = LocalDateTime.now()
// Форматируем время в нужный формат (ДД.ММ.ГГГГ - чч:мм:сс)
def formatterDateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")
def formattedDateTime = currentDateTime.format(formatterDateTime)
// Преобразуем отформатированное время в объект java.util.Date
def parsedDateTime = LocalDateTime.parse(formattedDateTime, formatterDateTime)
// Устанавливаем новое значение
issue.set {
    setCustomFieldValue('Когда Выполнить', parsedDateTime)
}
