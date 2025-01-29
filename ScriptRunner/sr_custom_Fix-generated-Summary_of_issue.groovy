import com.atlassian.jira.issue.MutableIssue
//def issue = Issues.getByKey('ITS-185903') as MutableIssue
////
import com.atlassian.jira.issue.MutableIssue
import java.time.LocalDateTime
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade


@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule IQLFacade iqlFacade

/* Custom field for the waitng field */

final def searchJql = 'project = its and issuetype = "Сервисный запрос" AND resolution in (Готово, Закрыто) and "Сводный итог ИТ" ~ "Запланированных работ по запросу нет" and issueFunction in hasSubtasks()'
//final def searchJql = 'key = ITS-64319'
def issues = Issues.search(searchJql.toString())
def issue
for (issueObject in issues) {
    ////create subtasks to service request
    issue = Issues.getByKey(issueObject.key) as MutableIssue
    def organizationDescription = ''
    def officeDescription = ''
    def contactDescription = ''
    def workDescription = ''
    def infosysTypeValue = ''
    def infosysTypeDescription = ''
    def infosysDescription = ''
    def infosysContractValue = ''
    def worklogSumDescription = ''
    def issueAssignee = ''
    def summaryDescription
    def subTasks = issue.getSubTaskObjects()
    def subTasksCount
    int i

    if (subTasks == null) {
        subTasksCount = 0
    } else {
        subTasksCount = subTasks.size()
        for (i = 0; i < subTasks.size(); i++) {        
            if (subTasks[i].getResolutionId() == "10000" || subTasks[i].getResolutionId() == "10100") { //resolution must be Готово or Закрыто
                subTasksCount = subTasksCount
            } else {
                subTasksCount = subTasksCount - 1
            }
        }
        /*
        if (subTasks.size() > 1) {
            subTasksCount = subTasks.size()
        } else if (subTasks[0].getResolutionId() == "10900") {//exclude resolution Ошибка заведения задачи
            subTasksCount = 0
        } else {
            subTasksCount = 1
        }
        */
    }
    if (!subTasks.isEmpty() && subTasksCount > 0) {
        summaryDescription =  """
                h2. *Корневой запрос ИТ решён.*
                Текущая дата формирования отчёта - ${LocalDateTime.now().format("dd.MM.yyyy - HH:mm:ss")}.

                h3. Выполненные работы по запросу:
                ||Тикет||Резолюция||Организация||Офис||Контакт||Выполненная работа||Тип инфосистемы||Инфосистема||Договор инфосистемы||Инженер||Журнал работ, минуты||Платность||Выезд||Доп материалы||
                """
        //adding subtasks to task summary
        for (subTask in subTasks) {
            //if (subTask.getResolutionId() != "10900") {//exclude resolution Ошибка заведения задачи
            if (subTask.getResolutionId() == "10000" || subTask.getResolutionId() == "10100") {//resolution must be Готово or Закрыто
                //
                organizationDescription = "[" + subTask.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Организация')[0].getId() + "]"
                officeDescription = "[" + subTask.getCustomFieldValue('Офис_Подразделение')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Офис_Подразделение')[0].getId() + "]"
                contactDescription = "[" + subTask.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Контакт')[0].getId() + "]"
                if (!subTask.getCustomFieldValue('Решение')) {
                    workDescription = "Нет выполненной работы. Планируемая работа - [" + subTask.getCustomFieldValue('Планируемая работа')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Планируемая работа')[0].getId() + "]"
                } else {
                    workDescription = "[" + subTask.getCustomFieldValue('Решение')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Решение')[0].getId() + "]"
                }
                infosysTypeValue = iqlFacade.findObjects(2,'objectTypeId = 152 and object HAVING inboundReferences(key = ' + subTask.getCustomFieldValue('Инфосистема')[0].getObjectKey() + ')') 
                infosysTypeDescription = "[" + infosysTypeValue[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/CMDB-" + infosysTypeValue[0].getName().toString() + "]"
                infosysDescription = "[" + subTask.getCustomFieldValue('Инфосистема')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/CMDB-" + subTask.getCustomFieldValue('Инфосистема')[0].getId() + "]"
                if (subTask.getCustomFieldValue('Инфосистема')[0].getReference('Договор')) {
                    infosysContractValue = '[Да|https://help.it-russia.com/secure/insight/assets/CRM-' + subTask.getCustomFieldValue('Инфосистема')[0].getReference('Договор').getId() + "]"
                } else {
                    infosysContractValue = 'Нет'
                }
                if (subTask.timeSpent == 0 || subTask.timeSpent == null) {worklogSumDescription = 0} else {worklogSumDescription = subTask.timeSpent.div(60).toString()} 
                if (subTask.assignee) {
                    issueAssignee = subTask.getAssignee().getDisplayName()
                } else {
                    issueAssignee = 'robot'
                }
                summaryDescription = summaryDescription + "|${subTask.getKey()}|${subTask.getResolution().getName()}|${organizationDescription}|${officeDescription}|${contactDescription}|${workDescription}|${infosysTypeDescription}|${infosysDescription}|${infosysContractValue}|${issueAssignee}|${worklogSumDescription}|${subTask.getCustomFieldValue('Платная задача').toString()}|${subTask.getCustomFieldValue('Требуется выезд').toString()}|${subTask.getCustomFieldValue('Доп материалы').toString()}|" + '\n'
            }
        }
        //adding incidents to task summary
        //if (!issue.getCustomFieldValue('Инциденты').isEmpty()) {
        if (issue.getCustomFieldValue('Инциденты')) {
            summaryDescription = summaryDescription + """    
                    h3. Зафиксированные инциденты по запросу:
                    ||Тикет||Организация||Офис||Контакт||Платность||Выезд||Доп материалы||
            """
            for (i=0; i < issue.getCustomFieldValue('Инциденты').size(); i++) {
                def incident = Issues.getByKey(issue.getCustomFieldValue('Инциденты')[i].getKey()) as MutableIssue
                organizationDescription = "[" + incident.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Организация')[0].getId() + "]"
                officeDescription = "[" + incident.getCustomFieldValue('Офис_Подразделение')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Офис_Подразделение')[0].getId() + "]"
                contactDescription = "[" + incident.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Контакт')[0].getId() + "]"
                summaryDescription = summaryDescription + "|${incident.getKey()}|${organizationDescription}|${officeDescription}|${contactDescription}|${incident.getCustomFieldValue('Платная задача').toString()}|${incident.getCustomFieldValue('Требуется выезд').toString()}|${incident.getCustomFieldValue('Доп материалы').toString()}|" + '\n'
            }
        }
        //adding projects to task summary
        //if (!issue.getCustomFieldValue('Проекты').isEmpty()) {
        if (issue.getCustomFieldValue('Проекты')) {
            summaryDescription = summaryDescription + """    
                    h3. Зафиксированные проекты по запросу:
                    ||Тикет||Организация||Контакт||
            """
            for (i=0; i < issue.getCustomFieldValue('Проекты').size(); i++) {
                def projects = Issues.getByKey(issue.getCustomFieldValue('Проекты')[i].getKey()) as MutableIssue
                organizationDescription = "[" + projects.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + projects.getCustomFieldValue('Организация')[0].getId() + "]"
                contactDescription = "[" + projects.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + projects.getCustomFieldValue('Контакт')[0].getId() + "]"
                summaryDescription = summaryDescription + "|${projects.getKey()}|${organizationDescription}|${contactDescription}|" + '\n'
            }
        }
        if (summaryDescription.length() >= 32767) {
                summaryDescription = """
                    h2. *Корневой запрос ИТ решён.*
                    Текущая дата формирования отчёта - ${LocalDateTime.now().format("dd.MM.yyyy - HH:mm:ss")}.

                    h3. Выполненных работы по запросу слишком много, весь объем не помещается в единое поле, нужно обратиться к системному администратору за ручным формированием отчёта.
                    """
        }
        issue.update { 
            setEventDispatchOption(EventDispatchOption.DO_NOT_DISPATCH)
            setCustomFieldValue(12600, summaryDescription)
        }
    } else {
        summaryDescription =  """
            h2. *Корневой запрос ИТ решён.*
            Текущая дата формирования отчёта - ${LocalDateTime.now().format("dd.MM.yyyy - HH:mm:ss")}.

            h3. Запланированных работ по запросу нет.
            """
        
        //adding incidents to task summary
        //if (!issue.getCustomFieldValue('Инциденты').isEmpty()) {
        if (issue.getCustomFieldValue('Инциденты')) {
            summaryDescription = summaryDescription + """    
                    h3. Зафиксированные инциденты по запросу:
                    ||Тикет||Организация||Офис||Контакт||Платность||Выезд||Доп материалы||
            """
            for (i=0; i < issue.getCustomFieldValue('Инциденты').size(); i++) {
                def incident = Issues.getByKey(issue.getCustomFieldValue('Инциденты')[i].getKey()) as MutableIssue
                organizationDescription = "[" + incident.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Организация')[0].getId() + "]"
                officeDescription = "[" + incident.getCustomFieldValue('Офис_Подразделение')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Офис_Подразделение')[0].getId() + "]"
                contactDescription = "[" + incident.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Контакт')[0].getId() + "]"
                summaryDescription = summaryDescription + "|${incident.getKey()}|${organizationDescription}|${officeDescription}|${contactDescription}|${incident.getCustomFieldValue('Платная задача').toString()}|${incident.getCustomFieldValue('Требуется выезд').toString()}|${incident.getCustomFieldValue('Доп материалы').toString()}|" + '\n'
            }
        }
        //adding projects to task summary
        //if (!issue.getCustomFieldValue('Проекты').isEmpty()) {
        if (issue.getCustomFieldValue('Проекты')) {
            summaryDescription = summaryDescription + """    
                    h3. Зафиксированные проекты по запросу:
                    ||Тикет||Организация||Контакт||
            """
            for (i=0; i < issue.getCustomFieldValue('Проекты').size(); i++) {
                def projects = Issues.getByKey(issue.getCustomFieldValue('Проекты')[i].getKey()) as MutableIssue
                organizationDescription = "[" + projects.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + projects.getCustomFieldValue('Организация')[0].getId() + "]"
                contactDescription = "[" + projects.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + projects.getCustomFieldValue('Контакт')[0].getId() + "]"
                summaryDescription = summaryDescription + "|${projects.getKey()}|${organizationDescription}|${contactDescription}|" + '\n'
            }
        }
        issue.update { 
            setEventDispatchOption(EventDispatchOption.DO_NOT_DISPATCH)
            setCustomFieldValue(12600, summaryDescription)
        }
    }
}
return true 
