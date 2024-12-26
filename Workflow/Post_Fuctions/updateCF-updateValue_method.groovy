import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
//help -- https://habr.com/ru/companies/raiffeisenbank/articles/349876/
def parentIssue = issue.getParentObject() as MutableIssue
if (parentIssue.getCustomFieldValue('Customer Request Type').getValue() == 'its/23d6b2c9-4c2c-4f08-9c83-125b625f7001') {
    def orgField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Организация")[0]
    orgField.updateValue(null, parentIssue, new ModifiedValue("", issue.getCustomFieldValue('Организация')), new DefaultIssueChangeHolder())
    def contactField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Контакт")[0]
    contactField.updateValue(null, parentIssue, new ModifiedValue("", issue.getCustomFieldValue('Контакт')), new DefaultIssueChangeHolder())
}
