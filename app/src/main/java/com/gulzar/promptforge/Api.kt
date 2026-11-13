package com.gulzar.promptforge

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Api {
    private fun request(url:String, method:String="GET", token:String="", body:JSONObject?=null, github:Boolean=false):String {
        val c=URL(url).openConnection() as HttpURLConnection
        c.requestMethod=method; c.connectTimeout=30000; c.readTimeout=90000
        c.setRequestProperty("Accept", if(github) "application/vnd.github+json" else "application/json")
        c.setRequestProperty("Content-Type","application/json")
        if(token.isNotBlank()) c.setRequestProperty("Authorization", if(github) "Bearer $token" else "Bearer $token")
        if(github){c.setRequestProperty("X-GitHub-Api-Version","2022-11-28"); c.setRequestProperty("User-Agent","PromptForge-Android") }
        if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toString().toByteArray())}}
        val code=c.responseCode; val txt=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty()
        if(code !in 200..299) error("HTTP $code: ${txt.take(500)}")
        return txt
    }
    fun generate(config:AppConfig, prompt:String):Generation {
        val models=config.preferredModels.split(",").map{it.trim()}.filter{it.isNotBlank()}.distinct()
        var last:Throwable?=null
        for(model in models) try {
            val system="""You are an Android coding agent. Return ONLY valid JSON with this shape: {"summary":"...","files":[{"path":"relative/path","content":"complete file text"}]}. Never use absolute paths, .., binary data, secrets, workflow files, or .git paths. Generate complete compile-ready files. The repository is ${config.owner}/${config.repo}, branch ${config.branch}."""
            val context=runCatching{repositoryContext(config)}.getOrDefault("")
            val userPrompt=if(context.isBlank()) prompt else "$prompt\n\nExisting repository context:\n$context"
            val body=JSONObject().put("model",model).put("messages",JSONArray().put(JSONObject().put("role","system").put("content",system)).put(JSONObject().put("role","user").put("content",userPrompt))).put("temperature",0.2)
            val raw=request("https://openrouter.ai/api/v1/chat/completions","POST",config.openRouterKey,body)
            var text=JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
            if(text.startsWith("```")){ text=text.substringAfter('
').substringBeforeLast("```").trim() }
            val o=JSONObject(text); val a=o.getJSONArray("files"); val files=(0 until a.length()).map { i -> val f=a.getJSONObject(i); val p=f.getString("path"); require(!p.startsWith("/") && ".." !in p && !p.startsWith(".git") && !p.startsWith(".github/workflows")){"Unsafe path: $p"}; GeneratedFile(p,f.getString("content")) }
            require(files.isNotEmpty()){"AI returned no files"}; return Generation(o.optString("summary","Generated changes"),files)
        } catch(t:Throwable){ last=t }
        throw last ?: IllegalStateException("No free model available")
    }
    fun repositoryContext(c:AppConfig):String {
        val candidates=listOf("README.md","settings.gradle.kts","build.gradle.kts","app/build.gradle.kts")
        return candidates.mapNotNull { path -> runCatching {
            val u="https://api.github.com/repos/${c.owner}/${c.repo}/contents/$path?ref=${java.net.URLEncoder.encode(c.branch,"UTF-8")}"
            val o=JSONObject(request(u,token=c.githubToken,github=true));
            val decoded=String(Base64.decode(o.getString("content").replace("\n",""),Base64.DEFAULT)).take(12000)
            "--- $path ---\n$decoded"
        }.getOrNull() }.joinToString("\n")
    }

    fun testGithub(c:AppConfig):String = JSONObject(request("https://api.github.com/repos/${c.owner}/${c.repo}",token=c.githubToken,github=true)).getString("full_name")
    fun pushFile(c:AppConfig, file:GeneratedFile, message:String) {
        val url="https://api.github.com/repos/${c.owner}/${c.repo}/contents/${file.path.split('/').joinToString("/"){java.net.URLEncoder.encode(it,"UTF-8").replace("+","%20")}}"
        val sha=runCatching{JSONObject(request("$url?ref=${java.net.URLEncoder.encode(c.branch,"UTF-8")}",token=c.githubToken,github=true)).getString("sha")}.getOrNull()
        val b=JSONObject().put("message",message).put("content",Base64.encodeToString(file.content.toByteArray(),Base64.NO_WRAP)).put("branch",c.branch); if(sha!=null)b.put("sha",sha)
        request(url,"PUT",c.githubToken,b,true)
    }
}
