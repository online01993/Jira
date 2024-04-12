import java.sql.Timestamp
import com.atlassian.jira.component.ComponentAccessor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import com.opensymphony.workflow.InvalidInputException

/*
def issueManager = ComponentAccessor.getIssueManager()
def issue = issueManager.getIssueObject("ITS-87885")
*/
def dueDateDateField = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10600))
def dueDateValue = dueDateDateField as Timestamp

// Получаем текущее время и текущий час
def currentDateTime = LocalDateTime.now()
def currentHour  = LocalDateTime.now().getHour()
// Форматируем время в нужный формат (ДД.ММ.ГГГГ - чч:мм:сс)
def formatterDateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")
def formattedDateTime = currentDateTime.format(formatterDateTime)
// Преобразуем отформатированное время в объект java.util.Date
def parsedDateTime = LocalDateTime.parse(formattedDateTime, formatterDateTime)

/*exit if valuecfwaitinginsight is empty      */
if (dueDateValue == null) {
    throw new InvalidInputException("Когда Выполнить", "Поле Когда Выполнить должно быть заполнено")
}

// Правило опредление автоматики выставления времени для поля когда выполнить
if (dueDateValue.toLocalDateTime() > currentDateTime.plusDays(7)) {
    throw new InvalidInputException("Когда Выполнить", "Поле Когда Выполнить не должно быть больше 7 дней от текущего момента")
}
else {
    // Поле Когда выполнить указано верно
    return null
}
