package com.sakura.agent

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keystore key + AES-GCM. No backups, plaintext tokens, or automatic chat persistence. */
class PrivateStore(context: Context) {
    private val preferences = context.getSharedPreferences("sakura_private", Context.MODE_PRIVATE)
    private val alias = "sakura_private_v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }

    fun read(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        val parts = encoded.split(':')
        require(parts.size == 2) { "Almacenamiento privado inválido." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    fun write(name: String, value: String?) {
        if (value == null) {
            check(preferences.edit().remove(name).commit()) { "No se pudo borrar el dato." }
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val result = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString(name, result).commit()) { "No se pudo guardar el dato." }
    }

    fun memories(): List<Memory> {
        val array = JSONArray(read("memories") ?: "[]")
        return (0 until array.length()).map {
            val item = array.getJSONObject(it)
            Memory(item.getString("id"), item.getString("kind"), item.getString("text"))
        }
    }

    fun saveMemories(values: List<Memory>) {
        val array = JSONArray()
        values.forEach { array.put(JSONObject().put("id", it.id).put("kind", it.kind).put("text", it.text)) }
        write("memories", array.toString())
    }
}
