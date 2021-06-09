package ktx

import com.github.kotlintelegrambot.entities.User

fun UserfromId(id: Long, isBot: Boolean) = User(id = id, isBot = isBot, firstName = "")