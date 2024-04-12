import java.sql.Timestamp
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

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
/* Custom field for the waitng field */
CustomField cfwaitinginsight = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12400);
def valuecfwaitinginsight = issue.getCustomFieldValue(cfwaitinginsight);               
/* Get Insight Object Facade from plugin accessor */
Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade");  
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass);
/* Get Insight IQL Facade from plugin accessor */
Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);
/*Get Attribute value from Assets of CustomField */
def valuecfwaitinginsightAttrBean = objectFacade.loadObjectAttributeBean(valuecfwaitinginsight[0].getId(), 'Задержка')
def valuecfwaitinginsightAttr = valuecfwaitinginsightAttrBean.getObjectAttributeValueBeans()[0].getValue() as Long


// Правило опредление автоматики выставления времени для поля когда выполнить
if (dueDateValue.toLocalDateTime() <= currentDateTime.plusMinutes(1)) {
    // Поле Когда выполнить меньше текущего времени а значит руками НЕ выставлено и нужно автоматически прописать новое значение
    //if ((currentHour >= 9) && (currentHour < 21) && (dueDateValue.toLocalDateTime().getHour() + 1 <= 21)) {// В текущем дне возвращаем
    if ((currentHour >= 9) && (currentHour < 21) && (currentHour + valuecfwaitinginsightAttr < 21)) {// В текущем дне возвращаем
        issue.set {
            // Генерируем новое время
            //def newDateTime = LocalDateTime.now().clearTime().plusDays(1).plusHours(1).plusMinutes(1)
            def newDateTime = LocalDateTime.now().plusHours(valuecfwaitinginsightAttr)            
            // Форматируем время в нужный формат (ДД.ММ.ГГГГ - чч:мм:сс)
            def formatternewDateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")
            def formattednewDateTime = newDateTime.format(formatternewDateTime)
            // Преобразуем отформатированное время в объект java.util.Date
            def parsednewDateTime = LocalDateTime.parse(formattednewDateTime, formatternewDateTime)
            // Устанавливаем новое значение
            setCustomFieldValue('Когда Выполнить', parsednewDateTime)
        }
    }
    else {
        if ((currentHour > 21) || (currentHour + valuecfwaitinginsightAttr > 21)) {// Переносим на следующий день
            issue.set {
                // Генерируем новое время
                //def newDateTime = LocalDateTime.now().clearTime().plusDays(1).plusHours(1).plusMinutes(1)
                def newDateTime = LocalDateTime.now().clearTime().plusDays(1).plusHours(9 + valuecfwaitinginsightAttr)
                // Форматируем время в нужный формат (ДД.ММ.ГГГГ - чч:мм:сс)
                def formatternewDateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")
                def formattednewDateTime = newDateTime.format(formatternewDateTime)
                // Преобразуем отформатированное время в объект java.util.Date
                def parsednewDateTime = LocalDateTime.parse(formattednewDateTime, formatternewDateTime)
                // Устанавливаем новое значение
                setCustomFieldValue('Когда Выполнить', parsednewDateTime)
            }
        }
    }
}
else {
    // Поле Когда выполнить больше текущего времени а значит руками выставлено и перезаписывать не нужно
    return null
}
