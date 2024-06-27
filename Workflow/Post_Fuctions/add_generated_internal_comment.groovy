import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.model.ChangeItem

def externalWorkValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10402)).toString()
def infoSysValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11105)).toString()
def officeValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10501)).toString()
def workTypeStartValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12503))
def workTypeEndValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11101))
def workStartValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12501))
def workEndValue = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106))
def externalWorkchangeItems = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue,'Требуется выезд')
def infoSyschangeItems = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue,'Инфосистема')
def officechangeItems = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue,'Офис_Подразделение')
def external_work_changed = false
def infosys_changed = false 
def office_changed = false 
def workType_changed = false 
def work_changed = false
// Проверка изменений поля 'Требуется выезд'
if (externalWorkchangeItems) {
    def externalWorklastValue = externalWorkchangeItems.last().getFromString()
    external_work_changed = true
}
// Проверка изменений поля 'Инфосистема'
if (infoSyschangeItems) {
    def infoSyslastValue = infoSyschangeItems.last().getFromString()
    infosys_changed = true
}
// Проверка изменений поля 'Офис_Подразделение'
if (officechangeItems) {
    def officelastValue = officechangeItems.last().getFromString()
    office_changed = true
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
if (external_work_changed == true || infosys_changed == true || office_changed == true || workType_changed == true || work_changed == true) {
    def comment = """h2. *Задача не была автоматически распределена по следующей(-им) причине(-ам):*
    """
    if (external_work_changed == true) {
        comment = comment + '-- значение поля Требуется выезд изменено\n'
    }
    if (infosys_changed == true) {
        comment = comment + '-- значение поля Инфосистема изменено. Работы были выполнены для другой инфосистеме -' + infoSyschangeItems.last().getFromString() + '\n'
    }
    if (infosys_changed == true) {
        comment = comment + '-- значение поля Офис_Подразделение изменено. Актуальное значение поля -' + officechangeItems.last().getFromString() + '\n'
    }
    if (workType_changed == true) {
        comment = comment + '-- тип выполненной работы отличается от планируемого типа работы\n'
    }
    if (work_changed == true) {
        comment = comment + '-- выполненная работа отличается от планируемой работы\n'
    }
    final SD_PUBLIC_COMMENT = "sd.public.comment"
    def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
    ComponentAccessor.getCommentManager().create(issue,ComponentAccessor.getUserManager().getUserByName('robot'), comment, null, null, new Date(),properties,false)    
}
