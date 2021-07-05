import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import javax.imageio.ImageIO

class ImageProcessor(scope : CoroutineScope, outFile : String, bufferSize : Int) : CoroutineScope by scope {

    //Creating our HTTPClient - we shouldn't just automatically follow redirects, but we will for the purpose of this exercise
    private var client : HttpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
    private val file : File = File(outFile)
    private val outputFileWriter : FileWriter = FileWriter(file)
    private val bufferSize = bufferSize

    init {
        //Write the header row of the csv file.  Also, we should use a csv writer
        outputFileWriter.write("url,color1,color2,color3" + "\n")
    }

    data class ImageBytes(
        val url: String,
        val bytes: ByteArray
    )
    data class GetBytesArrayRequest(val url: String,
                                    val imageBytesChannel: SendChannel<ImageBytes>)

    data class ImageColors(val url: String,
                           val color1: Int,
                           val color2: Int,
                           val color3: Int)

    data class GetImageColorsRequest(
        val url: String, val bytes: ByteArray,
        val colorsChannel: SendChannel<ImageColors>)

    private val imageRetriever: SendChannel<GetBytesArrayRequest> = actor {
        consumeEach {
            val request = HttpRequest.newBuilder().uri(URI.create(it.url)).build()
            try {
                val response = client.send(request, BodyHandlers.ofByteArray())
                it.imageBytesChannel.send(ImageBytes(it.url, response.body()))
            } catch (e: IOException) {
                //TODO we need to handle this exception
            }
        }
    }

    suspend fun processImageRetrieverRequest(r1: String): ImageBytes {
        return select {
            val chan = Channel<ImageBytes>(bufferSize / 2)
            val request = GetBytesArrayRequest(r1, chan)
            imageRetriever.onSend(request) {
                chan.receive()
            }
        }
    }

    private val colorsRetriever: SendChannel<GetImageColorsRequest> = actor {
        consumeEach {
            val colorFrequencies : MutableMap<Int,Int> = mutableMapOf()

            val img = it.bytes.inputStream().use{stream->
                ImageIO.read(stream)
            }
            if(img != null){
                for (y in 0 until img.height){
                    for(x in 0 until img.width){
                        val pixel = img.getRGB(x,y)
                        colorFrequencies[pixel] = colorFrequencies[pixel]?.plus(1) ?: 1
                    }
                }
                val sortedMap = colorFrequencies.entries.sortedByDescending { (_,value) -> value }
                val firstThree = sortedMap.asSequence().take(3)
                it.colorsChannel.send(
                    ImageColors(
                        it.url,
                        firstThree.elementAt(0).key,
                        firstThree.elementAt(1).key,
                        firstThree.elementAt(2).key
                    )
                )
            }
            else{
                it.colorsChannel.send(ImageColors("", 0, 0, 0))
            }
            it.colorsChannel.close()
        }
    }

    suspend fun processColorsRetrieverRequest(url: String, bytes: ByteArray): ImageColors {
        return select {
            val chan = Channel<ImageColors>(bufferSize / 2)
            val request = GetImageColorsRequest(url, bytes, chan)
            colorsRetriever.onSend(request) {
                chan.receive()
            }
        }
    }

    fun writeImageResults(imageColors: ImageColors){
        if(imageColors.url.isNotEmpty()){
            val color1 = getHexString(imageColors.color1)
            val color2 = getHexString(imageColors.color2)
            val color3 = getHexString(imageColors.color3)
            outputFileWriter.write(imageColors.url + "," + color1 + "," + color2 + "," + color3 + "\n")
        }
    }

    private fun getHexString(color : Int) : String{
        return "#%02X%02X%02X".format(
            ( color and 0xff0000 ).shr(16),
            ( color and 0xff00 ).shr(8),
            ( color and 0xff )
        )
    }

    fun shutdown() {
        imageRetriever.close()
        colorsRetriever.close()
        outputFileWriter.close()
    }
}