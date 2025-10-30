package com.example.coded

import com.example.coded.data.EnhancedMessageRepository

object AppModule {
    val messageRepository by lazy { EnhancedMessageRepository() }
}