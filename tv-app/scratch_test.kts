import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

val page = URL("https://mkissa.to").readText()
val epoch = Regex(""""epoch":([0-9]+)""").find(page)?.groupValues?.get(1)
val partB = Regex(""""partB":"([^"]*)"""").find(page)?.groupValues?.get(1)
println("epoch: $epoch")
println("partB: $partB")

if (partB != null) {
    val partBBytes = Base64.getDecoder().decode(partB)
    println("partB length: ${partBBytes.size}")
}
