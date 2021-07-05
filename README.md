# image-coding-challenge

## Usage

The program takes three arguments:
1. input file name
2. output file name
3. boolean flag to check for duplicate urls

Note: the program does not fail gracefully at this time.  It'll bomb if missing the first two inputs, and assuming that you do not want to check for duplicate urls if you do not enter the third.

## How it works
The program determines how many workers to use based on the number of available processors.  It determines the buffer size based on the number of workers.  These could easily be made configurable, but for now it uses the maximum amount available.

The program will determine the top 3 most prevelant colors for each image and output the results to a csv file with the url and the top 3 colors.  

By default, the program will look for duplicate urls - that is, it will not process a url if we've already processed it, and only one line for that url will be processed and output to the csv file.

## Notes
I time-boxed this project, but these are the next things I'd do if I were to continue.
1. Logging needs to be added - right now there are TODO's where we would need to catch exceptions and we should add some logging at various points of the program.
2. The program still does not run as fast as I'd like it - I believe there is still a place where I'm blocking, maybe with my HTTPClient, and this should be looked into further.
3. I'm manually writing out the csv - a csv writer should be used to handle this.
