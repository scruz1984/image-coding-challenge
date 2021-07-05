fun main(args: Array<String>) {

    //Args: input file, output file, whether or not we want to check for duplicates
    val inFile = args[0]
    val outFile = args[1]
    var checkForDuplicateUrls = args[2] == "true"

    //Determine how many workers and buffer size from number of available processors
    val workers = Runtime.getRuntime().availableProcessors()
    val bufferSize = workers * 4

    //Process our image file
    FindImageColors().processImageFile(inFile, outFile, checkForDuplicateUrls, workers, bufferSize)
}
