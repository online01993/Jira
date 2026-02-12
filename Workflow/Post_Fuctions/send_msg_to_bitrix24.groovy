import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.user.ApplicationUser
import groovy.json.JsonSlurper
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager

// Получаем создателя задачи
ApplicationUser reporter = issue.getReporter()
String reporterEmail = reporter.getEmailAddress()

// если робот то не надо ему отправлять почту
if (reporter.name == 'vi-robot-jira' || reporter.name == 'vi-robot-monitoring') {
    return true
}

// Логируем для отладки
log.info("Reporter: ${reporter.getDisplayName()}, Email: ${reporterEmail}")

// 1) API запрос для получения ID пользователя в Bitrix24 по email
String bitrixUserSearchUrl = "https://vashinvest.bitrix24.ru/rest/16018/<<SUPER_SECRET_KEY>>/user.search?FILTER%5BEMAIL%5D=${reporterEmail}"

def bitrixUserId = null

try {
    def connection = new URL(bitrixUserSearchUrl).openConnection()
    connection.setRequestMethod("GET")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Accept", "application/json")
    
    int responseCode = connection.getResponseCode()
    
    if (responseCode == 200) {
        def response = connection.getInputStream().getText()
        def jsonSlurper = new JsonSlurper()
        def result = jsonSlurper.parseText(response)
        
        // Проверяем результат
        if (result.result && result.result.size() > 0) {
            bitrixUserId = result.result[0].ID
            log.info("Bitrix24 User ID found: ${bitrixUserId}")
        } else {
            log.warn("User not found in Bitrix24 for email: ${reporterEmail}")
        }
    } else {
        log.error("Failed to search Bitrix24 user. Response code: ${responseCode}")
    }
} catch (Exception e) {
    //log.error("Error searching Bitrix24 user: ${e.message}")
    CommentManager commentManager = ComponentAccessor.getCommentManager()
    def comment = """h2. *Сообщение в Битрикс24 не отправлено автоматически, поэтому необходимо вручную проинформировать пользователя либо обратиться к администратору Jira.*
    Код ошибки -- ${e.message}
    """
    final SD_PUBLIC_COMMENT = "sd.public.comment"
    def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
    def user = ComponentAccessor.getUserManager().getUserByName('robot')
    commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
}

// 2) Формируем текст сообщения msgSentToBitrix
String jiraIssueKey = issue.getKey()
String jiraIssueSummary = issue.getSummary()
String jiraIssueUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/" + jiraIssueKey
// Создаем текст сообщения с информацией о задаче
String msgSentToBitrix = """
Ваша заявка успешна зарегистрирована:
📌 Ключ: ${jiraIssueKey}
📋 Название: ${jiraIssueSummary}
🔗 Ссылка: ${jiraIssueUrl}
👤 Автор: ${reporter.getDisplayName()}
""".stripIndent().trim()
//log.info("Message to send: ${msgSentToBitrix}")

// 3) Отправляем сообщение в Bitrix24 чат, если нашли пользователя
if (bitrixUserId) {
    try {
        // Кодируем сообщение для URL
        String encodedMessage = URLEncoder.encode(msgSentToBitrix, "UTF-8")
        String bitrixSendMessageUrl = "https://vashinvest.bitrix24.ru/rest/16018/<<SUPER_SECRET_KEY>>/im.message.add?DIALOG_ID=${bitrixUserId}&MESSAGE=${encodedMessage}&SYSTEM=Y"
        def sendConnection = new URL(bitrixSendMessageUrl).openConnection()
        sendConnection.setRequestMethod("GET")
        sendConnection.setRequestProperty("Content-Type", "application/json")
        sendConnection.setRequestProperty("Accept", "application/json")
        int sendResponseCode = sendConnection.getResponseCode()        
        if (sendResponseCode == 200) {
            def sendResponse = sendConnection.getInputStream().getText()
            def sendJsonSlurper = new JsonSlurper()
            def sendResult = sendJsonSlurper.parseText(sendResponse)
            if (sendResult.result) {
                log.info("Message successfully sent to Bitrix24, message ID: ${sendResult.result}")
            } else {
                log.error("Failed to send message to Bitrix24: ${sendResult}")
            }
        } else {
            log.error("Failed to send message to Bitrix24. Response code: ${sendResponseCode}")
        }
    } catch (Exception e) {
        //log.error("Error sending message to Bitrix24: ${e.message}")
        CommentManager commentManager = ComponentAccessor.getCommentManager()
        def comment = """h2. *Сообщение в Битрикс24 не отправлено автоматически, поэтому необходимо вручную проинформировать пользователя либо обратиться к администратору Jira.*
        Код ошибки -- ${e.message}
        """
        final SD_PUBLIC_COMMENT = "sd.public.comment"
        def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
        def user = ComponentAccessor.getUserManager().getUserByName('robot')
        commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
    }
} else {
    //log.warn("Skipping Bitrix24 message - user ID not found")
    CommentManager commentManager = ComponentAccessor.getCommentManager()
    def comment = """h2. *Сообщение в Битрикс24 не отправлено автоматически, поэтому необходимо вручную проинформировать пользователя либо обратиться к администратору Jira.*
    Код ошибки -- Skipping Bitrix24 message - user ID not found
    """
    final SD_PUBLIC_COMMENT = "sd.public.comment"
    def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
    def user = ComponentAccessor.getUserManager().getUserByName('robot')
    commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
}
//return "Script completed"
return true
