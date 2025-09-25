import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val extractor = M3U8Extractor()

    // Your encrypted data
    val encryptedResult = "kNywVny8bQYoC2kY_PXYbAU1N2E22qmN5NmgzjtGVReZD1Ql62x1bAOIHOFbqcpvWzOhTtvBXik_jNn-QetGc5J8qhvtyL55Wv_EYT1aIdDF1Ok8U3aXRvYFP3QbiG4FaPJ0sCeNOZkUGGX1kzpZnhbPBwLY0_Yc45YJv2eLCikjRvCWeldo-SGlQa3YRdu7IJT8vTiG9AxyuymjwetmzhIacKaguqo1-bKXnX4u9vhC57ZZgj-9WI4sL-2TWEePk2uf7794DDwu65Vp0I3uwdz-YFQtNxB9miknWLvrMB5Vn32-c_2CbUfUIGClGa-VfCszuZ9A0dk9nRvwEwrHrqrMyCFE57BRlFYdS79LmNXuxiJrw88a5v5xOgaMn5i-0ihVH8e3B5WXUTxrH1vfLa6Il7OnrMt-kj2YjbmI4xGsNUvYrOfNF3SeO-LH09IF8P7i0ju34eyEpeHnfGLBNBjdKTRpCr4PajLkzuB5D3__KgAD0cesANV2F_9OrGlpAPUXcBDMSH5u0Y1UdMD6dPc9ltXWouUiUeq2UJDXAeTDqEM04i5M9fknrVkNoSD8Iupt754udkiu6NG0rBDhthGoV00x9ik1ezXAk9Z3tgFdIWCA1nKc7NOPGk-FDU_Eort6fatl8aY87KFWQW1lsW8i-Q5wzMlJ1VdmDKigKyPhIY22cHxUffJZr9s4jggu3ydP8egRW0c6m0BuVkcRcWHscTQGPGRpVREU1w3iaC4b9Vcsl9jz_3MZ8OdFyuKyL_NYBcKcnhUuA_urqZ38vW7dw-4HxpTC5US8xcEEZzeIrcM6doLSU_cuwc3Mlt8hGRJMXSeblfb4KPF5exLz-2AMmDA7NB4pZkfUCv1y4LpwvBES2V6-kXJWjMjAK3-E6-h6fuMSSCsaJm0hN6Y"

    // Your HTML data
    val htmlData = """
        <div class="server-note"> <p>You are watching <b>Episode 1</b></p> <span>If the current server is not working, please try switching to other servers.</span></div><div class="server-wrap"> <div class="server-type" data-tabs=".server-wrap .server-items"> <span class="tab active" data-id="sub"> <svg class="sub"><use href="#sub"></use></svg> Hard Sub </span> <span class="tab " data-id="softsub"> <svg class="sub"><use href="#sub"></use></svg> Soft Sub </span> <span class="tab " data-id="dub"> <svg class="dub"><use href="#dub"></use></svg> Dub </span> </div> <div class="server-items lang-group" data-id="sub" style="display: ;"> <span class="server" data-sid="3" data-eid="c4W9-aCp" data-lid="dIO-8KKj4w" >Server 1</span> <span class="server" data-sid="2" data-eid="c4W9-aCp" data-lid="dIO-8KKj4g" >Server 2</span> </div> <div class="server-items lang-group" data-id="softsub" style="display: none;"> <span class="server" data-sid="3" data-eid="c4S48aWh" data-lid="dIOz8aCh5Q" >Server 1</span> <span class="server" data-sid="2" data-eid="c4S48aWh" data-lid="dIOz8aCh5A" >Server 2</span> </div> <div class="server-items lang-group" data-id="dub" style="display: none;"> <span class="server" data-sid="3" data-eid="eoK78ac" data-lid="dIS_8a-j5w" >Server 1</span> <span class="server" data-sid="2" data-eid="eoK78ac" data-lid="dIS_8a-j5g" >Server 2</span> </div> </div>
    """.trimIndent()

    // Extract m3u8 URLs
    val videoSources = extractor.extractM3U8Urls(encryptedResult, htmlData)

    println("\n=== Extracted M3U8 URLs ===")
    videoSources.forEach { source ->
        println("Server: ${source.type}")
        println("Quality: ${source.quality}")
        println("URL: ${source.url}")
        println("---")
    }

    // Example of how to use a specific server (Server 1 from Hard Sub)
    val servers = extractor.parseServerData(htmlData)
    val server1 = servers.find { it.serverName == "Server 1" && it.eid == "c4W9-aCp" }

    if (server1 != null) {
        val decodedData = extractor.decodeObfuscatedData(encryptedResult)
        val m3u8Url = extractor.generateM3U8Url(server1, decodedData)
        println("\nSpecific Server 1 URL: $m3u8Url")
    }
}
