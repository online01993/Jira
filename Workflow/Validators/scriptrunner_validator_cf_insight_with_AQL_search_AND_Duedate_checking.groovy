//Валидатор проверки блокируемых резолюций ожидания
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.Issue
import com.opensymphony.workflow.InvalidInputException


//def issue = Issues.getByKey('ITS-87885')
/* Custom field with the value to filter on (blocking)*/
def dueDateDateField = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10600))
def dueDateValue = dueDateDateField as Timestamp
/* Custom field for the waitng field */
CustomField cfwaitinginsight = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12400);
def valuecfwaitinginsight = issue.getCustomFieldValue(cfwaitinginsight);               
/* Get Insight Object Facade from plugin accessor */
Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade");  
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass);
/* Get Insight IQL Facade from plugin accessor */
Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);
// Получаем текущее время и текущий час
def currentDateTime = LocalDateTime.now()
def currentHour  = LocalDateTime.now().getHour()
// Форматируем время в нужный формат (ДД.ММ.ГГГГ - чч:мм:сс)
def formatterDateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")
def formattedDateTime = currentDateTime.format(formatterDateTime)
// Преобразуем отформатированное время в объект java.util.Date
def parsedDateTime = LocalDateTime.parse(formattedDateTime, formatterDateTime)

/*exit if valuecfwaitinginsight is empty      */
if (valuecfwaitinginsight == null) {
    throw new InvalidInputException("Ожидание", "Ожидание должно быть заполнено")
}

/*exit if valuecfwaitinginsight is empty      */
if (dueDateValue == null) {
    throw new InvalidInputException("Когда Выполнить", "Поле Когда Выполнить должно быть заполнено")
}

/* Get IQL search and return true?false       */
def objects = iqlFacade.findObjectsByIQLAndSchema(9, "Временные = true and Key = ID-" + valuecfwaitinginsight[0].getId()); 
if ((!objects.isEmpty()) && (dueDateValue.toLocalDateTime() <= currentDateTime.plusMinutes(1))) {
    throw new InvalidInputException("Когда Выполнить", "Для данной категории ожидающих резолюции требуется указание времени для возврата к работам")
   }

return true
