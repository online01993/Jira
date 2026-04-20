import groovy.json.JsonOutput
import org.apache.log4j.Logger
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.http.entity.ContentType
import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonSlurper
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager


// ================= НАСТРОЙКИ =================
def BITRIX_WEBHOOK = "https://vashinvest.bitrix24.ru/rest/<<TOKEN_ID>>/<<TOKEN_KEY>>/"
def STATIC_IBLOCK_TYPE_ID = "lists"
def STATIC_IBLOCK_ID = "198"
def STATIC_NAME = "СПР#"
def STATIC_PROPERTY_3398 = "1416"
// ============================================ 
//def  = Logger.getLogger("bitrix24.incident.hook")
//log.info("Начинаем обработку инцидента ${issue.key} для отправки в Битрикс24")

try {
    // ------------------ ПОЛУЧЕНИЕ ДАННЫХ ИЗ JIRA ------------------
    def issueKey = issue.key
    // Убираем дефис, так как в Битрикс он может вызвать проблемы в URL
    def elementCode = "${issueKey.replaceAll('-', '')}" 
    
    // Даты
    def dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
    def strDateStart = dateFormat.format(issue.created)
    def strDateEnd = dateFormat.format(issue.resolutionDate ?: new Date())

    // Описание с заменой переносов на %0A (хотя в POST это не обязательно, но оставим как ТЗ)
    def description = issue.description ?: "Описание отсутствует"
    //def descriptionEncoded = description.replaceAll("\r\n", "%0A").replaceAll("\n", "%0A")
    def descriptionEncoded = description + """. 
    Задача в Jira - ${issue.url}
    """

    // ------------------ ПОИСК ID СОТРУДНИКА В БИТРИКС (через POST) ------------------
    def reporterEmail = issue.reporter?.emailAddress
    String bitrixUserId = null

    if (reporterEmail) {
        try {
            def searchUrl = "${BITRIX_WEBHOOK}user.search"
            def httpClient = HttpClients.createDefault()
            def post = new HttpPost(searchUrl)
            
            // Тело запроса для поиска
            def searchBody = [FILTER: [EMAIL: reporterEmail]]
            post.setEntity(new StringEntity(JsonOutput.toJson(searchBody), ContentType.APPLICATION_JSON))
            
            def response = httpClient.execute(post)
            def responseText = EntityUtils.toString(response.entity)
            def json = new JsonSlurper().parseText(responseText)

            if (json.result && json.result.size() > 0) {
                bitrixUserId = json.result[0].ID
                //log.info("Найден ID пользователя Битрикс: ${bitrixUserId} для ${reporterEmail}")
            } else {
                //log.warn("Пользователь с email ${reporterEmail} не найден в Битрикс24.")
                CommentManager commentManager = ComponentAccessor.getCommentManager()
                def comment = """Пользователь с email ${reporterEmail} не найден в Битрикс24 для фиксации СПР
                Код ошибки -- ${e.message}
                """
                final SD_PUBLIC_COMMENT = "sd.public.comment"
                def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
                def user = ComponentAccessor.getUserManager().getUserByName('vi-robot-jira')
                commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
            }
            post.releaseConnection()
        } catch (Exception e) {
            log.error("Ошибка поиска пользователя в Битрикс", e)
            CommentManager commentManager = ComponentAccessor.getCommentManager()
            def comment = """Ошибка поиска пользователя в Битрикс"
            Код ошибки -- ${e.message}
            """
            final SD_PUBLIC_COMMENT = "sd.public.comment"
            def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
            def user = ComponentAccessor.getUserManager().getUserByName('vi-robot-jira')
            commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
        }
    }

    // ------------------ ФОРМИРОВАНИЕ ТЕЛА POST-ЗАПРОСА ------------------
    def fields = [
        NAME: STATIC_NAME,
        PROPERTY_952: strDateStart,
        PROPERTY_972: strDateEnd,
        PREVIEW_TEXT: descriptionEncoded,
        PROPERTY_3398: STATIC_PROPERTY_3398
    ]

    if (bitrixUserId) {
        fields.PROPERTY_3508 = bitrixUserId
    }

    def requestBody = [
        IBLOCK_TYPE_ID: STATIC_IBLOCK_TYPE_ID,
        IBLOCK_ID: STATIC_IBLOCK_ID,
        ELEMENT_CODE: elementCode,
        FIELDS: fields
    ]

    // ------------------ ОТПРАВКА POST-ЗАПРОСА В БИТРИКС ------------------
    def addUrl = "${BITRIX_WEBHOOK}lists.element.add"
    //log.debug("Отправка POST запроса на ${addUrl}")
    //log.debug("Тело запроса (без base64): " + JsonOutput.toJson(requestBody).replaceAll("\"PROPERTY_3400\":\"[^\"]+\"", "\"PROPERTY_3400\":\"[BASE64_HIDDEN]\""))

    def httpClient = HttpClients.createDefault()
    def post = new HttpPost(addUrl)
    post.setHeader("Content-Type", "application/json")
    post.setEntity(new StringEntity(JsonOutput.toJson(requestBody), "UTF-8"))

    def response = httpClient.execute(post)
    def responseCode = response.statusLine.statusCode
    def responseText = EntityUtils.toString(response.entity)
    
    //log.debug("Код ответа: ${responseCode}")
    //log.debug("Тело ответа: ${responseText}")

    def jsonResponse = new JsonSlurper().parseText(responseText)

    if (responseCode == 200 && !jsonResponse.error) {
        //log.info("Инцидент ${issueKey} успешно зафиксирован в Битрикс24. ID элемента: ${jsonResponse.result}")
        // Опционально: Добавляем комментарий в задачу
        CommentManager commentManager = ComponentAccessor.getCommentManager()
        def comment = "✅ Данные успешно переданы в Битрикс24 (СПР) -- ${jsonResponse.result}"
        final SD_PUBLIC_COMMENT = "sd.public.comment"
        def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
        def user = ComponentAccessor.getUserManager().getUserByName('vi-robot-jira')
        commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
    } else {
        //log.error("Ошибка при отправке инцидента ${issueKey}. Код: ${responseCode}, Ответ: ${responseText}")
        CommentManager commentManager = ComponentAccessor.getCommentManager()
        def comment = "Ошибка при отправке инцидента ${issueKey} в СПР Битрикс24. Код: ${responseCode}, Ответ: ${responseText}"
        final SD_PUBLIC_COMMENT = "sd.public.comment"
        def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
        def user = ComponentAccessor.getUserManager().getUserByName('vi-robot-jira')
        commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
    }
    
    post.releaseConnection()

} catch (Exception e) {
    log.error("Критическая ошибка при выполнении скрипта для задачи ${issue?.key}", e)
    CommentManager commentManager = ComponentAccessor.getCommentManager()
    def comment = """Критическая ошибка при выполнении скрипта для задачи"
    Код ошибки -- ${e.message}
    """    
    final SD_PUBLIC_COMMENT = "sd.public.comment"
    def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
    def user = ComponentAccessor.getUserManager().getUserByName('vi-robot-jira')
    commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
}
