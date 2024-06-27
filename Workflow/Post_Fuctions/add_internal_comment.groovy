import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject

CommentManager commentManager = ComponentAccessor.getCommentManager()
def comment = """h2. *Задача не была автоматически распределена по следующей(-им) причине(-ам):*
    """
 
comment = comment + '-- значение поля Требуется выезд изменено\n'
final SD_PUBLIC_COMMENT = "sd.public.comment"
def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
def user = ComponentAccessor.getUserManager().getUserByName('robot')
commentManager.create(issue,user, comment, null, null, new Date(),properties,false)
