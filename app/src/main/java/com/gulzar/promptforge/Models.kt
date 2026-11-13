package com.gulzar.promptforge

data class AppConfig(
    val openRouterKey:String="", val githubToken:String="",
    val owner:String="", val repo:String="", val branch:String="main",
    val preferredModels:String="openrouter/free,google/gemma-3-27b-it:free,nvidia/nemotron-3.5-lightning:free"
)
data class GeneratedFile(val path:String, val content:String, val selected:Boolean=true)
data class Generation(val summary:String, val files:List<GeneratedFile>)
