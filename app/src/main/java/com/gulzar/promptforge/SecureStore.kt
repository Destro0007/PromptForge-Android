package com.gulzar.promptforge

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("promptforge", Context.MODE_PRIVATE)
    private val alias = "PromptForgeKey"
    private fun key(): SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (ks.getKey(alias,null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun enc(v:String):String { val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key()); return Base64.encodeToString(c.iv+c.doFinal(v.toByteArray()),Base64.NO_WRAP) }
    private fun dec(v:String):String = runCatching { val b=Base64.decode(v,Base64.NO_WRAP); val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,b.copyOfRange(0,12))); String(c.doFinal(b.copyOfRange(12,b.size))) }.getOrDefault("")
    fun load()=AppConfig(dec(prefs.getString("or","")!!),dec(prefs.getString("gh","")!!),prefs.getString("owner","")!!,prefs.getString("repo","")!!,prefs.getString("branch","main")!!,prefs.getString("models","openrouter/free,google/gemma-3-27b-it:free,nvidia/nemotron-3.5-lightning:free")!!)
    fun save(c:AppConfig){prefs.edit().putString("or",enc(c.openRouterKey)).putString("gh",enc(c.githubToken)).putString("owner",c.owner).putString("repo",c.repo).putString("branch",c.branch).putString("models",c.preferredModels).apply()}
}
