import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.io.File

class FindImageColors {

    private val processedUrls = mutableListOf<String>()

    private fun readFileLineByLine(fileName: String): List<String> = File(fileName).useLines { it.toList() }

    @ExperimentalCoroutinesApi
    fun processImageFile(inFile : String, outFile :String, checkForDuplicates : Boolean, workers : Int, bufferSize : Int) = runBlocking {
        val startingProgramTime = System.nanoTime()
        val imageProcessor = ImageProcessor(this, outFile, bufferSize)
        val urlList = readFileLineByLine(inFile)

        val urlChannel = produce {
            for(url in urlList){send(url)}
        }
        coroutineScope {
            repeat(workers){
                launch{processImageUrls(urlChannel, checkForDuplicates, imageProcessor)}

            }
        }
        //Shutdown our image processor
        imageProcessor.shutdown()

        val finishProgramTime = System.nanoTime()
        //This should be logged, but printing program runtime for now
        println((finishProgramTime - startingProgramTime) / 1000000)
    }

    private suspend fun processImageUrls(imageUrls : ReceiveChannel<String>, checkForDuplicates: Boolean, imageProcessor : ImageProcessor){
        for(url in imageUrls){
            coroutineScope {
                //If we want to check for duplicates we need to keep track of our processed urls
                if(checkForDuplicates){
                    if(!processedUrls.contains(url)){
                        processImages(url, imageProcessor)
                        processedUrls.add(url)
                    }
                }else{
                    processImages(url, imageProcessor)
                }
            }
        }
    }

    private suspend fun processImages(url : String, imageProcessor: ImageProcessor){
        val imageBytes = imageProcessor.processImageRetrieverRequest(url)
        val imageColors = imageProcessor.processColorsRetrieverRequest(imageBytes.url, imageBytes.bytes)
        imageProcessor.writeImageResults(imageColors)
    }
}