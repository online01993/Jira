/*import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.jira.issue.MutableIssue*/
//def issue = Issues.getByKey('ITS-30802') as MutableIssue
////
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule IQLFacade iqlFacade

final def resolutionBefore = 'ID-'
final def resolutionAfter = 'ID-'
final def infosysList = 'CMDB-0000, CMDB-1111, CMDB-2222, CMDB-3333'


/* Custom field for the waitng field */
CustomField resolutionCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106);
final def searchJql = 'Организация is not EMPTY and Контакт is not EMPTY and Офис_Подразделение is not EMPTY and resolution is not EMPTY and Решение = ' + resolutionBefore + 'and Инфосистема in aqlFunction(\'"Тип устройства" in (' + infosysList + ')\')'
def issues = Issues.search(searchJql.toString())
def i
//return searchJql
for (issue in issues) {
    def valueresolutionCF = issue.getCustomFieldValue(resolutionCF);               
    /* Get Insight IQL Facade from plugin accessor */
    //Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().loadClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
    //def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);

    /* Get IQL search and return true?false       */
    if (!valueresolutionCF) { return issue }
    //return valueresolutionCF.size()
    for (i = 0; i < valueresolutionCF.size(); i++) {
        if (valueresolutionCF[i].getObjectKey() == resolutionBefore.toString()) {
            issue.update {
                setCustomFieldValue('Решение') {remove(resolutionBefore.toString())}
            }
            issue.update {
                setCustomFieldValue('Решение') {add(resolutionAfter.toString())}
            }            
        }               
    }
}
