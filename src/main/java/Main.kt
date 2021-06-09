import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.dispatcher.newChatMembers
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import math.Expression
import math.operator.Times
import math.term.X
import math.unaryPlus
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.properties.Delegates

val map = mutableMapOf<Long, MutableMap<Long, MutableMap<String, Any>>>()
lateinit var bot: Bot
var botId by Delegates.notNull<Long>()

fun main(args: Array<String>) {
    botId = args[0].substring(0, args[0].indexOf(':') - 1).toLong()
    println(botId)
    bot = bot {
        token = args[0]
        logLevel = LogLevel.Error
        proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", 7890))
        dispatch {
            newChatMembers {
                newChatMembers.forEach {
                    if (it.id == botId)
                        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "已将 Bot 添加至本群，在授予管理权限后方可使用。")
                    else
                        startVerification(it, message.chat)
                }
            }
            message {
                map[message.chat.id]?.let { userMap ->
                    userMap[message.from?.id]?.let {
                        if (it["answer"] == message.text) {
                            endVerification(message.from!!, message.chat)
                        } else {
                            bot.deleteMessage(chatId = ChatId.fromId(message.chat.id), messageId = message.messageId)
                            bot.deleteMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                messageId = it["messageId"] as Long
                            )
                            it["messageId"] =
                                sendVerificationMessage(message.from!!, message.chat, userText = message.text)
                        }
                    }
                }
            }
            callbackQuery {
                if (callbackQuery.data.contains('@')) {
                    val callbackDataList = callbackDataToList(callbackQuery.data)
                    when (callbackQuery.data.substring(callbackQuery.data.lastIndexOf('@') + 1)) {
                        "passByAdmin" -> if (isAdmin(callbackQuery.from, callbackQuery.message!!.chat))
                            endVerification(callbackDataList[1].toLong(), callbackDataList[0].toLong())
                        else
                            bot.sendMessage(chatId = ChatId.fromId(callbackQuery.message!!.chat.id), text = "禁止自娱自乐。")
                        "banByAdmin" -> if (isAdmin(callbackQuery.from, callbackQuery.message!!.chat))
                            bot.kickChatMember(
                                chatId = ChatId.fromId(callbackDataList[0].toLong()),
                                userId = callbackDataList[1].toLong()
                            )
                        else
                            bot.sendMessage(chatId = ChatId.fromId(callbackQuery.message!!.chat.id), text = "禁止自娱自乐。")
                    }
                } else {
                    when (callbackQuery.data) {

                    }
                }
            }
            command("start") {
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Hi There!")
            }
            command("testbot") {
                //bot.sendMessage(ChatId.fromId(message.chat.id),text = message.text!!)
                message.from?.let { startVerification(it, message.chat) }
            }
        }
    }
    bot.startPolling()
}

fun startVerification(user: User, chat: Chat) {
    map.putIfAbsent(chat.id, mutableMapOf())
    val expression = generateExpression()
    map[chat.id]?.put(
        user.id,
        mutableMapOf(
            "expression" to expression.first,
            "answer" to expression.second,
            "messageId" to sendVerificationMessage(user, chat, expression)
        )
    )
    println("-------------------------------------------------------------")
    println("用户 ${user.username}(${user.id}) 在 ${chat.title}(${chat.id}) 开始验证")
    println(expression.first)
    println(expression.second)
    println("-------------------------------------------------------------")
    println()
}

fun endVerification(user: User, chat: Chat, userMap: MutableMap<Long, MutableMap<String, Any>>? = map[chat.id]) {
    userMap?.get(user.id)?.let {
        bot.deleteMessage(chatId = ChatId.fromId(chat.id), messageId = it["messageId"] as Long)
        if (it.size == 1) map.remove(chat.id) else userMap.remove(user.id)
        bot.sendMessage(chatId = ChatId.fromId(chat.id), text = "${it["expression"]} ==> ${it["answer"]}\n\n验证通过。")
        println("-------------------------------------------------------------")
        println("用户 ${user.username}(${user.id}) 在 ${chat.title}(${chat.id}) 结束验证")
        println(it["expression"])
        println(it["answer"])
        println("-------------------------------------------------------------")
        println()
    }
}

fun endVerification(userId: Long, chatId: Long) =
    endVerification(User(id = userId, isBot = false, firstName = ""), Chat(id = chatId, type = ""))

fun generateExpression(intRange: IntRange = (1..10)): Pair<String, String> {
    val expression = Expression().apply {
        var canAddOperator = true
        for (i in intRange) {
            terms.add(X((-100..100).random(), (0..10).random()))
            if (((1..100).random() >= 50) and canAddOperator) {
                operators[i] = Times()
                canAddOperator = !canAddOperator
            } else {
                canAddOperator = !canAddOperator
            }
        }
    }
    return expression.toString() to expression.derivative().toString()
}

fun sendVerificationMessage(
    user: User,
    chat: Chat,
    expression: Pair<String, String>? = null,
    userText: String? = null
): Long {
    val inlineButton = InlineKeyboardMarkup.create(
        listOf(
            InlineKeyboardButton.CallbackData(text = "人工通过", callbackData = "${chat.id}:${user.id}@passByAdmin"),
            InlineKeyboardButton.CallbackData(text = "封禁", callbackData = "${chat.id}:${user.id}@banByAdmin")
        )
    )
    return if (expression == null) {
        bot.sendMessage(
            chatId = ChatId.fromId(chat.id),
            text = "答案错误${if (userText == null) "" else "：$userText"}\n\n${user.username}，请对以下表达式进行求导：\n${
                map[chat.id]?.get(user.id)?.get("expression")
            }",
            replyMarkup = inlineButton
        ).first?.body()?.result?.messageId!!
    } else {
        bot.sendMessage(
            chatId = ChatId.fromId(chat.id),
            text = "欢迎 ${user.username}\n请对以下表达式进行求导：\n${expression.first}",
            replyMarkup = inlineButton
        ).first?.body()?.result?.messageId!!
    }
}

fun isAdmin(user: User, chat: Chat) =
    bot.getChatAdministrators(ChatId.fromId(chat.id)).getOrDefault(listOf()).any { user.id == it.user.id }

fun callbackDataToList(string: String) = mutableListOf<String>().apply {
    var i = 0
    while (true) {
        add(string.substring(i, string.indexOfAny(listOf(":", "@"), i)))
        i = string.indexOf(':', i) + 1
        if (i == 0) break
    }
}.toList()