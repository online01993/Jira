import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.model.ChangeItem

def externalWorkValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10402))
def infoSysValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11105))
def organizationValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10301))
def officeValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10501))
def contactValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10300))
def workTypeStartValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12503))
def workTypeEndValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11101))
def workStartValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12501))
def workEndValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106))
def externalWorkchangeItems = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue,'Требуется выезд')
def infoSyschangeItems = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue,'Инфосистема')
def organizationchangeItems = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue,'Организация')
def officechangeItems = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue,'Офис_Подразделение')
def contactchangeItems = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue,'Контакт')
def external_work_changed = false
def infosys_changed = false 
def organization_changed = false 
def office_changed = false
def contact_changed = false  
def workType_changed = false 
def work_changed = false
// Проверка изменений поля 'Требуется выезд'
if (externalWorkchangeItems) {
    external_work_changed = true
}
// Проверка изменений поля 'Инфосистема'
if (infoSyschangeItems) {
    infosys_changed = true
}
// Проверка изменений поля 'Организация'
if (organizationchangeItems) {
    organization_changed = true
}
// Проверка изменений поля 'Офис_Подразделение'
if (officechangeItems) {
    office_changed = true
}
// Проверка изменений поля 'Контакт'
if (contactchangeItems) {
    contact_changed = true
}
// Проверка заявленного типа работы и итоговый тип работы
if (issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12503)) != issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11101))) {
    workType_changed = true
}
// Проверка заявленной работы и выполненной работой по итогу
if (issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12501)) != issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106))) {
    work_changed = true
}

//выводим комментарий с причиной почему автоматическое распределение невозможно
if (external_work_changed == true || infosys_changed == true || organization_changed == true || office_changed == true || contact_changed == true || workType_changed == true || work_changed == true) {
    def comment = """h2. *Задача не была автоматически распределена по следующей(-им) причине(-ам):*
    """
    if (external_work_changed == true) {
        comment = comment + '-- значение поля *"Требуется выезд"* изменено. Было - *' + externalWorkchangeItems.last().getFromString() + '*. Стало - *' + externalWorkValue.toString() + '*\n'
    }
    if (infosys_changed == true) {
        comment = comment + '-- значение поля *"Инфосистема"* изменено. Было - *' + infoSyschangeItems.last().getFromString() + '*. Стало - *' + infoSysValue[0].getName() + '*\n'
    }
    if (organization_changed == true) {
        comment = comment + '-- значение поля *"Организация"* изменено. Было - *' + organizationchangeItems.last().getFromString() + '*. Стало - *' + organizationValue[0].getName() + '*\n'
    }
    if (office_changed == true) {
        comment = comment + '-- значение поля *"Офис_Подразделение"* изменено. Было - *' + officechangeItems.last().getFromString() + '*. Стало - *' + officeValue[0].getName() + '*\n'
    }
    if (contact_changed == true) {
        comment = comment + '-- значение поля *"Контакт"* изменено. Было - *' + contactchangeItems.last().getFromString() + '*. Стало - *' + contactValue[0].getName() + '*\n'
    }
    if (workType_changed == true) {
        comment = comment + '-- *тип выполненной работы* отличается от планируемого типа работы. Было - *' + issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12503))[0].getName() + '*. Стало - *' + issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11101))[0].getName() + '*\n'
    }
    if (work_changed == true) {
        comment = comment + '-- *выполненная работа* отличается от планируемой работы. Было - *' + issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12501))[0].getName() + '*. Стало - *' + issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106))[0].getName() + '*\n'
    }
    final SD_PUBLIC_COMMENT = "sd.public.comment"
    def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
    ComponentAccessor.getCommentManager().create(issue,ComponentAccessor.getUserManager().getUserByName('robot'), comment, null, null, new Date(),properties,false)    
}
