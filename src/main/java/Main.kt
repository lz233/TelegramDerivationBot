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
import java.net.InetSocketAddress
import java.net.Proxy

const val botId = 1768638849L

val map = mutableMapOf<Long, MutableMap<Long, MutableMap<String, Any>>>()

fun main() {
    val bot = bot {
        token = "1768638849:AAGSqWi4LJaCvIUwdBzYo6xjo9qS-qqtL1U"
        logLevel = LogLevel.All()
        proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", 7890))
        dispatch {
            newChatMembers {
                newChatMembers.forEach {
                    if (it.id == botId)
                        bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "已将 Bot 添加至本群，在授予管理权限后方可使用。")
                    else {
                        startVerification(bot, it, message.chat)
                    }
                }
            }
            message {
                map[message.chat.id]?.let { userMap ->
                    userMap[message.from?.id]?.let {
                        if (it["answer"].toString() == message.text) {
                            if (it.size == 1) map.remove(message.chat.id) else userMap.remove(message.from?.id)
                            bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "${it["expression"]}${it["answer"]}\n验证通过。")
                        } else {
                            bot.deleteMessage(chatId = ChatId.fromId(message.chat.id), messageId = message.messageId)
                        }
                    }
                }
            }
            callbackQuery {
                if (callbackQuery.data.contains('@')) {
                    val callbackDataList = callbackDataToList(callbackQuery.data)
                    when (callbackQuery.data.substring(callbackQuery.data.lastIndexOf('@')+1)) {
                        "passByAdmin" -> if (isAdmin(bot, callbackQuery.from, callbackQuery.message!!.chat))
                        endVerification(bot, User(id = callbackDataList[1].toLong(),isBot = false,firstName = ""), Chat(id = callbackDataList[0].toLong(),type = ""))
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
                message.from?.let { startVerification(bot, it, message.chat) }
            }
            command("testit") {
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                        listOf(InlineKeyboardButton.CallbackData(text = "人工通过", callbackData = "passByAdmin")),
                        listOf(InlineKeyboardButton.CallbackData(text = "Show alert", callbackData = "showAlert")))
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), replyMarkup = inlineKeyboardMarkup,
                        text = "text")
            }
        }
    }
    bot.startPolling()
}

fun startVerification(bot: Bot, user: User, chat: Chat) {
    map.putIfAbsent(chat.id, mutableMapOf())
    val expression = generateExpression()
    map[chat.id]?.put(user.id, mutableMapOf("expression" to expression.first, "answer" to expression.second))
    bot.sendMessage(chatId = ChatId.fromId(chat.id),
            text = "欢迎 ${user.username}\n请计算以下表达式：\n${expression.first}",
            replyMarkup = InlineKeyboardMarkup.create(
                    listOf(InlineKeyboardButton.CallbackData(text = "人工通过", callbackData = "${chat.id}:${user.id}@passByAdmin"),
                            InlineKeyboardButton.CallbackData(text = "封禁", callbackData = "233"))))
}

fun endVerification(bot: Bot, user: User, chat: Chat, userMap: MutableMap<Long, MutableMap<String, Any>>? = map[chat.id]) {
    userMap?.get(user.id)?.let {
        if (it.size == 1) map.remove(chat.id) else userMap.remove(user.id)
        bot.sendMessage(chatId = ChatId.fromId(chat.id), text = "${it["expression"]}${it["answer"]}\n验证通过。")
    }
}

fun generateExpression(intRange: IntRange = (1..100)): Pair<String, Int> {
    val a = intRange.random()
    val b = intRange.random()
    return "$a+$b=" to a + b
}

fun generateUsersButton(): List<List<KeyboardButton>> {
    return listOf(listOf(KeyboardButton("Request contact")), listOf(KeyboardButton("Request contact")))
}

fun isAdmin(bot: Bot, user: User, chat: Chat): Boolean {
    return with(user.id) {
        bot.getChatAdministrators(ChatId.fromId(chat.id)).getOrDefault(listOf()).forEach {
            if (this == it.user.id) return@with true
        }
        false
    }
}

fun callbackDataToList(string: String) = mutableListOf<String>().apply {
    var i = 0
    while (true) {
        add(string.substring(i, string.indexOfAny(listOf(":", "@"), i)))
        i = string.indexOf(':', i) + 1
        if (i == 0) break
    }
}.toList()