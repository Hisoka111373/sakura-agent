package com.sakura.agent

/** Bounded public-page conversion; never evaluates JavaScript or follows links. */
object WebText {
    fun plain(html: String): String = html.take(262144)
        .replace(Regex("(?is)<script\\b.*?</script\\s*>")," ")
        .replace(Regex("(?is)<style\\b.*?</style\\s*>")," ")
        .replace(Regex("(?is)<!--.*?-->")," ")
        .replace(Regex("(?is)<br\\s*/?>|</(?:p|div|li|h[1-6]|tr|section|article)>"),"\n")
        .replace(Regex("(?s)<[^>]+>")," ")
        .replace("&nbsp;"," ").replace("&amp;","&").replace("&lt;","<")
        .replace("&gt;",">").replace("&quot;","\"").replace("&#39;","'")
        .lines().map { it.replace(Regex("[ \\t\\r]+")," ").trim() }
        .filter { it.isNotEmpty() }.joinToString("\n").take(12000)
}